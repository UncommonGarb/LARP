package com.airoleplay.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airoleplay.app.data.local.entity.ChatMessageEntity
import com.airoleplay.app.ui.navigation.Screen
import com.airoleplay.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val haptic = LocalHapticFeedback.current

    val isAtBottom by remember {
        derivedStateOf {
            val lastItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastItem == null || lastItem.index >= listState.layoutInfo.totalItemsCount - 2
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.partialGeneration) {
        if (uiState.messages.isNotEmpty() && isAtBottom) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            uiState.character?.id?.let {
                                navController.navigate(Screen.CharacterDetail.createRoute(it))
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceCard)
                        ) {
                            if (uiState.character?.avatarImagePath != null) {
                                AsyncImage(
                                    model = uiState.character!!.avatarImagePath,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = uiState.character?.name ?: "Loading...",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val statusColor = when (uiState.connectionStatus) {
                                    ConnectionStatus.CONNECTED -> SuccessGreen
                                    ConnectionStatus.DISCONNECTED -> ErrorRed
                                    ConnectionStatus.CONNECTING -> WarningYellow
                                    else -> TextSecondary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = uiState.connectionStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(DarkBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (attachedImageUri != null) {
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "Attached",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { attachedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(SurfaceCard.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...", color = TextSecondary) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            IconButton(onClick = { /* Image Picker logic */ }) {
                                Icon(Icons.Default.Add, contentDescription = "Attach", tint = TextSecondary)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val base64Images = mutableListOf<String>()
                                viewModel.sendMessage(inputText, base64Images)
                                inputText = ""
                                attachedImageUri = null
                            }
                        },
                        containerColor = if (inputText.isNotBlank()) AccentColor else SurfaceCard,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.messages) { message ->
                MessageBubble(
                    message = message,
                    isUser = message.role == "user",
                    avatarPath = if (message.role == "user") uiState.activePersona?.avatarImagePath else uiState.character?.avatarImagePath,
                    onRegenerate = { viewModel.regenerateLastMessage() },
                    onDelete = { viewModel.deleteMessage(message) },
                    fetchAlternatives = { groupId -> viewModel.getSwipeAlternatives(groupId) },
                    onSwipeAlternative = { viewModel.switchSwipeAlternative(message.swipeGroupId!!, it) }
                )
            }

            if (uiState.isGenerating && uiState.partialGeneration.isNotEmpty()) {
                item {
                    MessageBubble(
                        message = ChatMessageEntity(
                            sessionId = 0,
                            role = "assistant",
                            content = uiState.partialGeneration
                        ),
                        isUser = false,
                        avatarPath = uiState.character?.avatarImagePath,
                        isStreaming = true,
                        onRegenerate = { },
                        onDelete = { }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessageEntity,
    isUser: Boolean,
    avatarPath: String?,
    isStreaming: Boolean = false,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    fetchAlternatives: suspend (String) -> List<ChatMessageEntity> = { emptyList() },
    onSwipeAlternative: (Long) -> Unit = {}
) {
    var showOptions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
            ) {
                if (avatarPath != null) {
                    AsyncImage(
                        model = avatarPath,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(0.8f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) UserBubbleColor else AssistantBubbleColor)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showOptions = true }
                        )
                    }
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content + if (isStreaming) " \u2588" else "",
                    color = if (isUser) Color.White else TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (!isUser && !isStreaming && message.swipeGroupId != null) {
                var alternatives by remember { mutableStateOf<List<ChatMessageEntity>>(emptyList()) }
                var currentIndex by remember { mutableStateOf(0) }

                LaunchedEffect(message.swipeGroupId) {
                    val msgs = fetchAlternatives(message.swipeGroupId)
                    alternatives = msgs
                    currentIndex = msgs.indexOfFirst { it.id == message.id }.coerceAtLeast(0)
                }

                if (alternatives.size > 1) {
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentIndex > 0) {
                                    onSwipeAlternative(alternatives[currentIndex - 1].id)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Previous Alternative",
                                tint = if (currentIndex > 0) TextSecondary else Color.Transparent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "${currentIndex + 1} / ${alternatives.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                if (currentIndex < alternatives.size - 1) {
                                    onSwipeAlternative(alternatives[currentIndex + 1].id)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Next Alternative",
                                tint = if (currentIndex < alternatives.size - 1) TextSecondary else Color.Transparent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showOptions,
                onDismissRequest = { showOptions = false }
            ) {
                if (!isUser) {
                    DropdownMenuItem(
                        text = { Text("Regenerate") },
                        onClick = {
                            showOptions = false
                            onRegenerate()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Copy Text") },
                    onClick = { showOptions = false }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = ErrorRed) },
                    onClick = {
                        showOptions = false
                        onDelete()
                    }
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
            ) {
                if (avatarPath != null) {
                    AsyncImage(
                        model = avatarPath,
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
