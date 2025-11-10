package com.driftdetector.app.presentation.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.driftdetector.app.core.drift.InstantDriftFixManager.PatchCandidate
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.DriftType
import com.driftdetector.app.domain.model.PatchType
import com.driftdetector.app.presentation.viewmodel.InstantDriftFixState
import com.driftdetector.app.presentation.viewmodel.InstantDriftFixViewModel
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantDriftFixScreen(
    onNavigateBack: () -> Unit,
    viewModel: InstantDriftFixViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Multiple file picker launcher
    val multipleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.size >= 2) {
            val files = uris.take(2).map { uri ->
                uri to extractFileName(context, uri)
            }
            viewModel.storeFilesAndAnalyze(files[0], files[1])
        } else if (uris.size == 1) {
            // If only one file, prompt for the second
            val file1 = uris[0] to extractFileName(context, uris[0])
            viewModel.storeFirstFile(file1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚡ Instant Drift Fix") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is InstantDriftFixState.Idle -> {
                    IdleScreen(
                        onUploadFiles = { 
                            multipleFileLauncher.launch(arrayOf("*/*"))
                        },
                        hasFirstFile = state.hasFirstFile,
                        firstFileName = state.firstFileName
                    )
                }
                is InstantDriftFixState.Analyzing -> {
                    AnalyzingScreen()
                }
                is InstantDriftFixState.AnalysisComplete -> {
                    AnalysisCompleteScreen(
                        state = state,
                        onApplyPatches = { selectedIds ->
                            viewModel.applyPatchesAndExport(selectedIds, state)
                        },
                        onCancel = { viewModel.reset() }
                    )
                }
                is InstantDriftFixState.ApplyingPatches -> {
                    ApplyingPatchesScreen(state)
                }
                is InstantDriftFixState.PatchesApplied -> {
                    PatchesAppliedScreen(
                        state = state,
                        onDownloadModel = {
                            downloadFile(context, state.patchedModelFile)
                        },
                        onDownloadData = {
                            downloadFile(context, state.patchedDataFile)
                        },
                        onReset = { viewModel.reset() }
                    )
                }
                is InstantDriftFixState.Error -> {
                    ErrorScreen(
                        message = state.message,
                        onReset = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

@Composable
fun IdleScreen(
    onUploadFiles: () -> Unit,
    hasFirstFile: Boolean,
    firstFileName: String?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Instant fix",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "⚡ Instant Drift Detection & Fix",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Upload your ML model + dataset together. Get instant AI-powered drift analysis and automated fixes in seconds!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            if (hasFirstFile && firstFileName != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "File uploaded",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "First file uploaded",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                firstFileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onUploadFiles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    "Upload files",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        if (hasFirstFile) "Upload Second File" else "Upload Model + Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (hasFirstFile) "Select your second file" else "Select 2 files together",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            HowItWorksCard()
        }

        item {
            FeatureHighlightsCard()
        }
    }
}

@Composable
fun HowItWorksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "🎯 How It Works",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            StepItem(
                number = "1",
                title = "Upload Both Files",
                description = "Select ML model and dataset in one go",
                icon = Icons.Default.Upload
            )

            StepItem(
                number = "2",
                title = "AI Analysis (< 2 sec)",
                description = "RunAnywhere SDK analyzes drift instantly",
                icon = Icons.Default.Psychology
            )

            StepItem(
                number = "3",
                title = "Smart Patches",
                description = "AI recommends best fixes with explanations",
                icon = Icons.Default.AutoFixHigh
            )

            StepItem(
                number = "4",
                title = "Download Fixed Files",
                description = "Get patched files saved to Downloads",
                icon = Icons.Default.Download
            )
        }
    }
}

@Composable
fun FeatureHighlightsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "✨ Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FeatureItem("⚡ Instant analysis (< 2 seconds)")
            FeatureItem("🤖 AI-powered recommendations")
            FeatureItem("📥 Direct download to device")
            FeatureItem("🔒 100% offline & private")
            FeatureItem("🎯 Multiple patch types")
            FeatureItem("📊 Drift reduction metrics")
        }
    }
}

