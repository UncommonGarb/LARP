package com.airoleplay.app.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airoleplay.app.ui.navigation.Screen
import com.airoleplay.app.ui.theme.AccentColor
import com.airoleplay.app.ui.theme.DarkBackground
import com.airoleplay.app.ui.theme.SurfaceCard
import com.airoleplay.app.ui.theme.TextPrimary
import com.airoleplay.app.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val isOnboarded by viewModel.isOnboarded.collectAsState()

    LaunchedEffect(isOnboarded) {
        if (isOnboarded) {
            navController.navigate(Screen.Characters.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    var step by remember { mutableStateOf(1) }
    var baseUrl by remember { mutableStateOf("http://192.168.1.100:11434") }
    var type by remember { mutableStateOf("OLLAMA") }

    Surface(
        color = DarkBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                1 -> {
                    Text(
                        text = "Welcome to AI Roleplay",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your private hub for character roleplay powered by local LLMs.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { step = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                    ) {
                        Text("Get Started")
                    }
                }
                2 -> {
                    Text(
                        text = "Connect a Backend",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "To start chatting, you need a local LLM server running on your network (like Ollama or KoboldCPP).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("OLLAMA", "KOBOLDCPP").forEach { t ->
                            val selected = type == t
                            OutlinedButton(
                                onClick = { type = t },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) AccentColor else Color.Transparent,
                                    contentColor = if (selected) Color.White else TextPrimary
                                )
                            ) {
                                Text(t)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = AccentColor,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            viewModel.saveInitialConnection(baseUrl, type)
                            navController.navigate(Screen.Characters.route) {
                                popUpTo(0) { inclusive = true } // Clear stack
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                    ) {
                        Text("Connect & Finish")
                    }
                    TextButton(onClick = {
                        navController.navigate(Screen.Characters.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Text("Skip for now", color = TextSecondary)
                    }
                }
            }
        }
    }
}
