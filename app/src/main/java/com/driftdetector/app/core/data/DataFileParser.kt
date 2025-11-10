package com.driftdetector.app.core.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.min

/**
 * Enhanced data file parser supporting multiple formats with comprehensive validation:
 * - CSV (.csv)
 * - JSON (.json)
 * - TSV/Tab-delimited (.tsv, .txt)
 * - Excel-like formats (basic support)
 * - Pipe-delimited (.psv)
 * - Space-delimited (.dat)
 *
 * Features:
 * - File corruption detection
 * - Data quality validation
 * - Format auto-detection
 * - Detailed error reporting
 */
class DataFileParser(private val context: Context) {

    /**
     * Exception types for detailed error handling
     */
    sealed class DataParsingException(message: String, cause: Throwable? = null) :
        Exception(message, cause) {
        class FileNotReadableException(uri: Uri) :
            DataParsingException("File not readable or accessible: $uri")

        class EmptyFileException :
            DataParsingException("File is empty or contains no valid data")

        class UnsupportedFormatException(format: String) :
            DataParsingException("Unsupported file format: $format. Supported formats: CSV, JSON, TSV, TXT, PSV, DAT")

        class CorruptedDataException(val corruptedRows: List<Int>, val totalRows: Int) :
            DataParsingException(
                "Data corruption detected: ${corruptedRows.size} of $totalRows rows are corrupted or malformed. " +
                        "Corrupted row numbers: ${
                            corruptedRows.take(10).joinToString(", ")
                        }${if (corruptedRows.size > 10) "..." else ""}"
            )