@Composable
fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun StepItem(number: String, title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    number,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnalyzingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated progress indicator
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp
            )

            Text(
                "🤖 AI Analysis in Progress...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                "RunAnywhere SDK is analyzing your files",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "⚡ Typical completion: < 2 seconds",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ... existing code ...

@Composable
fun PatchesAppliedScreen(
    state: InstantDriftFixState.PatchesApplied,
    onDownloadModel: () -> Unit,
    onDownloadData: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedFileType by remember { mutableStateOf<ExportFileType?>(null) }
    
    // File picker for custom save location
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let {
            val file = when (selectedFileType) {
                ExportFileType.MODEL -> state.patchedModelFile
                ExportFileType.DATA -> state.patchedDataFile
                else -> null
            }
            file?.let { saveFileToUri(context, it, uri) }
        }
    }

    if (showExportDialog && selectedFileType != null) {
        ExportOptionsDialog(
            fileName = when (selectedFileType) {
                ExportFileType.MODEL -> state.patchedModelFile.name
                ExportFileType.DATA -> state.patchedDataFile.name
                else -> ""
            },
            file = when (selectedFileType) {
                ExportFileType.MODEL -> state.patchedModelFile
                ExportFileType.DATA -> state.patchedDataFile
                else -> null
            } ?: state.patchedModelFile,
            onDismiss = { showExportDialog = false },
            onDownloadToFolder = {
                selectedFileType?.let {
                    when (it) {
                        ExportFileType.MODEL -> downloadFile(context, state.patchedModelFile)
                        ExportFileType.DATA -> downloadFile(context, state.patchedDataFile)
                    }
                }
                showExportDialog = false
            },
            onSaveToCustomLocation = {
                val fileName = when (selectedFileType) {
                    ExportFileType.MODEL -> state.patchedModelFile.name
                    ExportFileType.DATA -> state.patchedDataFile.name
                    else -> "file.dat"
                }
                saveFileLauncher.launch(fileName)
                showExportDialog = false
            },
            onShare = {
                selectedFileType?.let {
                    when (it) {
                        ExportFileType.MODEL -> shareFile(context, state.patchedModelFile)
                        ExportFileType.DATA -> shareFile(context, state.patchedDataFile)
                    }
                }
                showExportDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "🎉 Patches Applied!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Your files have been successfully patched to reduce drift",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            ImprovementCard(state)
        }

        item {
            Text(
                "📥 Export Patched Files",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Download, save, or share your patched files",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Enhanced Model Export Card
        item {
            EnhancedFileExportCard(
                title = "Patched Model",
                fileName = state.patchedModelFile.name,
                fileSize = formatFileSize(state.patchedModelFile.length()),
                icon = Icons.Default.Memory,
                onExport = {
                    selectedFileType = ExportFileType.MODEL
                    showExportDialog = true
                }
            )
        }

        // Enhanced Data Export Card
        item {
            EnhancedFileExportCard(
                title = "Patched Dataset",
                fileName = state.patchedDataFile.name,
                fileSize = formatFileSize(state.patchedDataFile.length()),
                icon = Icons.Default.TableChart,
                onExport = {
                    selectedFileType = ExportFileType.DATA
                    showExportDialog = true
                }
            )
        }

        // Quick Actions
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "⚡ Quick Actions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                exportBothFiles(context, state.patchedModelFile, state.patchedDataFile)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, "Download both", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download Both")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                shareBothFiles(context, state.patchedModelFile, state.patchedDataFile)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, "Share both", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Both")
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, "Start over")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Over")
            }
        }
    }
}

enum class ExportFileType {
    MODEL, DATA
}

@Composable
fun EnhancedFileExportCard(
    title: String,
    fileName: String,
    fileSize: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    fileSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = onExport,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.FileDownload, "Export", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export")
            }
        }
    }
}

