package com.driftdetector.app.presentation.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.MLModel
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.presentation.viewmodel.ModelUploadViewModel
import com.driftdetector.app.presentation.viewmodel.UploadMethod
import com.driftdetector.app.presentation.viewmodel.UploadedFile
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelUploadScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToPatches: () -> Unit = {},
    viewModel: ModelUploadViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Model & Data") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Section
            item {
                HeroSection()
            }
            
            // Upload Methods
            item {
                Text(
                    "Choose Upload Method",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                UploadMethodsGrid(
                    selectedMethod = uiState.selectedMethod,
                    onMethodSelect = { viewModel.selectMethod(it) }
                )
            }
            
            // Upload Area based on selected method
            item {
                AnimatedVisibility(
                    visible = uiState.selectedMethod != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    when (uiState.selectedMethod) {
                        UploadMethod.LOCAL_FILE -> LocalFileUploadSection(
                            onFilesSelected = { viewModel.uploadFiles(it) }
                        )
                        UploadMethod.CLOUD_STORAGE -> CloudStorageSection(
                            onConnect = { provider -> viewModel.connectCloudStorage(provider) }
                        )
                        UploadMethod.URL_IMPORT -> UrlImportSection(
                            onImportUrl = { viewModel.importFromUrl(it) }
                        )
                        UploadMethod.DRAG_DROP -> DragDropSection(
                            onFilesDropped = { viewModel.uploadFiles(it) }
                        )
                        null -> {}
                    }
                }
            }
            
            // Upload Progress
            if (uploadProgress > 0f && uploadProgress < 1f) {
                item {
                    if (uiState.isProcessing) {
                        ProcessingProgressCard(progress = uploadProgress)
                    } else {
                        UploadProgressCard(progress = uploadProgress)
                    }
                }
            }
            
            // Success/Error Messages
            if (uiState.successMessage != null || uiState.error != null) {
                item {
                    MessageCard(
                        successMessage = uiState.successMessage,
                        errorMessage = uiState.error,
                        onDismiss = { viewModel.clearMessages() }
                    )
                }
            }
            
            // Processing Results
            uiState.processedModel?.let { model ->
                item {
                    val driftResult = uiState.detectedDrift
                    if (driftResult != null) {
                        // Show full processing results with drift detection
                        ProcessingResultsCard(
                            model = model,
                            driftResult = driftResult,
                            patch = uiState.synthesizedPatch,
                            onNavigateToDashboard = onNavigateToDashboard,
                            onNavigateToPatches = onNavigateToPatches
                        )
                    } else {
                        // Show model registration success card
                        ModelRegisteredCard(
                            model = model,
                            onNavigateToDashboard = onNavigateToDashboard
                        )
                    }
                }
            }
            
            // Uploaded Files
            if (uiState.uploadedFiles.isNotEmpty()) {
                item {
                    Text(
                        "Uploaded Files (${uiState.uploadedFiles.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(uiState.uploadedFiles) { file ->
                    UploadedFileCard(
                        file = file,
                        onRemove = { viewModel.removeFile(file.id) },
                        onConfigure = { viewModel.configureFile(file.id) }
                    )
                }
            }
            
            // Feature Highlights
            if (uiState.uploadedFiles.isEmpty()) {
                item {
                    FeatureHighlights()
                }
            }
            
            // Spacer for bottom padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HeroSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "Upload model to cloud",
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Upload Your ML Models & Data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Securely upload TensorFlow Lite, ONNX models, and datasets in CSV, JSON, or Parquet formats",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun UploadMethodsGrid(
    selectedMethod: UploadMethod?,
    onMethodSelect: (UploadMethod) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UploadMethodCard(
                method = UploadMethod.LOCAL_FILE,
                icon = Icons.Default.FolderOpen,
                title = "Local Files",
                description = "Browse device storage",
                isSelected = selectedMethod == UploadMethod.LOCAL_FILE,
                onClick = { onMethodSelect(UploadMethod.LOCAL_FILE) },
                modifier = Modifier.weight(1f)
            )
            
            UploadMethodCard(
                method = UploadMethod.CLOUD_STORAGE,
                icon = Icons.Default.Cloud,
                title = "Cloud Storage",
                description = "Google Drive, Dropbox",
                isSelected = selectedMethod == UploadMethod.CLOUD_STORAGE,
                onClick = { onMethodSelect(UploadMethod.CLOUD_STORAGE) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UploadMethodCard(
                method = UploadMethod.URL_IMPORT,
                icon = Icons.Default.Link,
                title = "URL Import",
                description = "Import from web link",
                isSelected = selectedMethod == UploadMethod.URL_IMPORT,
                onClick = { onMethodSelect(UploadMethod.URL_IMPORT) },
                modifier = Modifier.weight(1f)
            )
            
            UploadMethodCard(
                method = UploadMethod.DRAG_DROP,
                icon = Icons.Default.DragIndicator,
                title = "Drag & Drop",
                description = "Drop files here",
                isSelected = selectedMethod == UploadMethod.DRAG_DROP,
                onClick = { onMethodSelect(UploadMethod.DRAG_DROP) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadMethodCard(
    method: UploadMethod,
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier.scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = when (method) {
                    UploadMethod.LOCAL_FILE -> "Local file upload"
                    UploadMethod.CLOUD_STORAGE -> "Cloud storage upload"
                    UploadMethod.URL_IMPORT -> "URL import upload"
                    UploadMethod.DRAG_DROP -> "Drag and drop upload"
                },
                modifier = Modifier.size(40.dp),
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LocalFileUploadSection(
    onFilesSelected: (List<Uri>) -> Unit
) {
    // Use OpenMultipleDocuments to allow selecting multiple files at once
    val multipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onFilesSelected(uris)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = "Browse local files",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        "Select Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Choose both model and data files together",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

            // Instructions Card
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Upload instructions",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "📝 How to Upload",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        "1. Click the button below to open file picker",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "2. Select your model file (e.g., model.tflite)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "3. Hold Ctrl/Cmd and click your data file (e.g., data.csv)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "4. Click 'Open' to upload both files together",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = "Upload tip",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "💡 Tip: You can also select files one at a time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // Main Upload Button
            Button(
                onClick = {
                    // Open file picker allowing multiple file selection
                    multipleFilesLauncher.launch(arrayOf("*/*"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.FileUpload,
                    contentDescription = "Upload file button",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Select Model & Data Files",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Browse and select files",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Divider()
            
            // Supported formats info
            SupportedFormatsInfo()

            Divider()

            // Offline Access Note
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = "Offline access available",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Works offline! Access previously synced files from Google Drive, OneDrive, and Dropbox without internet connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CloudStorageSection(
    onConnect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Connect Cloud Storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Google Drive
            FilledTonalButton(
                onClick = { onConnect("google_drive") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF4285F4).copy(alpha = 0.1f)
                )
            ) {
                Icon(Icons.Default.CloudCircle, contentDescription = "Google Drive")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Google Drive")
            }
            
            // Dropbox
            FilledTonalButton(
                onClick = { onConnect("dropbox") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF0061FF).copy(alpha = 0.1f)
                )
            ) {
                Icon(Icons.Default.Cloud, contentDescription = "Dropbox")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Dropbox")
            }
            
            // OneDrive
            FilledTonalButton(
                onClick = { onConnect("onedrive") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF0078D4).copy(alpha = 0.1f)
                )
            ) {
                Icon(Icons.Default.Cloud, contentDescription = "OneDrive")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect OneDrive")
            }
        }
    }
}

@Composable
fun UrlImportSection(
    onImportUrl: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Import from URL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            OutlinedTextField(
                value = url,
                onValueChange = { 
                    url = it
                    showError = false
                },
                label = { Text("File URL") },
                placeholder = { Text("https://example.com/model.tflite") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = "URL link input")
                },
                isError = showError,
                supportingText = if (showError) {
                    { Text("Please enter a valid URL") }
                } else {
                    { Text("Enter a direct link to your model or dataset file") }
                }
            )
            
            Button(
                onClick = {
                    if (url.isBlank() || !url.startsWith("http")) {
                        showError = true
                    } else {
                        onImportUrl(url)
                        url = ""
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = url.isNotBlank()
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download from URL")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import from URL")
            }
        }
    }
}

@Composable
fun DragDropSection(
    onFilesDropped: (List<Uri>) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dragdrop")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Use OpenMultipleDocuments for better offline support (Google Drive)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onFilesDropped(uris)
        }
    }

    Card(
        onClick = {
            // Open documents with all file types, allowing offline access
            fileLauncher.launch(arrayOf("*/*"))
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Drag and drop area",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                
                Text(
                    "Drag & Drop Files Here",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    "Or click to browse files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                FilledTonalButton(
                    onClick = { fileLauncher.launch(arrayOf("*/*")) }
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Browse files button")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Files")
                }
            }
        }
    }
}

@Composable
fun UploadProgressCard(progress: Float) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(40.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Uploading Files...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${(progress * 100).toInt()}% Complete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun UploadedFileCard(
    file: UploadedFile,
    onRemove: () -> Unit,
    onConfigure: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (file.processed)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (file.processed)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            file.processed -> Icons.Default.CheckCircle
                            file.isModel -> Icons.Default.Memory
                            else -> Icons.Default.TableChart
                        },
                        contentDescription = when {
                            file.processed -> "File processed successfully"
                            file.isModel -> "Model file"
                            else -> "Data file"
                        },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = file.size,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (file.format != null) {
                    AssistChip(
                        onClick = { },
                        label = { Text(file.format, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                when {
                    file.processingStatus == "UPLOADING" -> Text(
                        text = "Uploading...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    file.processingStatus == "PROCESSING" -> Text(
                        text = "Processing...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    file.processingStatus == "PROCESSED" -> Text(
                        text = "Processed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    file.processingStatus == "FAILED" -> Text(
                        text = "Upload Failed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Actions
            if (!file.processed) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onConfigure) {
                        Icon(Icons.Default.Settings, "Configure")
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, "Remove")
                    }
                }
            }
        }
    }
}

@Composable
fun SupportedFormatsInfo() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Supported Formats:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        
        FormatChipRow(
            title = "Models",
            formats = listOf(".tflite", ".onnx", ".h5", ".pb")
        )
        
        FormatChipRow(
            title = "Data",
            formats = listOf(".csv", ".json", ".parquet", ".avro")
        )
    }
}