        class InconsistentFeatureCountException(
            val expectedCount: Int,
            val inconsistentRows: Map<Int, Int>
        ) :
            DataParsingException(
                "Inconsistent feature count: Expected $expectedCount features, but found ${inconsistentRows.size} rows with different counts. " +
                        "Examples: ${
                            inconsistentRows.entries.take(5)
                                .joinToString(", ") { "row ${it.key} has ${it.value} features" }
                        }"
            )

        class InvalidDataTypeException(row: Int, column: Int, value: String) :
            DataParsingException("Invalid data type at row $row, column $column: Cannot convert '$value' to numeric value")

        class InsufficientDataException(actualCount: Int, minimumRequired: Int) :
            DataParsingException("Insufficient data: Found $actualCount samples, minimum required: $minimumRequired")

        class UnexpectedErrorException(message: String, cause: Throwable?) :
            DataParsingException("Unexpected error while parsing file: $message", cause)
    }

    /**
     * Parse data file based on extension with comprehensive validation
     */
    fun parseFile(uri: Uri, fileName: String, expectedFeatures: Int): Result<List<FloatArray>> {
        return try {
            Timber.d("📊 Parsing data file: $fileName (expected $expectedFeatures features)")

            // Step 1: Check file readability
            if (!isFileReadable(uri)) {
                return Result.failure(DataParsingException.FileNotReadableException(uri))
            }

            // Step 2: Detect and validate file format
            val format = detectFileFormat(uri, fileName)
            if (!isSupportedFormat(format)) {
                return Result.failure(DataParsingException.UnsupportedFormatException(format))
            }

            // Step 3: Parse based on detected format
            val data = when {
                fileName.endsWith(".csv", ignoreCase = true) -> parseCSV(uri, expectedFeatures)
                fileName.endsWith(".json", ignoreCase = true) -> parseJSON(uri, expectedFeatures)
                fileName.endsWith(".tsv", ignoreCase = true) -> parseTSV(uri, expectedFeatures)
                fileName.endsWith(".txt", ignoreCase = true) -> parseTextFile(uri, expectedFeatures)
                fileName.endsWith(".psv", ignoreCase = true) -> parsePipeSeparated(
                    uri,
                    expectedFeatures
                )

                fileName.endsWith(".dat", ignoreCase = true) -> parseSpaceSeparated(
                    uri,
                    expectedFeatures
                )

                else -> parseAutoDetect(uri, expectedFeatures)
            }

            // Step 4: Validate data quality
            validateDataQuality(data, expectedFeatures, fileName)?.let {
                return Result.failure(it)
            }

            if (data.isEmpty()) {
                Timber.w("⚠️ No valid data parsed, file might be empty or malformed")
                return Result.failure(DataParsingException.EmptyFileException())
            }

            Timber.i("✅ Successfully parsed ${data.size} rows with ${data.firstOrNull()?.size ?: 0} features")
            Result.success(data)

        } catch (e: DataParsingException) {
            Timber.e(e, "❌ Data parsing error: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to parse data file: $fileName")
            Result.failure(
                DataParsingException.UnexpectedErrorException(e.message ?: "Unknown error", e)
            )
        }
    }

    /**
     * Check if file is readable and accessible
     */
    private fun isFileReadable(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.read() // Try to read at least one byte
                true
            } ?: false
        } catch (e: IOException) {
            Timber.e(e, "File not readable: $uri")
            false
        }
    }

    /**
     * Detect file format from content
     */
    private fun detectFileFormat(uri: Uri, fileName: String): String {
        val extension = fileName.substringAfterLast(".", "").lowercase()

        // Also check content for better detection
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = CharArray(100)
                val reader = stream.bufferedReader()
                val charsRead = reader.read(buffer, 0, 100)
                val preview = if (charsRead > 0) String(buffer, 0, charsRead) else ""

                when {
                    preview.trim().startsWith("{") || preview.trim()
                        .startsWith("[") -> return "json"

                    preview.contains("\t") -> return "tsv"
                    preview.contains("|") -> return "psv"
                    preview.contains(",") -> return "csv"
                }
            }
        } catch (e: IOException) {
            Timber.w("Could not preview file content for format detection")
        }

        return extension
    }

    /**
     * Check if format is supported
     */
    private fun isSupportedFormat(format: String): Boolean {
        return format in listOf("csv", "json", "tsv", "txt", "psv", "dat")
    }

    /**
     * Validate data quality and detect corruption
     */
    private fun validateDataQuality(
        data: List<FloatArray>,
        expectedFeatures: Int,
        fileName: String
    ): DataParsingException? {
        if (data.isEmpty()) {
            return DataParsingException.EmptyFileException()
        }

        // Minimum data requirement
        val minimumSamples = 50
        if (data.size < minimumSamples) {
            return DataParsingException.InsufficientDataException(data.size, minimumSamples)
        }

        // Check for corrupted rows (NaN, Inf, or all zeros)
        val corruptedRows = mutableListOf<Int>()
        data.forEachIndexed { index, row ->
            if (isCorruptedRow(row)) {
                corruptedRows.add(index + 1)
            }
        }

        if (corruptedRows.isNotEmpty() && corruptedRows.size > data.size * 0.1) {
            // More than 10% corrupted is unacceptable
            return DataParsingException.CorruptedDataException(corruptedRows, data.size)
        }

        // Check for inconsistent feature counts
        val inconsistentRows = mutableMapOf<Int, Int>()
        data.forEachIndexed { index, row ->
            if (row.size != expectedFeatures) {
                inconsistentRows[index + 1] = row.size
            }
        }

        if (inconsistentRows.isNotEmpty() && inconsistentRows.size > data.size * 0.05) {
            // More than 5% inconsistent is problematic
            return DataParsingException.InconsistentFeatureCountException(
                expectedFeatures,
                inconsistentRows
            )
        }

        // Check data ranges (detect if all features have same value - likely corrupt)
        val allSameValue = data.all { row ->
            row.distinct().size == 1
        }

        if (allSameValue && data.size > 10) {
            Timber.w("⚠️ All samples have identical values - possible data corruption")
            return DataParsingException.CorruptedDataException(
                (1..data.size).toList(),
                data.size
            )
        }

        return null
    }

    /**
     * Check if a row is corrupted
     */
    private fun isCorruptedRow(row: FloatArray): Boolean {
        if (row.isEmpty()) return true

        return row.any { it.isNaN() || it.isInfinite() } ||
                row.all { it == 0f } // All zeros might indicate corruption
    }

    /**
     * Parse CSV file with enhanced features
     */
    private fun parseCSV(uri: Uri, expectedFeatures: Int): List<FloatArray> {
        val data = mutableListOf<FloatArray>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    var hasHeader = false

                    // Detect header (contains letters or non-numeric first cell)
                    if (line != null) {
                        val firstCell = line.split(",").firstOrNull()?.trim() ?: ""
                        hasHeader = firstCell.any { it.isLetter() } || firstCell.isEmpty()

                        if (hasHeader) {
                            Timber.d("📋 Detected CSV header: ${line.take(100)}")
                            line = reader.readLine()
                        }
                    }

                    // Parse data rows
                    var rowCount = 0
                    while (line != null) {
                        rowCount++
                        try {
                            val row = parseCSVRow(line, expectedFeatures)
                            if (row != null && row.size == expectedFeatures) {
                                data.add(row)
                            } else if (row != null) {
                                Timber.w("⚠️ Row $rowCount has ${row.size} features, expected $expectedFeatures - padding/truncating")
                                data.add(normalizeFeatureCount(row, expectedFeatures))
                            }
                        } catch (e: Exception) {
                            Timber.w("⚠️ Skipping invalid row $rowCount: ${e.message}")
                        }

                        line = reader.readLine()
                    }

                    Timber.d("✅ Parsed ${data.size} valid rows from CSV")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read CSV file")
        }

        return data
    }

    /**
     * Parse CSV row handling quoted values and edge cases
     */
    private fun parseCSVRow(line: String, expectedFeatures: Int): FloatArray? {
        val values = mutableListOf<String>()
        var currentValue = StringBuilder()
        var insideQuotes = false

        for (char in line) {
            when {
                char == '"' -> insideQuotes = !insideQuotes
                char == ',' && !insideQuotes -> {
                    values.add(currentValue.toString().trim())
                    currentValue = StringBuilder()
                }
                else -> currentValue.append(char)
            }
        }
        values.add(currentValue.toString().trim())

        // Convert to floats
        return try {
            values.take(expectedFeatures)
                .map { it.replace("\"", "").trim() }
                .filter { it.isNotEmpty() }
                .map { parseNumeric(it) }
                .toFloatArray()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse JSON file (multiple formats supported)
     */
    private fun parseJSON(uri: Uri, expectedFeatures: Int): List<FloatArray> {
        val data = mutableListOf<FloatArray>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val json = inputStream.bufferedReader().use { it.readText() }
                val jsonElement = JsonParser.parseString(json)

                when {
                    // Format 1: {"data": [[1,2,3], [4,5,6]]}
                    jsonElement.isJsonObject -> {
                        val obj = jsonElement.asJsonObject
                        when {
                            obj.has("data") -> {
                                val dataArray = obj.getAsJsonArray("data")
                                parseJsonArray(dataArray, expectedFeatures, data)
                            }
                            obj.has("values") -> {
                                val dataArray = obj.getAsJsonArray("values")
                                parseJsonArray(dataArray, expectedFeatures, data)
                            }
                            obj.has("rows") -> {
                                val dataArray = obj.getAsJsonArray("rows")
                                parseJsonArray(dataArray, expectedFeatures, data)
                            }
                            else -> {
                                // Try to parse object as single row
                                val row = parseJsonObjectAsRow(obj, expectedFeatures)
                                if (row != null) data.add(row)
                            }
                        }
                    }
                    // Format 2: [[1,2,3], [4,5,6]]
                    jsonElement.isJsonArray -> {
                        parseJsonArray(jsonElement.asJsonArray, expectedFeatures, data)
                    }
                }

                Timber.d("✅ Parsed ${data.size} rows from JSON")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse JSON file")
        }

        return data
    }

    /**
     * Parse JSON array to data rows
     */
    private fun parseJsonArray(
        jsonArray: JsonArray,
        expectedFeatures: Int,
        data: MutableList<FloatArray>
    ) {
        for (element in jsonArray) {
            when {
                element.isJsonArray -> {
                    // Row is an array: [1.0, 2.0, 3.0]
                    val row = element.asJsonArray.mapNotNull {
                        try {
                            it.asFloat
                        } catch (e: Exception) {
                            null
                        }
                    }.toFloatArray()

                    if (row.size == expectedFeatures) {
                        data.add(row)
                    } else if (row.isNotEmpty()) {
                        data.add(normalizeFeatureCount(row, expectedFeatures))
                    }
                }
                element.isJsonObject -> {
                    // Row is an object: {"feature_0": 1.0, "feature_1": 2.0}
                    val row = parseJsonObjectAsRow(element.asJsonObject, expectedFeatures)
                    if (row != null) data.add(row)
                }
            }
        }
    }

    /**
     * Parse JSON object as a data row
     */
    private fun parseJsonObjectAsRow(obj: JsonObject, expectedFeatures: Int): FloatArray? {
        return try {
            val values = mutableListOf<Float>()

            // Try numeric keys first (0, 1, 2, ...)
            for (i in 0 until expectedFeatures) {
                if (obj.has(i.toString())) {
                    values.add(obj.get(i.toString()).asFloat)
                } else if (obj.has("feature_$i")) {
                    values.add(obj.get("feature_$i").asFloat)
                } else if (obj.has("f$i")) {
                    values.add(obj.get("f$i").asFloat)
                }
            }

            // If not enough values, try all numeric values in order
            if (values.size < expectedFeatures) {
                values.clear()
                obj.entrySet().sortedBy { it.key }.forEach { entry ->
                    try {
                        values.add(entry.value.asFloat)
                    } catch (e: Exception) {
                        // Skip non-numeric values
                    }
                }
            }

            if (values.size >= expectedFeatures) {
                values.take(expectedFeatures).toFloatArray()
            } else {
                normalizeFeatureCount(values.toFloatArray(), expectedFeatures)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse TSV (tab-separated values)
     */
    private fun parseTSV(uri: Uri, expectedFeatures: Int): List<FloatArray> {
        return parseDelimitedFile(uri, '\t', expectedFeatures, "TSV")
    }

    /**
     * Parse pipe-separated values
     */
    private fun parsePipeSeparated(uri: Uri, expectedFeatures: Int): List<FloatArray> {
        return parseDelimitedFile(uri, '|', expectedFeatures, "PSV")
    }

    /**
     * Parse space-separated values
     */
    private fun parseSpaceSeparated(uri: Uri, expectedFeatures: Int): List<FloatArray> {
        return parseDelimitedFile(uri, ' ', expectedFeatures, "Space-delimited", skipMultiple = true)
    }

    /**
     * Parse generic delimited file
     */
    private fun parseDelimitedFile(
        uri: Uri,
        delimiter: Char,
        expectedFeatures: Int,
        formatName: String,
        skipMultiple: Boolean = false
    ): List<FloatArray> {
        val data = mutableListOf<FloatArray>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()

                    // Skip header if present
                    if (line?.any { it.isLetter() && !it.isWhitespace() } == true) {
                        line = reader.readLine()
                    }

                    while (line != null) {
                        try {
                            val parts = if (skipMultiple) {
                                line.split(Regex("\\s+"))
                            } else {
                                line.split(delimiter)
                            }

                            val values = parts
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .take(expectedFeatures)
                                .map { parseNumeric(it) }
                                .toFloatArray()

                            if (values.size == expectedFeatures) {
                                data.add(values)
                            } else if (values.isNotEmpty()) {
                                data.add(normalizeFeatureCount(values, expectedFeatures))
                            }
                        } catch (e: Exception) {
                            Timber.w("Skipping invalid row: ${e.message}")
                        }

                        line = reader.readLine()
                    }

                    Timber.d("✅ Parsed ${data.size} rows from $formatName")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse $formatName file")
        }

        return data
    }

    /**
     * Parse text file with auto-detection
     */
    private fun parseTextFile(uri: Uri, expectedFeatures: Int): List<FloatArray> {
        // Try to detect delimiter
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val firstLine = BufferedReader(InputStreamReader(inputStream)).use {
                    it.readLine()
                }

                if (firstLine != null) {
                    val delimiter = detectDelimiter(firstLine)
                    Timber.d("📊 Auto-detected delimiter: '${if (delimiter == '\t') "\\t" else delimiter}'")

                    return when (delimiter) {
                        ',' -> parseCSV(uri, expectedFeatures)
                        '\t' -> parseTSV(uri, expectedFeatures)
                        '|' -> parsePipeSeparated(uri, expectedFeatures)
                        ' ' -> parseSpaceSeparated(uri, expectedFeatures)
                        else -> parseCSV(uri, expectedFeatures) // Default to CSV
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to auto-detect format")
        }

        return emptyList()
    }

    /**
     * Auto-detect file format and parse
     */
    private fun parseAutoDetect(uri: Uri, expectedFeatures: Int): List<FloatArray> {
        Timber.d("🔍 Auto-detecting file format...")

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val preview = buildString {
                    repeat(5) { // Read first 5 lines
                        val line = reader.readLine() ?: return@buildString
                        appendLine(line)
                    }
                }
                reader.close()

                when {
                    preview.trim().startsWith("{") || preview.trim().startsWith("[") -> {
                        Timber.d("📊 Detected JSON format")
                        parseJSON(uri, expectedFeatures)
                    }
                    preview.contains("\t") -> {
                        Timber.d("📊 Detected TSV format")
                        parseTSV(uri, expectedFeatures)
                    }
                    preview.contains("|") -> {
                        Timber.d("📊 Detected pipe-separated format")
                        parsePipeSeparated(uri, expectedFeatures)
                    }
                    else -> {
                        Timber.d("📊 Defaulting to CSV format")
                        parseCSV(uri, expectedFeatures)
                    }
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Auto-detection failed")
            emptyList()
        }
    }

    /**
     * Detect delimiter from a line
     */
    private fun detectDelimiter(line: String): Char {
        val delimiters = listOf(',', '\t', '|', ';', ' ')
        val counts = delimiters.associateWith { delimiter ->
            line.count { it == delimiter }
        }

        return counts.maxByOrNull { it.value }?.key ?: ','
    }

    /**
     * Parse numeric value handling different formats
     */
    private fun parseNumeric(value: String): Float {
        return try {
            val cleaned = value.replace(Regex("[^0-9.\\-eE+]"), "")
            when {
                cleaned.isEmpty() -> 0f
                cleaned == "-" -> 0f
                else -> cleaned.toFloat()
            }
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Normalize feature count by padding or truncating
     */
    private fun normalizeFeatureCount(row: FloatArray, expectedCount: Int): FloatArray {
        return when {
            row.size == expectedCount -> row
            row.size > expectedCount -> {
                Timber.w("Truncating ${row.size} features to $expectedCount")
                row.take(expectedCount).toFloatArray()
            }
            else -> {
                Timber.w("Padding ${row.size} features to $expectedCount with zeros")
                FloatArray(expectedCount) { i ->
                    if (i < row.size) row[i] else 0f
                }
            }
        }
    }

    /**
     * Get file statistics
     */
    fun getFileStats(uri: Uri): FileStats? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var lineCount = 0
                var columnCount = 0
                var hasHeader = false

                val firstLine = reader.readLine()
                if (firstLine != null) {
                    lineCount++
                    hasHeader = firstLine.any { it.isLetter() }
                    val delimiter = detectDelimiter(firstLine)
                    columnCount = firstLine.split(delimiter).size
                }

                while (reader.readLine() != null) {
                    lineCount++
                }

                FileStats(
                    totalRows = lineCount - if (hasHeader) 1 else 0,
                    totalColumns = columnCount,
                    hasHeader = hasHeader
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get file stats")
            null
        }
    }
}

/**
 * File statistics
 */
data class FileStats(
    val totalRows: Int,
    val totalColumns: Int,
    val hasHeader: Boolean
)