@Composable
fun ExportOptionsDialog(
    fileName: String,
    file: File,
    onDismiss: () -> Unit,
    onDownloadToFolder: () -> Unit,
    onSaveToCustomLocation: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.FileDownload, "Export options", modifier = Modifier.size(32.dp))
        },
        title = {
            Text("Export Options")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Choose how to export:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExportOptionItem(
                    icon = Icons.Default.Folder,
                    title = "Save to Downloads",
                    description = "Save to Downloads/DriftGuardAI folder",
                    onClick = {
                        onDownloadToFolder()
                        onDismiss()
                    }
                )

                ExportOptionItem(
                    icon = Icons.Default.CreateNewFolder,
                    title = "Save to Custom Location",
                    description = "Choose where to save the file",
                    onClick = {
                        onSaveToCustomLocation()
                        onDismiss()
                    }
                )

                ExportOptionItem(
                    icon = Icons.Default.Share,
                    title = "Share File",
                    description = "Share via other apps",
                    onClick = {
                        onShare()
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExportOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnalysisCompleteScreen(
    state: InstantDriftFixState.AnalysisComplete,
    onApplyPatches: (Set<String>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedPatchIds by remember { mutableStateOf(setOf<String>()) }
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tabs for Drift Analysis vs Patch Recommendations
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("📊 Drift Analysis") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("🔧 Patch Fixes") }
            )
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> DriftAnalysisTab(state)
            1 -> PatchRecommendationsTab(
                state = state,
                selectedPatchIds = selectedPatchIds,
                onToggle = { id ->
                    selectedPatchIds = if (selectedPatchIds.contains(id)) {
                        selectedPatchIds - id
                    } else {
                        selectedPatchIds + id
                    }
                },
                onApplyPatches = { onApplyPatches(selectedPatchIds) },
                onCancel = onCancel
            )
        }
    }
}

// --- Visualization TabContent for Drift Analysis and Patch Recommendations ---

@Composable
fun DriftAnalysisTab(state: InstantDriftFixState.AnalysisComplete) {
    val driftResult = state.driftResult

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Card
        item {
            DriftSummaryCard(state)
        }

        // Drift Severity Visual
        item {
            DriftSeverityVisualization(driftResult)
        }

        // Feature-Level Drift Breakdown
        item {
            Text(
                "📊 Feature-Level Drift Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Detailed breakdown of how each feature is affected by drift",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(driftResult.featureDrifts) { featureDrift ->
            FeatureDriftCard(featureDrift)
        }

        // Drift Type Explanation
        item {
            DriftTypeExplanationCard(driftResult.driftType)
        }
    }
}

@Composable
fun DriftSummaryCard(state: InstantDriftFixState.AnalysisComplete) {
    val driftResult = state.driftResult

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                driftResult.driftScore > 0.4 -> MaterialTheme.colorScheme.errorContainer
                driftResult.driftScore > 0.2 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when {
                        driftResult.driftScore > 0.4 -> Icons.Default.Warning
                        driftResult.driftScore > 0.2 -> Icons.Default.Info
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = "Drift severity",
                    modifier = Modifier.size(48.dp),
                    tint = when {
                        driftResult.driftScore > 0.4 -> MaterialTheme.colorScheme.error
                        driftResult.driftScore > 0.2 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "🤖 AI Drift Analysis Complete",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        state.modelInfo.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Drift Score Badge - FIXED: More readable colors
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        driftResult.driftScore > 0.4 -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        driftResult.driftScore > 0.2 -> MaterialTheme.colorScheme.tertiary.copy(
                            alpha = 0.9f
                        )
                        else -> MaterialTheme.colorScheme.primary
                    }
                ) {
                    Text(
                        String.format("%.1f%%", driftResult.driftScore * 100),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            driftResult.driftScore > 0.4 -> MaterialTheme.colorScheme.onError
                            driftResult.driftScore > 0.2 -> MaterialTheme.colorScheme.onTertiary
                            else -> MaterialTheme.colorScheme.onPrimary
                        }
                    )
                }
            }

            HorizontalDivider()

            // Key Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricItem(
                    modifier = Modifier.weight(1f),
                    title = "Status",
                    value = if (driftResult.isDriftDetected) "⚠️ Drift" else "✅ Clean",
                    color = if (driftResult.isDriftDetected)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )

                MetricItem(
                    modifier = Modifier.weight(1f),
                    title = "Type",
                    value = driftResult.driftType.name.replace("_", " "),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricItem(
                    modifier = Modifier.weight(1f),
                    title = "Drifted Features",
                    value = "${driftResult.featureDrifts.count { it.isDrifted }}",
                    color = MaterialTheme.colorScheme.error
                )

                MetricItem(
                    modifier = Modifier.weight(1f),
                    title = "Total Features",
                    value = "${driftResult.featureDrifts.size}",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun DriftSeverityVisualization(driftResult: DriftResult) {
    // FIXED: Theme-aware colors for better visibility in dark mode
    val lowDriftColor = MaterialTheme.colorScheme.primary
    val moderateDriftColor = MaterialTheme.colorScheme.tertiary
    val highDriftColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    val progressBarBackground = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "📈 Drift Severity Meter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Visual drift meter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(progressBarBackground)
            ) {
                // Filled portion
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(driftResult.driftScore.toFloat())
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                driftResult.driftScore > 0.4 -> highDriftColor
                                driftResult.driftScore > 0.2 -> moderateDriftColor
                                else -> lowDriftColor
                            }
                        )
                )

                // Marker
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(driftResult.driftScore.toFloat())
                        .align(Alignment.CenterStart)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterEnd),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            3.dp,
                            when {
                                driftResult.driftScore > 0.4 -> highDriftColor
                                driftResult.driftScore > 0.2 -> moderateDriftColor
                                else -> lowDriftColor
                            }
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                String.format("%.0f", driftResult.driftScore * 100),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem("0% Low", lowDriftColor)
                LegendItem("20% Moderate", moderateDriftColor)
                LegendItem("40%+ High", highDriftColor)
            }

            // Interpretation
            Surface(
                color = when {
                    driftResult.driftScore > 0.4 -> MaterialTheme.colorScheme.errorContainer
                    driftResult.driftScore > 0.2 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    when {
                        driftResult.driftScore > 0.4 -> "⚠️ Critical: Immediate patching required"
                        driftResult.driftScore > 0.2 -> "⚡ Moderate: Patching recommended"
                        else -> "✅ Low: Model is healthy"
                    },
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        driftResult.driftScore > 0.4 -> MaterialTheme.colorScheme.onErrorContainer
                        driftResult.driftScore > 0.2 -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
        }
    }
}

