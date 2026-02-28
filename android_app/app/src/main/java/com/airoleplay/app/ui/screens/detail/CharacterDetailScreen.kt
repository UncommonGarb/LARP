package com.airoleplay.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airoleplay.app.ui.navigation.Screen
import com.airoleplay.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    navController: NavController,
    characterId: Long,
    viewModel: CharacterDetailViewModel = hiltViewModel()
) {
    val character by viewModel.character.collectAsState()
    val pastSessions by viewModel.pastSessions.collectAsState()
    val exportUri by viewModel.exportUri.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(exportUri) {
        exportUri?.let { uri ->
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                type = "image/png"
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Export Character Card"))
            viewModel.clearExportUri()
        }
    }

    if (character == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentColor)
        }
        return
    }

    val char = character!!

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                     IconButton(onClick = { viewModel.exportCharacter() }) {
                         Icon(androidx.compose.material.icons.Icons.Default.Share, contentDescription = "Export", tint = Color.White)
                     }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // Hero Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    AsyncImage(
                        model = char.avatarImagePath,
                        contentDescription = char.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, DarkBackground),
                                    startY = 300f
                                )
                            )
                    )
                }
            }

            item {
                // Character Info
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = char.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    // Tags
                    if (char.tags.isNotBlank()) {
                        Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            char.tags.split(",").map { it.trim() }.forEach { tag ->
                                if (tag.isNotBlank()) {
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(tag, color = AccentColor) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.Transparent),
                                        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true).copy() // Border is internal type unfortunately, so just keep simple
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = char.description.take(150) + if (char.description.length > 150) "..." else "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Navigate to Edit screen */ },
                            modifier = Modifier.weight(1f),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(AccentColor, AccentColor))),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentColor)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit")
                        }

                        Button(
                            onClick = {
                                viewModel.startNewChatSession("Chat") { newSessionId ->
                                    navController.navigate(Screen.ChatSession.createRoute(newSessionId))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Chat")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Past Chats section
                    Text(
                        text = "Past Chats",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (pastSessions.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("No previous conversations.", color = TextSecondary)
                            }
                        }
                    }
                }
            }

            items(pastSessions) { session ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            navController.navigate(Screen.ChatSession.createRoute(session.id))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = session.title.takeIf { it.isNotBlank() } ?: "Chat Session",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
