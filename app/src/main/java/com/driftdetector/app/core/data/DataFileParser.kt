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
 * - Column name preservation
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
     * Parsed data with column names preserved
     */
    data class ParsedData(
        val data: List<FloatArray>,
        val columnNames: List<String>,
        val hasHeader: Boolean
    )

    /**
     * Parse data file based on extension with comprehensive validation
     */
    fun parseFile(uri: Uri, fileName: String, expectedFeatures: Int): Result<ParsedData> {
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
            val parsedData = when {
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
            validateDataQuality(
                parsedData.data,
                expectedFeatures,
                fileName,
                parsedData.hasHeader
            )?.let {
                return Result.failure(it)
            }

            if (parsedData.data.isEmpty()) {
                Timber.w("⚠️ No valid data parsed, file might be empty or malformed")
                return Result.failure(DataParsingException.EmptyFileException())
            }

            Timber.i("✅ Successfully parsed ${parsedData.data.size} rows with ${parsedData.data.firstOrNull()?.size ?: 0} features")
            Result.success(parsedData)

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
        fileName: String,
        hasHeader: Boolean
    ): DataParsingException? {
        if (data.isEmpty()) {
            return DataParsingException.EmptyFileException()
        }

        // Minimum data requirement
        val minimumSamples = 20
        if (data.size < minimumSamples) {
            Timber.w("⚠️ Dataset has only ${data.size} samples, minimum recommended is $minimumSamples but proceeding anyway")
            // Don't fail, just warn
        }

        // Check for corrupted rows (NaN, Inf only - not zeros)
        val corruptedRows = mutableListOf<Int>()
        data.forEachIndexed { index, row ->
            if (isCorruptedRow(row)) {
                corruptedRows.add(index + 1)
            }
        }

        // Only fail if significant portion has NaN/Inf (>30% truly corrupted)
        if (corruptedRows.isNotEmpty() && corruptedRows.size > data.size * 0.3) {
            return DataParsingException.CorruptedDataException(corruptedRows, data.size)
        }

        // If expectedFeatures is 0 (auto-detect mode), determine from first row
        val actualExpectedFeatures = if (expectedFeatures == 0) {
            data.firstOrNull()?.size ?: 0
        } else {
            expectedFeatures
        }

        // Check for inconsistent feature counts
        val inconsistentRows = mutableMapOf<Int, Int>()
        data.forEachIndexed { index, row ->
            if (actualExpectedFeatures > 0 && row.size != actualExpectedFeatures) {
                inconsistentRows[index + 1] = row.size
            }
        }

        // Only fail if more than 20% of rows have wrong feature count
        if (inconsistentRows.isNotEmpty() && inconsistentRows.size > data.size * 0.2) {
            return DataParsingException.InconsistentFeatureCountException(
                actualExpectedFeatures,
                inconsistentRows
            )
        }

        // Remove the overly strict "all same value" check
        // Some datasets legitimately have constant values or normalized data
        // Only warn, don't fail
        val allSameValue = data.size > 10 && data.all { row ->
            row.distinct().size == 1
        }

        if (allSameValue) {
            Timber.w("⚠️ All samples have identical values - this may be expected for some datasets")
            // Don't fail, just warn
        }

        return null
    }

    /**
     * Check if a row is corrupted
     */
    private fun isCorruptedRow(row: FloatArray): Boolean {
        if (row.isEmpty()) return true

        // Only flag rows with NaN or Infinity - all zeros can be valid data
        return row.any { it.isNaN() || it.isInfinite() }
    }

    /**
     * Parse CSV file with enhanced features
     */
    private fun parseCSV(uri: Uri, expectedFeatures: Int): ParsedData {
        val data = mutableListOf<FloatArray>()
        var columnNames: List<String> = emptyList()
        var hasHeader = false

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    if (line != null) {
                        val firstCell = line.split(",").firstOrNull()?.trim() ?: ""
                        hasHeader = firstCell.any { it.isLetter() } || firstCell.isEmpty()

                        if (hasHeader) {
                            Timber.d("📋 Detected CSV header: ${line.take(100)}")
                            columnNames = line.split(",").map { it.trim().replace("\"", "") }
                            line = reader.readLine()
                        }
                    }

                    // FIXED: Auto-detect feature count from first data row if expectedFeatures is 0
                    var actualFeatureCount = expectedFeatures
                    if (actualFeatureCount == 0 && line != null) {
                        val firstRow = parseCSVRow(line, 1000) // Parse with large max
                        if (firstRow != null && firstRow.isNotEmpty()) {
                            actualFeatureCount = firstRow.size
                            Timber.d("📊 Auto-detected $actualFeatureCount features from first data row")

                            // Generate default column names if no header was found
                            if (columnNames.isEmpty()) {
                                columnNames = List(actualFeatureCount) { "feature_$it" }
                                Timber.d("📋 Generated default column names for $actualFeatureCount features")
                            }
                        }
                    }

                    // Parse data rows
                    var rowCount = 0
                    while (line != null) {
                        rowCount++
                        try {
                            val row = parseCSVRow(
                                line,
                                if (actualFeatureCount > 0) actualFeatureCount else 1000
                            )
                            if (row != null && row.isNotEmpty()) {
                                // FIXED: Accept rows even if feature count differs slightly
                                if (actualFeatureCount > 0) {
                                    if (row.size == actualFeatureCount) {
                                        data.add(row)
                                    } else {
                                        // Normalize to expected count
                                        data.add(normalizeFeatureCount(row, actualFeatureCount))
                                    }
                                } else {
                                    // No expected count, accept as-is
                                    data.add(row)
                                }
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

        // Ensure column names match data dimensions
        if (data.isNotEmpty() && columnNames.size != data.first().size) {
            val actualSize = data.first().size
            Timber.w("⚠️ Column name count (${columnNames.size}) doesn't match data dimensions ($actualSize), regenerating")
            columnNames = List(actualSize) { i ->
                if (i < columnNames.size) columnNames[i] else "feature_$i"
            }
        }

        return ParsedData(data, columnNames, hasHeader)
    }

    /**
     * Parse CSV row handling quoted values and edge cases
     */
    private fun parseCSVRow(line: String, maxFeatures: Int): FloatArray? {
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
            // FIXED: Take up to maxFeatures, but parse all valid numeric values
            values.take(maxFeatures)
                .map { it.replace("\"", "").trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull {
                    try {
                        parseNumeric(it)
                    } catch (e: Exception) {
                        null // Skip non-numeric values
                    }
                }
                .toFloatArray()
                .takeIf { it.isNotEmpty() } // Only return if we got at least one value
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse JSON file (multiple formats supported)
     */
    private fun parseJSON(uri: Uri, expectedFeatures: Int): ParsedData {
        val data = mutableListOf<FloatArray>()
        val columnNames = mutableListOf<String>()
        var hasHeader = false

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
                                // Check if there's a columns field
                                if (obj.has("columns")) {
                                    hasHeader = true
                                    val colArray = obj.getAsJsonArray("columns")
                                    colArray.forEach { columnNames.add(it.asString) }
                                }
                                parseJsonArray(
                                    dataArray,
                                    expectedFeatures,
                                    data,
                                    columnNames,
                                    hasHeader
                                )
                            }
                            obj.has("values") -> {
                                val dataArray = obj.getAsJsonArray("values")
                                if (obj.has("columns")) {
                                    hasHeader = true
                                    val colArray = obj.getAsJsonArray("columns")
                                    colArray.forEach { columnNames.add(it.asString) }
                                }
                                parseJsonArray(
                                    dataArray,
                                    expectedFeatures,
                                    data,
                                    columnNames,
                                    hasHeader
                                )
                            }
                            obj.has("rows") -> {
                                val dataArray = obj.getAsJsonArray("rows")
                                if (obj.has("columns")) {
                                    hasHeader = true
                                    val colArray = obj.getAsJsonArray("columns")
                                    colArray.forEach { columnNames.add(it.asString) }
                                }
                                parseJsonArray(
                                    dataArray,
                                    expectedFeatures,
                                    data,
                                    columnNames,
                                    hasHeader
                                )
                            }
                            else -> {
                                // Try to parse object as single row
                                val row = parseJsonObjectAsRow(obj, expectedFeatures)
                                if (row != null) {
                                    data.add(row)
                                    // Extract column names from object keys
                                    if (columnNames.isEmpty()) {
                                        hasHeader = true
                                        obj.entrySet().sortedBy { it.key }.forEach { entry ->
                                            try {
                                                entry.value.asFloat // Test if numeric
                                                columnNames.add(entry.key)
                                            } catch (e: Exception) {
                                                // Skip non-numeric values
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Format 2: [[1,2,3], [4,5,6]]
                    jsonElement.isJsonArray -> {
                        val array = jsonElement.asJsonArray
                        // Check if first element is object (has column names as keys)
                        if (array.size() > 0 && array[0].isJsonObject) {
                            val firstObj = array[0].asJsonObject
                            hasHeader = true
                            firstObj.entrySet().sortedBy { it.key }.forEach { entry ->
                                try {
                                    entry.value.asFloat // Test if numeric
                                    columnNames.add(entry.key)
                                } catch (e: Exception) {
                                    // Skip non-numeric values
                                }
                            }
                        }
                        parseJsonArray(
                            array,
                            expectedFeatures,
                            data,
                            columnNames,
                            hasHeader
                        )
                    }
                }

                Timber.d("✅ Parsed ${data.size} rows from JSON")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse JSON file")
        }

        // Ensure column names match data dimensions
        if (data.isNotEmpty() && columnNames.size != data.first().size) {
            val actualSize = data.first().size
            Timber.w("⚠️ Column name count (${columnNames.size}) doesn't match data dimensions ($actualSize), regenerating")
            columnNames.clear()
            columnNames.addAll(List(actualSize) { i ->
                if (i < columnNames.size) columnNames[i] else "feature_$i"
            })
        } else if (data.isNotEmpty() && columnNames.isEmpty()) {
            // Generate default column names if none were found
            val actualSize = data.first().size
            Timber.d("📋 Generating default column names for $actualSize features")
            columnNames.addAll(List(actualSize) { "feature_$it" })
        }

        return ParsedData(data, columnNames.toList(), hasHeader)
    }

    /**
     * Parse JSON array to data rows
     */
    private fun parseJsonArray(
        jsonArray: JsonArray,
        expectedFeatures: Int,
        data: MutableList<FloatArray>,
        columnNames: MutableList<String>,
        hasHeader: Boolean
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
    private fun parseTSV(uri: Uri, expectedFeatures: Int): ParsedData {
        return parseDelimitedFile(uri, '\t', expectedFeatures, "TSV")
    }

    /**
     * Parse pipe-separated values
     */
    private fun parsePipeSeparated(uri: Uri, expectedFeatures: Int): ParsedData {
        return parseDelimitedFile(uri, '|', expectedFeatures, "PSV")
    }

    /**
     * Parse space-separated values
     */
    private fun parseSpaceSeparated(uri: Uri, expectedFeatures: Int): ParsedData {
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
    ): ParsedData {
        val data = mutableListOf<FloatArray>()
        var columnNames: List<String> = emptyList()
        var hasHeader = false

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()

                    // Skip header if present
                    if (line?.any { it.isLetter() && !it.isWhitespace() } == true) {
                        hasHeader = true
                        columnNames = if (skipMultiple) {
                            line.split(Regex("\\s+"))
                        } else {
                            line.split(delimiter)
                        }.map { it.trim().replace("\"", "") }
                        line = reader.readLine()
                    }

                    // FIXED: Auto-detect feature count from first data row if expectedFeatures is 0
                    var actualFeatureCount = expectedFeatures
                    if (actualFeatureCount == 0 && line != null) {
                        try {
                            val parts = if (skipMultiple) {
                                line.split(Regex("\\s+"))
                            } else {
                                line.split(delimiter)
                            }

                            val firstRowValues = parts
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .mapNotNull {
                                    try {
                                        parseNumeric(it)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                            if (firstRowValues.isNotEmpty()) {
                                actualFeatureCount = firstRowValues.size
                                Timber.d(" Auto-detected $actualFeatureCount features from first data row")

                                // Generate default column names if no header was found
                                if (columnNames.isEmpty()) {
                                    columnNames = List(actualFeatureCount) { "feature_$it" }
                                    Timber.d("📋 Generated default column names for $actualFeatureCount features")
                                }
                            }
                        } catch (e: Exception) {
                            Timber.w("Could not auto-detect feature count: ${e.message}")
                        }
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
                                .mapNotNull {
                                    try {
                                        parseNumeric(it)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                .toFloatArray()

                            if (values.isNotEmpty()) {
                                // FIXED: Accept rows even if feature count differs
                                if (actualFeatureCount > 0) {
                                    if (values.size == actualFeatureCount) {
                                        data.add(values)
                                    } else {
                                        data.add(normalizeFeatureCount(values, actualFeatureCount))
                                    }
                                } else {
                                    // No expected count, accept as-is
                                    data.add(values)
                                }
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

        // Ensure column names match data dimensions
        if (data.isNotEmpty() && columnNames.size != data.first().size) {
            val actualSize = data.first().size
            Timber.w("⚠️ Column name count (${columnNames.size}) doesn't match data dimensions ($actualSize), regenerating")
            columnNames = List(actualSize) { i ->
                if (i < columnNames.size) columnNames[i] else "feature_$i"
            }
        }

        return ParsedData(data, columnNames, hasHeader)
    }

    /**
     * Parse text file with auto-detection
     */
    private fun parseTextFile(uri: Uri, expectedFeatures: Int): ParsedData {
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

        return ParsedData(emptyList(), emptyList(), false)
    }

    /**
     * Auto-detect file format and parse
     */
    private fun parseAutoDetect(uri: Uri, expectedFeatures: Int): ParsedData {
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
            } ?: ParsedData(emptyList(), emptyList(), false)
        } catch (e: Exception) {
            Timber.e(e, "Auto-detection failed")
            ParsedData(emptyList(), emptyList(), false)
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
        if (value.isBlank()) {
            throw NumberFormatException("Empty or blank value")
        }

        return try {
            // Remove common non-numeric characters but keep valid numeric parts
            val cleaned = value.trim()
                .replace(Regex("[$,%]"), "") // Remove currency symbols and percent
                .replace(Regex("\\s+"), "") // Remove all whitespace
                .trim()

            when {
                cleaned.isEmpty() -> throw NumberFormatException("Empty after cleaning")
                cleaned.equals(
                    "null",
                    ignoreCase = true
                ) -> throw NumberFormatException("Null value")

                cleaned.equals("na", ignoreCase = true) -> throw NumberFormatException("NA value")
                cleaned.equals("nan", ignoreCase = true) -> throw NumberFormatException("NaN value")
                cleaned == "-" -> throw NumberFormatException("Invalid minus sign")
                else -> {
                    val parsed = cleaned.toFloat()
                    // Validate the parsed value
                    if (parsed.isNaN() || parsed.isInfinite()) {
                        throw NumberFormatException("NaN or Infinite value")
                    }
                    parsed
                }
            }
        } catch (e: NumberFormatException) {
            // Re-throw with original value for better error messages
            throw NumberFormatException("Cannot parse '$value' as numeric: ${e.message}")
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