@Composable
fun LegendItem(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun FeatureDriftCard(featureDrift: com.driftdetector.app.domain.model.FeatureDrift) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (featureDrift.isDrifted)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (featureDrift.isDrifted) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (featureDrift.isDrifted)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        featureDrift.featureName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = if (featureDrift.isDrifted)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    else
                        MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        String.format("%.1f%%", featureDrift.driftScore * 100),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (featureDrift.isDrifted)
                            MaterialTheme.colorScheme.onError
                        else
                            MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Drift Score Bar - FIXED: Theme-aware background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(featureDrift.driftScore.toFloat())
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (featureDrift.isDrifted)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                )
            }

            // Statistics - FIXED: Better text visibility
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    "PSI Score",
                    featureDrift.psiScore?.let { String.format("%.4f", it) } ?: "N/A")
                StatItem(
                    "KS Statistic",
                    featureDrift.ksStatistic?.let { String.format("%.4f", it) } ?: "N/A")
                StatItem(
                    "Status",
                    if (featureDrift.isDrifted) "Drifted" else "Clean"
                )
            }

            // Details - FIXED: Better textbox visibility
            if (featureDrift.isDrifted) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "This feature shows significant distribution shift. Recommended actions: Normalization update or feature clipping.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DriftTypeExplanationCard(driftType: DriftType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    "Understanding ${driftType.name.replace("_", " ")}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                when (driftType) {
                    DriftType.COVARIATE_DRIFT ->
                        "Input feature distributions have changed. Your model receives different data patterns than during training."
                    DriftType.PRIOR_DRIFT ->
                        "Output label distributions have changed. The proportion of different classes in your data has shifted."
                    DriftType.CONCEPT_DRIFT ->
                        "The relationship between inputs and outputs has changed. Same inputs now lead to different outputs."
                    else -> "General data distribution changes detected."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            Text(
                "💡 Recommended Actions:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            when (driftType) {
                DriftType.COVARIATE_DRIFT -> {
                    ActionItem("Update feature normalization parameters")
                    ActionItem("Apply feature clipping for outliers")
                    ActionItem("Consider feature reweighting")
                }
                DriftType.PRIOR_DRIFT -> {
                    ActionItem("Adjust decision thresholds")
                    ActionItem("Rebalance training data")
                    ActionItem("Update output calibration")
                }
                DriftType.CONCEPT_DRIFT -> {
                    ActionItem("Retrain model with recent data")
                    ActionItem("Apply all available patches")
                    ActionItem("Monitor closely after patching")
                }
                else -> {
                    ActionItem("Review and apply AI-recommended patches")
                }
            }
        }
    }
}

