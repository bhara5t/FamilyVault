package com.example.familyvault

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.familyvault.data.AppDatabase
import com.example.familyvault.data.FamilyMember
import com.example.familyvault.security.FileSecurity
import com.example.familyvault.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                FamilyScreen(db)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(db: AppDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var category by remember { mutableStateOf("Aadhaar") }
    var showAddDialog by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val members by db.familyDao().getAllMembers().collectAsState(initial = emptyList())

    val filteredMembers = members.filter {
        val q = search.lowercase()
        it.name.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)
            selectedUri = it
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                isImporting = true
                importFromZip(context, db, it) { success, message ->
                    isImporting = false
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val categories = listOf("Aadhaar", "PAN", "Certificates", "Medical", "Insurance")
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Family Vault",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Import Button
                    IconButton(
                        onClick = { importLauncher.launch(arrayOf("application/zip")) },
                        enabled = !isImporting
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Download,
                                contentDescription = "Import",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search documents...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document Count
            if (filteredMembers.isNotEmpty()) {
                Text(
                    "${filteredMembers.size} Document${if (filteredMembers.size > 1) "s" else ""}",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Document List
            if (filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (search.isNotEmpty()) Icons.Outlined.SearchOff else Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (search.isNotEmpty()) "No matching documents" else "No documents yet",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (search.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap + to add or ↓ to import",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredMembers, key = { it.id }) { member ->
                        DocumentCard(
                            member = member,
                            onOpen = {
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
                            },
                            onDelete = {
                                scope.launch {
                                    db.familyDao().deleteMember(member)
                                    File(member.documentUri).delete()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Document Dialog
    if (showAddDialog) {
        AddDocumentDialog(
            name = name,
            onNameChange = { name = it },
            category = category,
            categories = categories,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            onCategorySelected = { category = it },
            selectedUri = selectedUri,
            onPickDocument = { documentLauncher.launch(arrayOf("*/*")) },
            onDismiss = {
                showAddDialog = false
                name = ""
                selectedUri = null
            },
            onConfirm = {
                if (name.trim().isNotBlank() && selectedUri != null) {
                    scope.launch {
                        val fileName = getFileName(context, selectedUri!!)
                        val extension = fileName.substringAfterLast('.', "")

                        val inputStream = context.contentResolver.openInputStream(selectedUri!!)
                        val tempFile = File(context.cacheDir, "sample.$extension")
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
                        tempFile.delete()

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
                            Toast.makeText(context, "Document added", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Document already exists", Toast.LENGTH_SHORT).show()
                        }

                        showAddDialog = false
                        name = ""
                        selectedUri = null
                    }
                }
            }
        )
    }
}

// Import function
private suspend fun importFromZip(
    context: android.content.Context,
    db: AppDatabase,
    zipUri: Uri,
    onComplete: (Boolean, String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val importDir = File(context.cacheDir, "import_${System.currentTimeMillis()}")
            importDir.mkdirs()

            // Extract ZIP
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        val file = File(importDir, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { outputStream ->
                                zipStream.copyTo(outputStream)
                            }
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }

            // Read metadata
            val metadataFile = File(importDir, "metadata.json")
            if (!metadataFile.exists()) {
                importDir.deleteRecursively()
                withContext(Dispatchers.Main) {
                    onComplete(false, " Invalid backup file")
                }
                return@withContext
            }

            val metadataJson = metadataFile.readText()
            val jsonArray = JSONArray(metadataJson)

            var importedCount = 0
            val existingMembers = db.familyDao().getAllMembersSync()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.getString("name")
                val category = obj.getString("category")

                // Check if already exists
                val exists = existingMembers.any {
                    it.name.equals(name, true) && it.category.equals(category, true)
                }

                if (!exists) {
                    // Find the document file
                    val documentFile = importDir.listFiles()?.find {
                        it.name.startsWith("${obj.optInt("id", 0)}_") ||
                                it.name.contains(name.replace(" ", "_"))
                    }

                    if (documentFile != null && documentFile.exists()) {
                        val extension = documentFile.extension
                        val encryptedFile = File(
                            context.filesDir,
                            "enc_${System.currentTimeMillis()}_$importedCount.$extension"
                        )

                        // Encrypt and save
                        FileSecurity.encryptFile(documentFile, encryptedFile)

                        db.familyDao().insertMember(
                            FamilyMember(
                                name = name,
                                category = category,
                                documentUri = encryptedFile.absolutePath
                            )
                        )
                        importedCount++
                    }
                }
            }

            // Cleanup
            importDir.deleteRecursively()

            withContext(Dispatchers.Main) {
                if (importedCount > 0) {
                    onComplete(true, "Imported $importedCount documents")
                } else {
                    onComplete(true, "No new documents to import")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onComplete(false, " Import failed: ${e.message}")
            }
        }
    }
}

@Composable
fun DocumentCard(
    member: FamilyMember,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    member.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    member.category,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onOpen) {
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFF44336)
                )
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Document") },
                text = { Text("Delete '${member.name}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete()
                            showDeleteConfirm = false
                        }
                    ) {
                        Text("Delete", color = Color(0xFFF44336))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentDialog(
    name: String,
    onNameChange: (String) -> Unit,
    category: String,
    categories: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCategorySelected: (String) -> Unit,
    selectedUri: Uri?,
    onPickDocument: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Document",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = onExpandedChange
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandedChange(false) }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    onCategorySelected(cat)
                                    onExpandedChange(false)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onPickDocument,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedUri == null) "Pick Document" else "Change Document")
                }

                selectedUri?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Selected: ${getFileName(LocalContext.current, it)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = name.isNotBlank() && selectedUri != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

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