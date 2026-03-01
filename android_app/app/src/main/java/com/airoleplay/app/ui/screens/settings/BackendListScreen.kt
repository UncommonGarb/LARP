package com.airoleplay.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airoleplay.app.data.local.entity.BackendConnectionEntity
import com.airoleplay.app.ui.navigation.Screen
import com.airoleplay.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackendListScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val connections by viewModel.backendConnections.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("AI Backends", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.ConnectionSettings.createRoute(-1L)) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Connection", tint = AccentColor)
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(connections) { conn ->
                ConnectionRow(
                    connection = conn,
                    onSelect = { viewModel.setActiveConnection(conn.id) },
                    onEdit = { navController.navigate(Screen.ConnectionSettings.createRoute(conn.id)) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(Screen.ConnectionSettings.createRoute(-1L)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AccentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Connection", color = AccentColor)
                }
            }
        }
    }
}
