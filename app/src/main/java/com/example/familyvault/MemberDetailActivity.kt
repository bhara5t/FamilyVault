package com.example.familyvault

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.familyvault.data.AppDatabase
import com.example.familyvault.security.FileSecurity
import java.io.File

class MemberDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val memberId = intent.getIntExtra("memberId", -1)
        val db = AppDatabase.getDatabase(this)

        setContent {
            DetailScreen(db, memberId)
        }
    }
}

@Composable
fun DetailScreen(db: AppDatabase, memberId: Int) {

    val context = LocalContext.current
    val members by db.familyDao().getAllMembers().collectAsState(initial = emptyList())

    val member = members.find { it.id == memberId }

    if (member == null) {
        Text("Member not found")
        return
    }

    Column(modifier = Modifier.padding(16.dp)) {

        Text(
            "${member.name} ",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Category: ${member.category}")

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            // 🔓 OPEN BUTTON
            Button(
                onClick = {

                    val encryptedFile = File(member.documentUri)
                    val extension = encryptedFile.extension
                    val decryptedFile = File(context.cacheDir, "dec_temp.$extension")

                    FileSecurity.decryptFile(encryptedFile, decryptedFile)

                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        decryptedFile
                    )

                    val intent = Intent(context, PreviewActivity::class.java)
                    intent.putExtra("uri", uri.toString())
                    context.startActivity(intent)

                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Open")
            }

            // ⬇️ DOWNLOAD BUTTON (FIXED)
            Button(
                onClick = {

                    try {
                        val encryptedFile = File(member.documentUri)
                        val extension = encryptedFile.extension
                        val decryptedFile = File(context.cacheDir, "dec_temp.$extension")

                        // 🔓 Decrypt first
                        FileSecurity.decryptFile(encryptedFile, decryptedFile)

                        val resolver = context.contentResolver

                        val mimeType = if (extension == "pdf") {
                            "application/pdf"
                        } else {
                            "image/*"
                        }

                        val contentValues = ContentValues().apply {
                            val cleanName = member.name.replace("\\s+".toRegex(), "_")
                            val cleanCategory = member.category.replace("\\s+".toRegex(), "_")

                            val fileName = "${cleanName}_${cleanCategory}_${(1000..9999).random()}.$extension"

                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
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
                            "Download failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Download")
            }
        }
    }
}