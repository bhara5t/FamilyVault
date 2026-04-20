package com.example.familyvault

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.familyvault.data.AppDatabase
import com.example.familyvault.data.FamilyMember
import com.example.familyvault.security.FileSecurity
import com.example.familyvault.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MemberDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val memberId = intent.getIntExtra("memberId", -1)
        val db = AppDatabase.getDatabase(this)

        setContent {
            val darkColorScheme = darkColorScheme(
                primary = DarkPrimary,
                onPrimary = DarkOnPrimary,
                primaryContainer = DarkPrimaryContainer,
                onPrimaryContainer = DarkOnPrimaryContainer,
                secondary = DarkSecondary,
                onSecondary = DarkOnSecondary,
                secondaryContainer = DarkSecondaryContainer,
                onSecondaryContainer = DarkOnSecondaryContainer,
                tertiary = DarkTertiary,
                onTertiary = DarkOnTertiary,
                tertiaryContainer = DarkTertiaryContainer,
                onTertiaryContainer = DarkOnTertiaryContainer,
                background = DarkBackground,
                onBackground = DarkOnBackground,
                surface = DarkSurface,
                onSurface = DarkOnSurface,
                surfaceVariant = DarkSurfaceVariant,
                onSurfaceVariant = DarkOnSurfaceVariant,
                error = DarkError,
                onError = DarkOnError
            )

            MaterialTheme(colorScheme = darkColorScheme) {
                // Set window background to dark to prevent white flash
                SideEffect {
                    window.statusBarColor = DarkBackground.toArgb()
                    window.navigationBarColor = DarkSurface.toArgb()
                }
                DetailScreen(db, memberId)
            }
        }
    }
}

// Extension function to convert Color to Int
fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(db: AppDatabase, memberId: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var member by remember { mutableStateOf<FamilyMember?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(memberId) {
        delay(50)
        member = db.familyDao().getMemberById(memberId)
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Document Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = isLoading to member,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) +
                            slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(150))
                },
                label = "content"
            ) { (loading, loadedMember) ->
                when {
                    loading -> {
                        // Dark loading screen
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Loading...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    loadedMember == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Document not found",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp
                            )
                        }
                    }
                    else -> {
                        MemberContent(
                            member = loadedMember,
                            isDownloading = isDownloading,
                            onDownloadingChange = { isDownloading = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberContent(
    member: FamilyMember,
    isDownloading: Boolean,
    onDownloadingChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Member Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Member Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Member Name:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        member.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Spacer(modifier = Modifier.height(16.dp))

                // Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Category:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        member.category,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Open Button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val encryptedFile = File(member.documentUri)
                            val extension = encryptedFile.extension
                            val decryptedFile = File(context.cacheDir, "sample.$extension")

                            FileSecurity.decryptFile(encryptedFile, decryptedFile)

                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                decryptedFile
                            )

                            val intent = Intent(context, PreviewActivity::class.java)
                            intent.putExtra("uri", uri.toString())
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to open file", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            // Download Button
            Button(
                onClick = {
                    scope.launch {
                        onDownloadingChange(true)
                        try {
                            val encryptedFile = File(member.documentUri)
                            val extension = encryptedFile.extension
                            val decryptedFile = File(context.cacheDir, "sample.$extension")

                            FileSecurity.decryptFile(encryptedFile, decryptedFile)

                            val resolver = context.contentResolver
                            val mimeType = when (extension.lowercase()) {
                                "pdf" -> "application/pdf"
                                "jpg", "jpeg" -> "image/jpeg"
                                "png" -> "image/png"
                                else -> "application/octet-stream"
                            }

                            val contentValues = ContentValues().apply {
                                val cleanName = member.name.replace("\\s+".toRegex(), "_")
                                val cleanCategory = member.category.replace("\\s+".toRegex(), "_")
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val fileName = "${cleanName}_${cleanCategory}_$timestamp.$extension"

                                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/FamilyVault")
                            }

                            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            uri?.let {
                                resolver.openOutputStream(it)?.use { outputStream ->
                                    decryptedFile.inputStream().use { inputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                Toast.makeText(context, "Saved to Downloads/FamilyVault", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                        } finally {
                            onDownloadingChange(false)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                enabled = !isDownloading
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Share Button
        OutlinedButton(
            onClick = {
                scope.launch {
                    try {
                        val encryptedFile = File(member.documentUri)
                        val extension = encryptedFile.extension
                        val decryptedFile = File(context.cacheDir, "sample.$extension")

                        FileSecurity.decryptFile(encryptedFile, decryptedFile)

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            decryptedFile
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = when (extension.lowercase()) {
                                "pdf" -> "application/pdf"
                                "jpg", "jpeg" -> "image/jpeg"
                                "png" -> "image/png"
                                else -> "*/*"
                            }
                            putExtra(Intent.EXTRA_STREAM, uri)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to share file", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}