package com.airoleplay.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
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
import com.airoleplay.app.data.local.entity.BackendConnectionEntity
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import com.airoleplay.app.ui.navigation.Screen
import com.airoleplay.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val activePersona by viewModel.activePersona.collectAsState()
    val connections by viewModel.backendConnections.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp + contentPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // User Persona Section
            item {
                Column {
                    PersonaSettingsCard(persona = activePersona) {
                        navController.navigate(Screen.PersonaSettings.route)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Manage Personas",
                        color = AccentColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { navController.navigate(Screen.PersonaSettings.route) }
                    )
                }
            }

            // Backend Connections Section
            item {
                Column {
                    Text(
                        text = "AI BACKENDS",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    connections.forEach { conn ->
                        ConnectionRow(
                            connection = conn,
                            onSelect = { viewModel.setActiveConnection(conn.id) },
                            onEdit = { navController.navigate(Screen.ConnectionSettings.createRoute(conn.id)) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.ConnectionSettings.createRoute(-1L)) } // New connection
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = AccentColor)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Add Connection", color = AccentColor, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Temporary Test Result display for convenience in development
            if (testResult != null) {
                item {
                    Text(
                        text = testResult!!,
                        color = if (testResult!!.startsWith("Success")) SuccessGreen else ErrorRed,
                        modifier = Modifier.padding(vertical = 8.dp).clickable { viewModel.clearTestResult() }
                    )
                }
            }

            // Global Defaults
            item {
                Column {
                    Text(
                        text = "GENERATION DEFAULTS",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SettingsSliderRow("Temperature", 0.8f)
                    SettingsSliderRow("Top-P", 0.9f)
                    SettingsSliderRow("Repetition Penalty", 1.1f)
                    SettingsNumberRow("Max New Tokens", "400")
                    SettingsNumberRow("Context Limit", "4096")
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "build:a0.01",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSliderRow(label: String, value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
        Text(value.toString(), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SettingsNumberRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
        Text(value, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PersonaSettingsCard(persona: UserPersonaEntity?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkBackground)
            ) {
                if (persona?.avatarImagePath != null) {
                    AsyncImage(
                        model = persona.avatarImagePath,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = persona?.name ?: "No Persona",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = persona?.description?.take(40) ?: "Tap to set up your persona.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            TextButton(onClick = onClick) {
                Text(if (persona == null) "Setup" else "Edit", color = AccentColor)
            }
        }
    }
}

@Composable
fun ConnectionRow(
    connection: BackendConnectionEntity,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Active dot
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(if (connection.isActive) SuccessGreen else TextSecondary.copy(alpha = 0.3f))
                .clickable { onSelect() }
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = connection.name,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${connection.type} \u00B7 ${connection.baseUrl}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Edit", tint = TextSecondary)
    }
}