@Composable
fun ActionItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "→",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun PatchRecommendationsTab(
    state: InstantDriftFixState.AnalysisComplete,
    selectedPatchIds: Set<String>,
    onToggle: (String) -> Unit,
    onApplyPatches: () -> Unit,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.patchCandidates.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🤖 AI-Recommended Fixes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${state.patchCandidates.count { it.isRecommended }} patches recommended by AI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "${selectedPatchIds.size}",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Select patches to apply:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(state.patchCandidates) { candidate ->
                EnhancedPatchCandidateCard(
                    candidate = candidate,
                    isSelected = selectedPatchIds.contains(candidate.id),
                    onToggle = { onToggle(candidate.id) },
                    driftResult = state.driftResult
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onApplyPatches,
                        modifier = Modifier.weight(1f),
                        enabled = selectedPatchIds.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Build, "Apply patches")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply ${selectedPatchIds.size} Patch${if (selectedPatchIds.size != 1) "es" else ""}")
                    }
                }
            }
        } else {
            item {
                NoPatchesRequiredCard(onCancel)
            }
        }
    }
}

@Composable
fun EnhancedPatchCandidateCard(
    candidate: PatchCandidate,
    isSelected: Boolean,
    onToggle: () -> Unit,
    driftResult: DriftResult
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (candidate.isRecommended && !isSelected) 3.dp else 1.dp,
            color = if (candidate.isRecommended && !isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            candidate.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (candidate.isRecommended) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        "AI Recommended",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        "AI Pick",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        candidate.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Impact Visualization
            ImpactVisualization(
                expectedReduction = candidate.estimatedDriftReduction,
                safetyScore = candidate.safetyScore,
                originalDrift = driftResult.driftScore
            )

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Expected Impact",
                    value = candidate.expectedImprovement,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )

                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Safety",
                    value = String.format("%.0f%%", candidate.safetyScore * 100),
                    color = if (candidate.safetyScore > 0.8)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.tertiaryContainer
                )
            }

            // Technical Details (expandable)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "This patch addresses ${getAffectedFeatures(candidate, driftResult)} drifted features",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ImpactVisualization(
    expectedReduction: Double,
    safetyScore: Double,
    originalDrift: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Expected Drift Reduction:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Before bar - FIXED: Theme-aware error color
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Before",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                ) {
                    Text(
                        String.format("%.0f%%", originalDrift * 100),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            // After bar - FIXED: Theme-aware success color
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "After",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    val afterDrift = originalDrift * (1 - expectedReduction)
                    Text(
                        String.format("%.0f%%", afterDrift * 100),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun NoPatchesRequiredCard(onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "No drift",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "✅ No Patches Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Your model and data are well-aligned. No drift detected that requires patching!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Default.Home, "Back")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Dashboard")
            }
        }
    }
}

fun getAffectedFeatures(candidate: PatchCandidate, driftResult: DriftResult): Int {
    return when (candidate.patch.patchType) {
        PatchType.FEATURE_CLIPPING,
        PatchType.NORMALIZATION_UPDATE,
        PatchType.FEATURE_REWEIGHTING -> {
            driftResult.featureDrifts.count { it.isDrifted }
        }
        else -> driftResult.featureDrifts.size
    }
}

// ... existing code ...

