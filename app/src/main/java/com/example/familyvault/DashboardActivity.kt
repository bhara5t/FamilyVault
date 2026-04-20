package com.example.familyvault

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.familyvault.data.AppDatabase
import com.example.familyvault.data.FamilyMember
import com.example.familyvault.security.FileSecurity
import com.example.familyvault.security.PinScreen
import com.example.familyvault.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                val db = AppDatabase.getDatabase(this)
                var unlocked by remember { mutableStateOf(false) }

                if (unlocked) {
                    DashboardScreen(db)
                } else {
                    PinScreen { unlocked = true }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(db: AppDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    val members by db.familyDao().getAllMembers().collectAsState(initial = emptyList())

    val categories = remember(members) {
        listOf("All") + members.map { it.category }.distinct()
    }

    val filteredMembers = remember(members, search, selectedCategory) {
        members.filter { member ->
            val matchesSearch = search.isEmpty() ||
                    member.name.contains(search, ignoreCase = true) ||
                    member.category.contains(search, ignoreCase = true)

            val matchesCategory = selectedCategory == null ||
                    selectedCategory == "All" ||
                    member.category == selectedCategory

            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Family Vault",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (members.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    exportAllDocuments(context, members)
                                    isExporting = false
                                }
                            },
                            enabled = !isExporting
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Upload,
                                    contentDescription = "Export",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
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
                onClick = {
                    context.startActivity(Intent(context, MainActivity::class.java))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
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

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips
            if (categories.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category ||
                                (category == "All" && selectedCategory == null)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = if (category == "All") null else category
                            },
                            label = { Text(category, fontSize = 15.sp) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Document Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Documents",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (filteredMembers.isNotEmpty()) {
                    Text(
                        "${filteredMembers.size} item${if (filteredMembers.size > 1) "s" else ""}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Documents List
            if (filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (search.isNotEmpty() || selectedCategory != null)
                                Icons.Outlined.SearchOff
                            else
                                Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (search.isNotEmpty() || selectedCategory != null)
                                "No matching documents"
                            else
                                "No documents yet",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (search.isNotEmpty() || selectedCategory != null)
                                "Try a different search"
                            else
                                "Tap + to add your first document",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMembers, key = { it.id }) { member ->
                        DocumentCard(
                            member = member,
                            onClick = {
                                val intent = Intent(context, MemberDetailActivity::class.java)
                                intent.putExtra("memberId", member.id)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    member: FamilyMember,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    member.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    member.category,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// Export Function
private suspend fun exportAllDocuments(context: Context, members: List<FamilyMember>) {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportFileName = "FamilyVault_Backup_$timestamp.zip"

        val metadataJson = JSONArray()
        members.forEach { member ->
            val obj = JSONObject()
            obj.put("name", member.name)
            obj.put("category", member.category)
            obj.put("documentUri", member.documentUri)
            metadataJson.put(obj)
        }

        val exportDir = File(context.cacheDir, "export_$timestamp")
        exportDir.mkdirs()

        val metadataFile = File(exportDir, "metadata.json")
        FileOutputStream(metadataFile).use {
            it.write(metadataJson.toString(2).toByteArray())
        }

        members.forEach { member ->
            val encryptedFile = File(member.documentUri)
            if (encryptedFile.exists()) {
                val extension = encryptedFile.extension
                val decryptedFile = File(exportDir, "${member.id}_${member.name.replace(" ", "_")}.$extension")
                FileSecurity.decryptFile(encryptedFile, decryptedFile)
            }
        }

        val zipFile = File(context.cacheDir, exportFileName)
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            exportDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entry = ZipEntry(file.relativeTo(exportDir).path)
                    zipOut.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, exportFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FamilyVault")
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                zipFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Toast.makeText(
                context,
                "✅ Backup saved to Downloads/FamilyVault",
                Toast.LENGTH_LONG
            ).show()
        }

        exportDir.deleteRecursively()
        zipFile.delete()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(
            context,
            "❌ Export failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}