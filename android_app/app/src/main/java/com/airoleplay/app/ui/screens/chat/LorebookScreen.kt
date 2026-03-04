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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("PEOPLE", "PLACES", "GENERAL")

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
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                containerColor = DarkBackground,
                contentColor = AccentColor,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[categories.indexOf(selectedCategory)]),
                        color = AccentColor
                    )
                }
            ) {
                categories.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        text = {
                            Text(
                                category.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (selectedCategory == category) AccentColor else TextSecondary
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (entries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No $selectedCategory entries found.", color = TextSecondary)
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
    }

    if (showAddDialog) {
        LorebookEntryDialog(
            initialCategory = selectedCategory,
            onDismiss = { showAddDialog = false },
            onSave = { title, keywords, content, cat, metadata ->
                viewModel.addLorebookEntry(
                    LorebookEntryEntity(
                        characterId = characterId,
                        title = title,
                        keywords = keywords,
                        content = content,
                        category = cat,
                        metadataJson = metadata
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
fun LorebookEntryDialog(
    initialCategory: String = "GENERAL",
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialCategory) }
    
    // Specialized Fields (shared across categories but contextually assigned)
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var appearance by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    
    var placeType by remember { mutableStateOf("") }
    var atmosphere by remember { mutableStateOf("") }
    var notableNpcs by remember { mutableStateOf("") }
    var worldHistory by remember { mutableStateOf("") }

    val categories = listOf("PEOPLE", "PLACES", "GENERAL")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Add Lorebook Entry", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Category Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.lowercase().replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentColor,
                                selectedLabelColor = Color.White,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Category-specific fields
                when (category) {
                    "PEOPLE" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LorebookTextField(age, { age = it }, "Age", Modifier.weight(1f))
                            LorebookTextField(gender, { gender = it }, "Gender", Modifier.weight(1f))
                        }
                        LorebookTextField(occupation, { occupation = it }, "Occupation")
                        LorebookTextField(personality, { personality = it }, "Personality Traits")
                        LorebookTextField(appearance, { appearance = it }, "Appearance Details")
                        LorebookTextField(relationship, { relationship = it }, "Relationship to Character")
                    }
                    "PLACES" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LorebookTextField(placeType, { placeType = it }, "Type (e.g. City, Tavern)", Modifier.weight(1f))
                            LorebookTextField(atmosphere, { atmosphere = it }, "Atmosphere", Modifier.weight(1f))
                        }
                        LorebookTextField(notableNpcs, { notableNpcs = it }, "Notable NPCs")
                        LorebookTextField(worldHistory, { worldHistory = it }, "History/Lore", minHeight = 100.dp)
                    }
                    "GENERAL" -> {
                         // General just uses the main content box mostly, but we could add a "Significance" field
                        LorebookTextField(worldHistory, { worldHistory = it }, "Significance/Notes", minHeight = 100.dp)
                    }
                }

                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Description / Summary") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
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
                onClick = { 
                    if (title.isNotBlank() && content.isNotBlank()) {
                        val metadata = when (category) {
                            "PEOPLE" -> """{"age":"$age","gender":"$gender","personality":"$personality","appearance":"$appearance","occupation":"$occupation","relationship":"$relationship"}"""
                            "PLACES" -> """{"type":"$placeType","atmosphere":"$atmosphere","npcs":"$notableNpcs","history":"$worldHistory"}"""
                            else -> if (worldHistory.isNotBlank()) """{"notes":"$worldHistory"}""" else null
                        }
                        onSave(title, keywords, content, category, metadata)
                    }
                },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LorebookTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = Dp.Unspecified
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier.fillMaxWidth().let { if (minHeight != Dp.Unspecified) it.height(minHeight) else it },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = DarkBackground,
            unfocusedContainerColor = DarkBackground,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}
