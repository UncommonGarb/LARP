package com.airoleplay.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airoleplay.app.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalGenerationSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val globalSettings by viewModel.globalSettings.collectAsState()
    val settings = globalSettings

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Generation Defaults", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "These defaults apply to all connections unless overridden per-connection.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Text("Global System Prompt", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            }
            item {
                OutlinedTextField(
                    value = settings?.globalSystemPrompt ?: "",
                    onValueChange = { viewModel.updateGlobalSystemPrompt(it) },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentColor,
                        unfocusedBorderColor = SurfaceCard,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard
                    ),
                    placeholder = { Text("You are {{char}}...", color = TextSecondary) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text("Generation Parameters", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            }

            // Temperature Slider
            item {
                GenSlider(
                    label = "Temperature",
                    value = settings?.defaultTemperature ?: 0.8f,
                    range = 0f..2f,
                    steps = 39,
                    format = "%.2f",
                    onValueChange = { viewModel.updateGlobalGenSettings(temperature = it) }
                )
            }

            // Top-P Slider
            item {
                GenSlider(
                    label = "Top-P",
                    value = settings?.defaultTopP ?: 0.9f,
                    range = 0f..1f,
                    steps = 19,
                    format = "%.2f",
                    onValueChange = { viewModel.updateGlobalGenSettings(topP = it) }
                )
            }

            // Repetition Penalty Slider
            item {
                GenSlider(
                    label = "Repetition Penalty",
                    value = settings?.defaultRepetitionPenalty ?: 1.1f,
                    range = 1f..2f,
                    steps = 19,
                    format = "%.2f",
                    onValueChange = { viewModel.updateGlobalGenSettings(repetitionPenalty = it) }
                )
            }

            // Max New Tokens
            item {
                GenNumberField(
                    label = "Max New Tokens",
                    value = settings?.defaultMaxNewTokens ?: 400,
                    onValueChange = { viewModel.updateGlobalGenSettings(maxNewTokens = it) }
                )
            }

            // Context Size Limit
            item {
                GenNumberField(
                    label = "Context Size Limit",
                    value = settings?.defaultContextSizeLimit ?: 4096,
                    onValueChange = { viewModel.updateGlobalGenSettings(contextSizeLimit = it) }
                )
            }

            // Top-K
            item {
                GenNumberField(
                    label = "Top-K",
                    value = settings?.defaultTopK ?: 40,
                    onValueChange = { viewModel.updateGlobalGenSettings(topK = it) }
                )
            }
        }
    }
}

@Composable
fun GenSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    format: String = "%.2f",
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Text(
                String.format(format, value),
                color = AccentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = { onValueChange(it) },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = AccentColor,
                activeTrackColor = AccentColor,
                inactiveTrackColor = SurfaceCard
            )
        )
    }
}

@Composable
fun GenNumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText
                newText.toIntOrNull()?.let { onValueChange(it) }
            },
            modifier = Modifier.width(120.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AccentColor,
                unfocusedBorderColor = SurfaceCard,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard
            )
        )
    }
}
