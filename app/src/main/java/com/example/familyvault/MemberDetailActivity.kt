package com.example.familyvault

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.familyvault.data.AppDatabase
import com.example.familyvault.data.FamilyMember
import com.example.familyvault.security.FileSecurity
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
                DetailScreen(db, memberId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(db: AppDatabase, memberId: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val members by db.familyDao().getAllMembers().collectAsState(initial = emptyList())
    val member = members.find { it.id == memberId }

    var isDownloading by remember { mutableStateOf(false) }

    if (member == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Member not found",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp
            )
        }
        return
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                elevation = CardDefaults.cardElevation(8.dp)
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
                                val decryptedFile = File(context.cacheDir, "dec_temp.$extension")

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
                                Toast.makeText(
                                    context,
                                    "Failed to open file",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Open",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Download Button
                Button(
                    onClick = {
                        scope.launch {
                            isDownloading = true
                            try {
                                val encryptedFile = File(member.documentUri)
                                val extension = encryptedFile.extension
                                val decryptedFile = File(context.cacheDir, "dec_temp.$extension")

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
                                    val cleanCategory =
                                        member.category.replace("\\s+".toRegex(), "_")
                                    val timestamp = SimpleDateFormat(
                                        "yyyyMMdd_HHmmss",
                                        Locale.getDefault()
                                    ).format(Date())

                                    val fileName =
                                        "${cleanName}_${cleanCategory}_$timestamp.$extension"

                                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                                    put(
                                        MediaStore.MediaColumns.RELATIVE_PATH,
                                        android.os.Environment.DIRECTORY_DOWNLOADS + "/FamilyVault"
                                    )
                                }

                                val uri = resolver.insert(
                                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                    contentValues
                                )

                                uri?.let {
                                    val outputStream = resolver.openOutputStream(it)
                                    val inputStream = decryptedFile.inputStream()

                                    inputStream.copyTo(outputStream!!)

                                    inputStream.close()
                                    outputStream.close()

                                    Toast.makeText(
                                        context,
                                        "Saved to Downloads",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    "❌ Download failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isDownloading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Download",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
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
                            val decryptedFile = File(context.cacheDir, "dec_temp.$extension")

                            FileSecurity.decryptFile(encryptedFile, decryptedFile)

                            val uri = androidx.core.content.FileProvider.getUriForFile(
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
                                putExtra(Intent.EXTRA_TEXT, "")
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share Document"
                                )
                            )
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Failed to share file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Share",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}