@Composable
fun FormatChipRow(
    title: String,
    formats: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$title:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            formats.forEach { format ->
                SuggestionChip(
                    onClick = { },
                    label = { Text(format, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
fun FeatureHighlights() {
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
                "✨ Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            FeatureItem(
                icon = Icons.Default.Security,
                title = "Secure & Private",
                description = "All uploads are encrypted and stored locally on your device"
            )
            
            FeatureItem(
                icon = Icons.Default.Speed,
                title = "Auto-Detection",
                description = "Automatically extracts model metadata and data schemas"
            )
            
            FeatureItem(
                icon = Icons.Default.CheckCircle,
                title = "Validation",
                description = "Real-time validation ensures compatibility before upload"
            )
            
            FeatureItem(
                icon = Icons.Default.CloudDone,
                title = "Cloud Integration",
                description = "Connect to Google Drive, Dropbox, and more"
            )
        }
    }
}

@Composable
fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UploadLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun UploadErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Upload error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Error",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProcessingProgressCard(progress: Float) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(40.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Processing Files...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Detecting drift & synthesizing patches",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun MessageCard(
    successMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    if (successMessage != null || errorMessage != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (errorMessage != null) 
                    MaterialTheme.colorScheme.errorContainer 
                else 
                    MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (errorMessage != null) Icons.Default.Error else Icons.Default.CheckCircle,
                    contentDescription = if (errorMessage != null) "Error message" else "Success message",
                    tint = if (errorMessage != null) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.tertiary
                )
                
                Text(
                    text = errorMessage ?: successMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Dismiss")
                }
            }
        }
    }
}

