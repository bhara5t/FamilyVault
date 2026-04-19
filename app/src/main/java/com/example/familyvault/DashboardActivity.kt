package com.example.familyvault

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {
            val db = AppDatabase.getDatabase(this)
            var unlocked by remember { mutableStateOf(false) }

            if (unlocked) {
                DashboardScreen(db)
            } else {
                com.example.familyvault.security.PinScreen {
                    unlocked = true
                }
            }

        }
    }
}

@Composable
fun DashboardScreen(db: AppDatabase) {

    val context = LocalContext.current
    var search by remember { mutableStateOf("") }

    val members by db.familyDao().getAllMembers().collectAsState(initial = emptyList())

    val filtered = members.filter {
        it.name.contains(search, ignoreCase = true) ||
                it.category.contains(search, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                context.startActivity(Intent(context, MainActivity::class.java))
            }) {
                Text("+")
            }
        }
    ) { padding ->

        Column(modifier = Modifier.padding(16.dp)) {

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search by name or category") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(filtered) { member ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        onClick = {
                            val intent = Intent(context, MemberDetailActivity::class.java)
                            intent.putExtra("memberId", member.id)
                            context.startActivity(intent)
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${member.name} ")
                            Text("Category: ${member.category}")
                        }
                    }
                }
            }
        }
    }
}