package com.airoleplay.app.ui.screens.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
    val scrollState = rememberScrollState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        viewModel.updateAvatarUri(uri)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                ),
                title = { Text(if (character.id == 0L) "Create Character" else "Edit Character") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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

            Spacer(modifier = Modifier.height(32.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    viewModel.saveCharacter(context) {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Character", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CreateTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedIndicatorColor = AccentColor,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3
        )
    }
}