@Composable
fun ProcessingResultsCard(
    model: MLModel,
    driftResult: DriftResult,
    patch: Patch?,
    onNavigateToDashboard: () -> Unit,
    onNavigateToPatches: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (driftResult.isDriftDetected)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (driftResult.isDriftDetected) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = if (driftResult.isDriftDetected) "Drift warning" else "No drift detected",
                    tint = if (driftResult.isDriftDetected)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                Column {
                    Text(
                        "Processing Results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        model.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider()
            
            // Drift Results
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Drift Detection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                ResultRow(
                    label = "Status",
                    value = if (driftResult.isDriftDetected) "⚠️ Drift Detected" else "✅ No Drift",
                    valueColor = if (driftResult.isDriftDetected) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
                
                ResultRow(
                    label = "Drift Score",
                    value = String.format("%.3f", driftResult.driftScore)
                )
                
                ResultRow(
                    label = "Drift Type",
                    value = driftResult.driftType.name.replace("_", " ")
                )
                
                ResultRow(
                    label = "Features Drifted",
                    value = "${driftResult.featureDrifts.count { it.isDrifted }} / ${driftResult.featureDrifts.size}"
                )
            }
            
            // Patch Results (if synthesized)
            if (patch != null) {
                Divider()
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "✨ Synthesized Patch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    ResultRow(
                        label = "Patch Type",
                        value = patch.patchType.name.replace("_", " ")
                    )
                    
                    ResultRow(
                        label = "Safety Score",
                        value = String.format("%.2f", patch.validationResult?.metrics?.safetyScore ?: 0.0),
                        valueColor = when {
                            (patch.validationResult?.metrics?.safetyScore ?: 0.0) > 0.7 -> MaterialTheme.colorScheme.primary
                            (patch.validationResult?.metrics?.safetyScore ?: 0.0) > 0.5 -> Color(0xFFFFA500)
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    
                    ResultRow(
                        label = "Status",
                        value = patch.status.name.replace("_", " ")
                    )
                }
            }
            
            // Action Buttons
            if (driftResult.isDriftDetected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToDashboard,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = "View dashboard")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Dashboard")
                    }
                    
                    if (patch != null) {
                        Button(
                            onClick = onNavigateToPatches,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "View patches")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Patches")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelRegisteredCard(
    model: MLModel,
    onNavigateToDashboard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Model registered successfully",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Column {
                    Text(
                        "Model Registered Successfully",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        model.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Model Information
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Model Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                ResultRow(
                    label = "Version",
                    value = model.version
                )
                
                ResultRow(
                    label = "Input Features",
                    value = "${model.inputFeatures.size} features"
                )
                
                ResultRow(
                    label = "Output Classes",
                    value = "${model.outputLabels.size} classes"
                )
                
                ResultRow(
                    label = "Status",
                    value = if (model.isActive) "✅ Active" else "❌ Inactive",
                    valueColor = if (model.isActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
            
            Divider()
            
            // Next Steps
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Next steps information",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Next Steps",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        "1. Upload a dataset (.csv, .json, .parquet)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "2. System will automatically detect drift",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "3. Patches will be generated if drift is detected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Action Button
            Button(
                onClick = onNavigateToDashboard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Dashboard, contentDescription = "Go to dashboard")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Go to Dashboard")
            }
        }
    }
}

@Composable
fun ResultRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
