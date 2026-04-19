package com.example.familyvault

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.familyvault.data.AppDatabase
import com.example.familyvault.data.FamilyMember
import com.example.familyvault.security.FileSecurity
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(this)
        setContent {
                FamilyScreen(db)
        }
    }
}

@Composable
fun FamilyScreen(db: AppDatabase) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var category by remember { mutableStateOf("Aadhaar") }

    val members by db.familyDao().getAllMembers().collectAsState(initial = emptyList())

    // 📁 File picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)
            selectedUri = it
        }
    }

    val categories = listOf("Aadhaar", "PAN", "Certificates", "Medical", "Insurance")
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {

        Text("Family Vault", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))


        Spacer(modifier = Modifier.height(8.dp))

        // 🔽 CATEGORY DROPDOWN
        Box {
            Button(onClick = { expanded = true }) {
                Text("Category: $category")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            category = it
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            launcher.launch(arrayOf("*/*"))
        }) {
            Text("Pick Document")
        }

        selectedUri?.let {
            Text("Selected: ${getFileName(context, it)}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔒 ADD MEMBER WITH CATEGORY + ENCRYPTION
        Button(
            onClick = {
                if (name.isNotBlank()  && selectedUri != null) {
                    scope.launch {

                        val fileName = getFileName(context, selectedUri!!)
                        val extension = fileName.substringAfterLast('.', "")

                        val inputStream =
                            context.contentResolver.openInputStream(selectedUri!!)

                        val tempFile = File(context.cacheDir, "temp_file.$extension")
                        val encryptedFile = File(
                            context.filesDir,
                            "enc_${System.currentTimeMillis()}.$extension"
                        )

                        inputStream?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        FileSecurity.encryptFile(tempFile, encryptedFile)

                        val exists = members.any {
                            it.name.equals(name, ignoreCase = true) &&
                                    it.category.equals(category, ignoreCase = true)
                        }

                        if (exists) {
                            android.widget.Toast
                                .makeText(context, "This document already exists for this person", android.widget.Toast.LENGTH_SHORT)
                                .show()
                        } else {

                            db.familyDao().insertMember(
                                FamilyMember(
                                    name = name,
                                    category = category,
                                    documentUri = encryptedFile.absolutePath
                                )
                            )

                            // ✅ Clear only when success
                            name = ""
                            selectedUri = null
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Member")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(members) { member ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {

                        Text("${member.name} )")

                        Spacer(modifier = Modifier.height(4.dp))

                        // ✅ SHOW CATEGORY
                        Text(
                            text = "Category: ${member.category}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Encrypted File",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            // 🔓 OPEN
                            Button(onClick = {
                                try {
                                    val encryptedFile = File(member.documentUri)

                                    val extension = encryptedFile.extension
                                    val decryptedFile =
                                        File(context.cacheDir, "dec_temp.$extension")

                                    FileSecurity.decryptFile(
                                        encryptedFile,
                                        decryptedFile
                                    )

                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        decryptedFile
                                    )

                                    val intent =
                                        Intent(context, PreviewActivity::class.java)
                                    intent.putExtra("uri", uri.toString())
                                    context.startActivity(intent)

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }) {
                                Text("Open")
                            }

                            Button(onClick = {
                                scope.launch {
                                    db.familyDao().deleteMember(member)
                                }
                            }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 📄 Get file name
fun getFileName(context: android.content.Context, uri: Uri): String {
    var name = "Unknown File"

    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && index != -1) {
            name = it.getString(index)
        }
    }

    return name
}