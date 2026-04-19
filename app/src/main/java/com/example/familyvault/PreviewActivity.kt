package com.example.familyvault

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.github.barteksc.pdfviewer.PDFView

class PreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uriString = intent.getStringExtra("uri")
        val uri = Uri.parse(uriString)

        setContent {
            PreviewScreen(uri)
        }
    }
}

@Composable
fun PreviewScreen(uri: Uri) {

    val fileName = uri.lastPathSegment ?: ""
    val extension = fileName.substringAfterLast('.', "").lowercase()

    when (extension) {

        "jpg", "jpeg", "png" -> {
            // 🖼️ ZOOMABLE IMAGE
            ZoomableImage(uri)
        }

        "pdf" -> {
            // 📄 PDF
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PDFView(ctx, null).apply {
                        fromUri(uri)
                            .enableSwipe(true)
                            .load()
                    }
                }
            )
        }

        else -> {
            androidx.compose.material3.Text("Preview not supported")
        }
    }
}

@Composable
fun ZoomableImage(uri: Uri) {

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
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
                    translationY = offsetY
                )
        )
    }
}