package com.example.familyvault

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy
import java.io.File

class PreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uriString = intent.getStringExtra("uri")
        val uri = Uri.parse(uriString)

        setContent {
            // Dark Theme Color Scheme
            val darkColorScheme = darkColorScheme(
                primary = Color(0xFF4CAF50),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF1B5E20),
                onPrimaryContainer = Color(0xFFA5D6A7),
                secondary = Color(0xFF2196F3),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFF0D47A1),
                onSecondaryContainer = Color(0xFF90CAF9),
                tertiary = Color(0xFFFF9800),
                onTertiary = Color.Black,
                tertiaryContainer = Color(0xFFE65100),
                onTertiaryContainer = Color(0xFFFFE0B2),
                background = Color(0xFF121212),
                onBackground = Color(0xFFE0E0E0),
                surface = Color(0xFF1E1E1E),
                onSurface = Color(0xFFE0E0E0),
                surfaceVariant = Color(0xFF2C2C2C),
                onSurfaceVariant = Color(0xFFB0B0B0),
                error = Color(0xFFCF6679),
                onError = Color.Black
            )

            MaterialTheme(colorScheme = darkColorScheme) {
                PreviewScreen(uri)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(uri: Uri) {
    val context = LocalContext.current

    val fileName = uri.lastPathSegment ?: "Unknown File"
    val extension = fileName.substringAfterLast('.', "").lowercase()

    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = if (isFullscreen) Color.Black else MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                PreviewTopBar(
                    fileName = fileName,
                    extension = extension,
                    isFullscreen = isFullscreen,
                    onFullscreenToggle = { isFullscreen = !isFullscreen },
                    onBack = { (context as? ComponentActivity)?.finish() },
                    onShare = { showShareDialog = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else innerPadding)
                .clickable(
                    onClick = { showControls = !showControls },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            when (extension) {
                "jpg", "jpeg", "png" -> {
                    ZoomableImage(
                        uri = uri,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "pdf" -> {
                    PDFViewer(
                        uri = uri,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    UnsupportedPreview(fileName = fileName)
                }
            }
        }
    }

    // Share Dialog
    if (showShareDialog) {
        ShareDialog(
            uri = uri,
            fileName = fileName,
            onDismiss = { showShareDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewTopBar(
    fileName: String,
    extension: String,
    isFullscreen: Boolean,
    onFullscreenToggle: () -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // File Type Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(getFileTypeColor(extension).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getFileTypeIcon(extension),
                        contentDescription = null,
                        tint = getFileTypeColor(extension),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        fileName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        extension.uppercase(),
                        fontSize = 12.sp,
                        color = (if (isFullscreen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onBackground
                )
            }
        },
        actions = {
            // Share Button
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onBackground
                )
            }

            // Fullscreen Toggle
            IconButton(onClick = onFullscreenToggle) {
                Icon(
                    if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                    tint = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isFullscreen) Color.Black.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            scrolledContainerColor = if (isFullscreen) Color.Black.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
        )
    )
}

@Composable
fun ShareDialog(
    uri: Uri,
    fileName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Share File",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Choose how to share:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Share Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShareOption(
                        icon = Icons.Outlined.Chat,
                        label = "WhatsApp",
                        color = Color(0xFF25D366),
                        onClick = {
                            shareToWhatsApp(context, uri, fileName)
                            onDismiss()
                        }
                    )

                    ShareOption(
                        icon = Icons.Outlined.Share,
                        label = "More",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            shareGeneric(context, uri, fileName)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ShareOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Share Functions
fun shareToWhatsApp(context: android.content.Context, uri: Uri, fileName: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(fileName)
            putExtra(Intent.EXTRA_STREAM, getShareableUri(context, uri))
            putExtra(Intent.EXTRA_TEXT, "")
            `package` = "com.whatsapp"
        }
        context.startActivity(Intent.createChooser(intent, "Share via WhatsApp"))
    } catch (e: Exception) {
        // WhatsApp not installed, fallback to generic share
        shareGeneric(context, uri, fileName)
    }
}

fun shareGeneric(context: android.content.Context, uri: Uri, fileName: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = getMimeType(fileName)
        putExtra(Intent.EXTRA_STREAM, getShareableUri(context, uri))
        putExtra(Intent.EXTRA_TEXT, "")
    }
    context.startActivity(Intent.createChooser(intent, "Share File"))
}

fun getShareableUri(context: android.content.Context, uri: Uri): Uri {
    return try {
        // If it's already a content URI, use it directly
        if (uri.scheme == "content") {
            uri
        } else {
            // Convert file URI to content URI using FileProvider
            val file = File(uri.path ?: return uri)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }
    } catch (e: Exception) {
        uri
    }
}

fun getMimeType(fileName: String): String {
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "doc", "docx" -> "application/msword"
        "xls", "xlsx" -> "application/vnd.ms-excel"
        else -> "*/*"
    }
}

@Composable
fun ZoomableImage(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var rotation by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotationDelta ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                    rotation += rotationDelta
                }
            }
    ) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    rotationZ = rotation
                )
        )

        // Reset Button (appears when zoomed or transformed)
        AnimatedVisibility(
            visible = scale > 1f || offsetX != 0f || offsetY != 0f || rotation != 0f,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                    rotation = 0f
                },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset View"
                )
            }
        }
    }
}

@Composable
fun PDFViewer(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(0) }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PDFView(ctx, null).apply {
                    fromUri(uri)
                        .enableSwipe(true)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .onLoad { nbPages ->
                            totalPages = nbPages
                        }
                        .onPageChange { page, _ ->
                            currentPage = page
                        }
                        .pageFitPolicy(FitPolicy.WIDTH)
                        .load()
                }
            }
        )

        // Page Indicator
        if (totalPages > 0) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "${currentPage + 1} / $totalPages",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun UnsupportedPreview(fileName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Preview Not Supported",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                fileName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "This file type cannot be previewed",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// Helper functions
fun getFileTypeIcon(extension: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (extension.lowercase()) {
        "jpg", "jpeg", "png" -> Icons.Outlined.Image
        "pdf" -> Icons.Outlined.PictureAsPdf
        else -> Icons.Outlined.InsertDriveFile
    }
}

fun getFileTypeColor(extension: String): Color {
    return when (extension.lowercase()) {
        "jpg", "jpeg", "png" -> Color(0xFF4CAF50)
        "pdf" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
}