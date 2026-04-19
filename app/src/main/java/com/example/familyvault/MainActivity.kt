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
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var search by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var category by remember { mutableStateOf("Aadhaar") }

    val members by db.familyDao().getAllMembers().collectAsState(initial = emptyList())

    val filteredMembers = members.filter {
        val q = search.lowercase()
        it.name.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
    }

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

    // ✅ SCAFFOLD FIX
    Scaffold { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding) // ✅ prevents overlap
                .padding(16.dp)
                .fillMaxSize()
        ) {

            Spacer(modifier = Modifier.height(8.dp)) // extra safe spacing

            // 🌿 TITLE
            Text(
                "Family Vault",
                fontSize = 26.sp,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🟢 SECTION 1 → ADD DOCUMENT
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text("Add Document", fontSize = 20.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Enter Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { launcher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pick Document")
                    }

                    selectedUri?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Selected: ${getFileName(context, it)}")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (name.trim().isNotBlank() && selectedUri != null) {
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
                                        it.name.equals(name, true) &&
                                                it.category.equals(category, true)
                                    }

                                    if (!exists) {
                                        db.familyDao().insertMember(
                                            FamilyMember(
                                                name = name,
                                                category = category,
                                                documentUri = encryptedFile.absolutePath
                                            )
                                        )

                                        name = ""
                                        selectedUri = null
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Document")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔍 SEARCH
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                label = { Text("Search by name or category") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 📋 LIST
            LazyColumn {
                items(filteredMembers) { member ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            Text(member.name, fontSize = 20.sp)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Category: ${member.category}")

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Button(
                                    onClick = {
                                        val encryptedFile = File(member.documentUri)
                                        val extension = encryptedFile.extension
                                        val decryptedFile =
                                            File(context.cacheDir, "dec_temp.$extension")

                                        FileSecurity.decryptFile(encryptedFile, decryptedFile)

                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            decryptedFile
                                        )

                                        val intent =
                                            Intent(context, PreviewActivity::class.java)
                                        intent.putExtra("uri", uri.toString())
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open")
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            db.familyDao().deleteMember(member)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete")
                                }
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