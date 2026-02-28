package com.airoleplay.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.airoleplay.app.domain.prompt.InstructFormat
import com.airoleplay.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsScreen(
    navController: NavController,
    viewModel: ConnectionSettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel<ConnectionSettingsViewModel>()
) {
    val state by viewModel.connectionState.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()

    var showModelDropdown by remember { mutableStateOf(false) }
    var showFormatDropdown by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == 0L) "New Connection" else "Edit Connection", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveConnection { navController.popBackStack() }
                    }) {
                        Text("Save", color = AccentColor, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name Field
            OutlinedTextField(
                value = state.name,
                onValueChange = { newName: String -> viewModel.updateField(name = newName) },
                label = { Text("Connection Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            // Backend Type Segments
            Text("Backend Type", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("OLLAMA", "KOBOLDCPP").forEach { type ->
                    val selected = state.type == type
                    OutlinedButton(
                        onClick = { viewModel.updateField(type = type) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) AccentColor else Color.Transparent,
                            contentColor = if (selected) Color.White else TextPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (selected) AccentColor else TextSecondary)
                        )
                    ) {
                        Text(type)
                    }
                }
            }

            // Base URL Field
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = { newUrl: String -> viewModel.updateField(baseUrl = newUrl) },
                label = { Text("Base URL (e.g. http://192.168.1.5:11434)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            // Test Connection Button
            Button(
                onClick = { viewModel.testConnection() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard, contentColor = AccentColor)
            ) {
                Text("Test Connection", fontWeight = FontWeight.SemiBold)
            }

            // Test Result feedback
            val res = testResult
            if (res != null) {
                Text(
                    text = res as String,
                    color = if ((res as String).startsWith("Success")) SuccessGreen else ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Model Selection (Ollama only, since Kobold returns the single loaded model)
            if (state.type == "OLLAMA" && availableModels.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().clickable { showModelDropdown = true }) {
                    OutlinedTextField(
                        value = state.modelName ?: "Select a Model",
                        onValueChange = { },
                        readOnly = true,
                        enabled = false,
                        label = { Text("Model", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = defaultTextFieldColors(),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary) }
                    )
                    DropdownMenu(
                        expanded = showModelDropdown,
                        onDismissRequest = { showModelDropdown = false },
                        modifier = Modifier.background(SurfaceCard)
                    ) {
                        availableModels.forEach { modelItem: String ->
                            DropdownMenuItem(
                                text = { Text(modelItem, color = TextPrimary) },
                                onClick = {
                                    viewModel.updateField(modelName = modelItem)
                                    showModelDropdown = false
                                }
                            )
                        }
                    }
                }
            } else if (state.type == "KOBOLDCPP" && state.modelName != null) {
                OutlinedTextField(
                    value = state.modelName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Loaded Model") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = defaultTextFieldColors()
                )
            }

            // Instruct Format Dropdown
            Box(modifier = Modifier.fillMaxWidth().clickable { showFormatDropdown = true }) {
                OutlinedTextField(
                    value = state.instructFormat,
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    label = { Text("Instruct Format (Overrides Global Default)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = defaultTextFieldColors(),
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary) }
                )
                DropdownMenu(
                    expanded = showFormatDropdown,
                    onDismissRequest = { showFormatDropdown = false },
                    modifier = Modifier.background(SurfaceCard)
                ) {
                    InstructFormat.values().forEach { formatItem: InstructFormat ->
                        DropdownMenuItem(
                            text = { Text(formatItem.name, color = TextPrimary) },
                            onClick = {
                                viewModel.updateField(instructFormat = formatItem.name)
                                showFormatDropdown = false
                            }
                        )
                    }
                }
            }

            // Active Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set as Active Connection", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = state.isActive,
                    onCheckedChange = { isChecked -> viewModel.updateField(isActive = isChecked) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete Button
            if (state.id != 0L) {
                Button(
                    onClick = {
                        viewModel.deleteConnection { navController.popBackStack() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = ErrorRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ErrorRed))
                ) {
                    Text("Delete Connection")
                }
            }
        }
    }
}

@Composable
fun defaultTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceCard,
    unfocusedContainerColor = SurfaceCard,
    focusedBorderColor = AccentColor,
    unfocusedBorderColor = Color.Transparent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
