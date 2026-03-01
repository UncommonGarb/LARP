package com.airoleplay.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import com.airoleplay.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LorebookScreen(
    navController: NavController,
    characterId: Long,
    viewModel: LorebookViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Lorebook", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Entry", tint = AccentColor)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (entries.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No lorebook entries for this character.", color = TextSecondary)
                    }
                }
            }

            items(entries) { entry ->
                LorebookEntryCard(
                    entry = entry,
                    onDelete = { viewModel.deleteLorebookEntry(entry) }
                )
            }
        }
    }

    if (showAddDialog) {
        LorebookEntryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, keywords, content ->
                viewModel.addLorebookEntry(
                    LorebookEntryEntity(
                        characterId = characterId,
                        title = title,
                        keywords = keywords,
                        content = content
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LorebookEntryCard(entry: LorebookEntryEntity, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, color = AccentColor, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Keywords: ${entry.keywords}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(entry.content, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LorebookEntryDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Add Lorebook Entry", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                TextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords (comma separated)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.height(150.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank() && content.isNotBlank()) onSave(title, keywords, content) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Save", color = AccentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