@Composable
fun DriftResultCard(state: InstantDriftFixState.AnalysisComplete) {
    val driftResult = state.driftResult

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (driftResult.isDriftDetected)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (driftResult.isDriftDetected) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = if (driftResult.isDriftDetected) "Drift detected" else "No drift",
                    tint = if (driftResult.isDriftDetected)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )

                Column {
                    Text(
                        "🤖 AI Drift Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        state.modelInfo.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Status:", fontWeight = FontWeight.Medium)
                Text(
                    if (driftResult.isDriftDetected) "⚠️ Drift Detected" else "✅ No Drift",
                    fontWeight = FontWeight.Bold,
                    color = if (driftResult.isDriftDetected)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Drift Score:", fontWeight = FontWeight.Medium)
                Text(
                    String.format("%.3f", driftResult.driftScore),
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Drift Type:", fontWeight = FontWeight.Medium)
                Text(
                    driftResult.driftType.name.replace("_", " "),
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Affected Features:", fontWeight = FontWeight.Medium)
                Text(
                    "${driftResult.featureDrifts.count { it.isDrifted }} / ${driftResult.featureDrifts.size}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PatchCandidateCard(
    candidate: PatchCandidate,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (candidate.isRecommended && !isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        candidate.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (candidate.isRecommended) {
                        Icon(
                            Icons.Default.Star,
                            "AI Recommended",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    candidate.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            candidate.expectedImprovement,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "Safety: ${String.format("%.0f%%", candidate.safetyScore * 100)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApplyingPatchesScreen(state: InstantDriftFixState.ApplyingPatches) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp
            )

            Text(
                "🔨 Applying Patches...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                "RunAnywhere SDK is generating patched files",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${state.selectedPatches.size} patch(es) selected",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ImprovementCard(state: InstantDriftFixState.PatchesApplied) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "📊 AI Analysis Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Original Drift Score:")
                Text(
                    String.format("%.3f", state.originalDriftScore),
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Drift Reduction:")
                Text(
                    "${state.driftReduction.toInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Patches Applied:")
                Text(
                    "${state.appliedPatches.size}",
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            Text(
                "✨ Patched files ready! Use them in your production pipeline to maintain model accuracy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                "⚠️ Error",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Default.Refresh, "Try again")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

// Helper functions
private fun extractFileName(context: Context, uri: Uri): String {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    } ?: uri.lastPathSegment ?: "unknown_file"
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun downloadFile(context: Context, file: File) {
    try {
        // Copy file to Downloads folder
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val driftGuardDir = File(downloadsDir, "DriftGuardAI")
        if (!driftGuardDir.exists()) {
            driftGuardDir.mkdirs()
        }

        val destFile = File(driftGuardDir, file.name)
        file.copyTo(destFile, overwrite = true)

        Timber.i("✅ File downloaded to: ${destFile.absolutePath}")

        // Show success message
        android.widget.Toast.makeText(
            context,
            "✅ Downloaded to: Downloads/DriftGuardAI/${file.name}",
            android.widget.Toast.LENGTH_LONG
        ).show()

        // Try to open the file
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Open downloaded file"))
        } catch (e: Exception) {
            // File downloaded but can't open - that's okay
            Timber.d("File downloaded successfully but couldn't auto-open")
        }

    } catch (e: Exception) {
        Timber.e(e, "Failed to download file")
        android.widget.Toast.makeText(
            context,
            "❌ Download failed: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

private fun saveFileToUri(context: Context, sourceFile: File, destinationUri: Uri) {
    try {
        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            sourceFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        Timber.i("✅ File saved to custom location")
        android.widget.Toast.makeText(
            context,
            "✅ File saved successfully",
            android.widget.Toast.LENGTH_SHORT
        ).show()

    } catch (e: Exception) {
        Timber.e(e, "Failed to save file")
        android.widget.Toast.makeText(
            context,
            "❌ Save failed: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

private fun shareFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Patched ML Model from DriftGuardAI")
            putExtra(Intent.EXTRA_TEXT, "Sharing patched file: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share patched file"))
        
        Timber.i("✅ Share dialog opened for file: ${file.name}")

    } catch (e: Exception) {
        Timber.e(e, "Failed to share file")
        android.widget.Toast.makeText(
            context,
            "❌ Share failed: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

private fun exportBothFiles(context: Context, modelFile: File, dataFile: File) {
    try {
        downloadFile(context, modelFile)
        // Small delay to show both toasts
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            downloadFile(context, dataFile)
        }, 500)
    } catch (e: Exception) {
        Timber.e(e, "Failed to export both files")
        android.widget.Toast.makeText(
            context,
            "❌ Export failed: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

private fun shareBothFiles(context: Context, modelFile: File, dataFile: File) {
    try {
        val modelUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            modelFile
        )
        val dataUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            dataFile
        )

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(listOf(modelUri, dataUri)))
            putExtra(Intent.EXTRA_SUBJECT, "Patched ML Files from DriftGuardAI")
            putExtra(Intent.EXTRA_TEXT, "Sharing patched model and dataset")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share patched files"))
        
        Timber.i("✅ Share dialog opened for both files")

    } catch (e: Exception) {
        Timber.e(e, "Failed to share both files")
        android.widget.Toast.makeText(
            context,
            "❌ Share failed: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
