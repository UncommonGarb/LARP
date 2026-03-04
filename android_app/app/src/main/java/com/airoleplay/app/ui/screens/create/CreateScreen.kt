package com.airoleplay.app.ui.screens.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
    val hasChanges by viewModel.hasChanges.collectAsState()
    val scrollState = rememberScrollState()
    var showDiscardDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        viewModel.updateAvatarUri(uri)
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    Scaffold(
        containerColor = DarkBackground,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkBackground,
                tonalElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        viewModel.saveCharacter(context) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Character", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                ),
                title = { Text(if (character.id == 0L) "Create Character" else "Edit Character") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) showDiscardDialog = true else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveCharacter(context) {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Done, contentDescription = "Save", tint = AccentColor)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar picker
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .clickable { imagePickerLauncher.launch("image/*") }
            ) {
                val displayUri = avatarUri ?: character.avatarImagePath
                if (displayUri != null) {
                    AsyncImage(
                        model = displayUri,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(AccentColor, CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CreateTextField(
                label = "Name",
                value = character.name,
                onValueChange = { newVal -> viewModel.updateCharacter { it.copy(name = newVal) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            CreateTextField(
                label = "Description / Physical Appearance",
                value = character.description,
                onValueChange = { newVal -> viewModel.updateCharacter { it.copy(description = newVal) } },
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            CreateTextField(
                label = "Personality Summary",
                value = character.personalitySummary,
                onValueChange = { newVal -> viewModel.updateCharacter { it.copy(personalitySummary = newVal) } },
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            CreateTextField(
                label = "Scenario",
                value = character.scenario,
                onValueChange = { newVal -> viewModel.updateCharacter { it.copy(scenario = newVal) } },
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            CreateTextField(
                label = "First Message",
                value = character.firstMessage,
                onValueChange = { newVal -> viewModel.updateCharacter { it.copy(firstMessage = newVal) } },
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            CreateTextField(
                label = "Example Dialogue",
                value = character.exampleDialogue,
                onValueChange = { newVal -> viewModel.updateCharacter { it.copy(exampleDialogue = newVal) } },
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = SurfaceCard, modifier = Modifier.padding(vertical = 16.dp))

            Text("Advanced Settings", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))

            Spacer(modifier = Modifier.height(16.dp))

            CreateTextField(
                label = "System Prompt Override",
                value = character.systemPromptOverride ?: "",
                onValueChange = { newVal -> viewModel.updateCharacter { it.copy(systemPromptOverride = newVal) } },
                singleLine = false,
                placeholder = "Leave blank to use global default..."
            )

            Spacer(modifier = Modifier.height(16.dp))

            CreateTextField(
                label = "Post-History Instructions",
                value = character.postHistoryInstructions ?: "",
                onValueChange = { newVal -> viewModel.updateCharacter { it.copy(postHistoryInstructions = newVal) } },
                singleLine = false,
                placeholder = "Always stay in character..."
            )

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = SurfaceCard, modifier = Modifier.padding(vertical = 8.dp))

            // Burn Pacing
            Text("Pacing / Burn Rate", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text("Controls how quickly relationships and plots develop.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
            
            val pacingOptions = listOf("SLOW", "REALISTIC", "FAST", "INSTANT")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                pacingOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = pacingOptions.size),
                        onClick = { viewModel.updateCharacter { it.copy(burnPacing = label) } },
                        selected = character.burnPacing == label,
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = AccentColor,
                            activeContentColor = Color.White,
                            inactiveContainerColor = SurfaceCard,
                            inactiveContentColor = TextSecondary
                        )
                    ) {
                        Text(label.lowercase().capitalize(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Autonomy / Agency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Agency / Autonomy", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    val agencyText = when {
                        character.autonomyLevel < 30 -> "User-Led (Compliant)"
                        character.autonomyLevel < 70 -> "Balanced (Cooperative)"
                        else -> "Independent (Has own agenda)"
                    }
                    Text(agencyText, color = AccentColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Text("${character.autonomyLevel}%", color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            Text("Decide if the character follows your lead or has their own secret wants and desires.", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            
            Slider(
                value = character.autonomyLevel.toFloat(),
                onValueChange = { newValue -> viewModel.updateCharacter { it.copy(autonomyLevel = newValue.toInt()) } },
                valueRange = 0f..100f,
                steps = 10,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = AccentColor,
                    activeTrackColor = AccentColor,
                    inactiveTrackColor = SurfaceCard
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    navController.popBackStack()
                }) {
                    Text("Discard", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
fun CreateTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    placeholder: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = value,
            placeholder = { Text(placeholder, color = TextSecondary) },
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedIndicatorColor = AccentColor,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AccentColor
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3
        )
    }
}
