package com.airoleplay.app.ui.screens.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airoleplay.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    navController: NavController,
    viewModel: CreateViewModel = hiltViewModel()
) {
    val character by viewModel.characterState.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var showAdvanced by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateAvatarUri(uri)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Create Character", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveCharacter { savedId ->
                            navController.popBackStack() // Or navigate to detail
                        }
                    }) {
                        Text("Save", color = AccentColor, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.saveCharacter {
                        navController.popBackStack()
                    }
                },
                containerColor = AccentColor,
                contentColor = Color.White
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, "Save")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Selector
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .border(2.dp, AccentColor, CircleShape)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = "Add Avatar", tint = TextSecondary, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields
            FormSection(
                label = "Name",
                value = character.name,
                onValueChange = { viewModel.updateField(name = it) },
                singleLine = true
            )

            FormSection(
                label = "Description",
                subtitle = "Who are they? Background, appearance, how they speak and think.",
                value = character.description,
                onValueChange = { viewModel.updateField(description = it) },
                singleLine = false,
                minLines = 4
            )

            FormSection(
                label = "Personality Summary",
                subtitle = "A short summary of key traits. e.g. 'Sarcastic, clever, secretly kind-hearted.'",
                value = character.personalitySummary,
                onValueChange = { viewModel.updateField(personalitySummary = it) },
                singleLine = false,
                minLines = 2
            )

            FormSection(
                label = "Scenario",
                subtitle = "The setting or context. Where are you meeting?",
                value = character.scenario,
                onValueChange = { viewModel.updateField(scenario = it) },
                singleLine = false,
                minLines = 3
            )

            FormSection(
                label = "First Message",
                subtitle = "What does your character say when the chat begins?",
                value = character.firstMessage,
                onValueChange = { viewModel.updateField(firstMessage = it) },
                singleLine = false,
                minLines = 3
            )

            FormSection(
                label = "Example Dialogues",
                subtitle = "Write a few back-and-forth exchanges.",
                value = character.exampleDialogue,
                onValueChange = { viewModel.updateField(exampleDialogue = it) },
                singleLine = false,
                minLines = 4
            )

            // Advanced Section Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Advanced Settings", color = AccentColor, fontWeight = FontWeight.Bold)
                // Icon for expand/collapse would go here
            }

            if (showAdvanced) {
                FormSection(
                    label = "Tags",
                    subtitle = "Comma separated tags (e.g. fantasy, shy, female)",
                    value = character.tags,
                    onValueChange = { viewModel.updateField(tags = it) },
                    singleLine = true
                )

                FormSection(
                    label = "Custom System Prompt",
                    subtitle = "Replaces global system prompt.",
                    value = character.systemPromptOverride ?: "",
                    onValueChange = { viewModel.updateField(systemPromptOverride = it) },
                    singleLine = false,
                    minLines = 3
                )

                FormSection(
                    label = "Post-History Instructions",
                    subtitle = "Injected at the very end of every prompt.",
                    value = character.postHistoryInstructions ?: "",
                    onValueChange = { viewModel.updateField(postHistoryInstructions = it) },
                    singleLine = false,
                    minLines = 3
                )
            }

            Spacer(modifier = Modifier.height(100.dp)) // Fab padding
        }
    }
}

@Composable
fun FormSection(
    label: String,
    subtitle: String? = null,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = AccentColor)
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedBorderColor = AccentColor,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
