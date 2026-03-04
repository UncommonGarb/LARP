package com.airoleplay.app.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
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
import com.airoleplay.app.ui.components.CharacterAvatar
import com.airoleplay.app.ui.navigation.Screen
import com.airoleplay.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: ChatsViewModel = hiltViewModel()
) {
    val chatHistory by viewModel.chatHistory.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Chats", style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
                actions = {
                    IconButton(onClick = { /* Search logic */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        if (chatHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No conversations yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Find a character and start chatting!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Characters.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                    ) {
                        Text("Explore Characters")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()) // Use passed padding
            ) {
                items(chatHistory) { item ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    // Swipe-to-dismiss wrap could be implemented here; for simplicity, a long press or explicit button
                    ChatHistoryRow(
                        item = item,
                        onClick = { navController.navigate(Screen.ChatSession.createRoute(item.session.id)) },
                        onDeleteClick = { showDeleteDialog = true }
                    )

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            containerColor = SurfaceCard,
                            title = { Text("Delete Chat?", color = TextPrimary) },
                            text = { Text("Are you sure you want to delete this conversation with ${item.character.name}? This cannot be undone.", color = TextSecondary) },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteSession(item.session)
                                    showDeleteDialog = false
                                }) {
                                    Text("Delete", color = ErrorRed)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel", color = TextPrimary)
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = SurfaceCard, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun ChatHistoryRow(
    item: ChatHistoryItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CharacterAvatar(
            name = item.character.name,
            avatarPath = item.character.avatarImagePath,
            size = 52.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.character.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                // Simple timestamp logic - normally would format properly
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Text(
                text = item.session.title.takeIf { it.isNotBlank() } ?: "Chat ${item.session.id}",
                style = MaterialTheme.typography.labelMedium,
                color = AccentColor,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            val previewPrefix = if (item.lastMessage?.role == "user") "You: " else "${item.character.name}: "
            val previewText = item.lastMessage?.content ?: "No messages yet."

            Text(
                text = "$previewPrefix$previewText",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1
            )
        }

        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary.copy(alpha = 0.5f))
        }
    }
}
