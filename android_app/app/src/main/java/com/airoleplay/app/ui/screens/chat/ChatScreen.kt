package com.airoleplay.app.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airoleplay.app.utils.ImageUtils
import com.airoleplay.app.data.local.entity.ChatMessageEntity
import com.airoleplay.app.ui.navigation.Screen
import com.airoleplay.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val haptic = LocalHapticFeedback.current

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val copiedPath = ImageUtils.copyUriToInternalStorage(context, uri, "attached_${System.currentTimeMillis()}.jpg")
                if (copiedPath != null) {
                    attachedImageUri = android.net.Uri.parse("file://$copiedPath")
                }
            }
        }
    }

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
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { showMenu = !showMenu }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkBackground)
                        ) {
                            val avatarModel = remember(uiState.character?.avatarImagePath) {
                                ImageUtils.resolveModel(uiState.character?.avatarImagePath)
                            }
                            if (avatarModel != null) {
                                AsyncImage(
                                    model = avatarModel,
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
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceCard)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(DarkBackground)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AnimatedVisibility(
                    visible = attachedImageUri != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
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
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(Icons.Default.Add, contentDescription = "Attach", tint = TextSecondary)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.sendMessage(inputText, attachedImageUri?.toString())
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
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

            // Custom Sliding Menu (Slides down from beneath header as overlay)
            AnimatedVisibility(
                visible = showMenu,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(bottom = 16.dp)
                ) {
                    // Menu Items
                    ListItem(
                        headlineContent = { Text("Persona") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(DarkBackground)
                            ) {
                                val personaModel = remember(uiState.activePersona?.avatarImagePath) {
                                    ImageUtils.resolveModel(uiState.activePersona?.avatarImagePath)
                                }
                                if (personaModel != null) {
                                    AsyncImage(
                                        model = personaModel,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = AccentColor)
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            showMenu = false
                            navController.navigate(Screen.PersonaSettings.route)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Lorebook") },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = AccentColor) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            uiState.character?.id?.let {
                                navController.navigate(Screen.Lorebook.createRoute(it))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Edit Character") },
                        leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, tint = AccentColor) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            uiState.character?.id?.let {
                                navController.navigate(Screen.Create.createRoute(it))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Chat History") },
                        leadingContent = { Icon(Icons.Default.History, contentDescription = null, tint = AccentColor) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            uiState.character?.id?.let {
                                navController.navigate(Screen.CharacterDetail.createRoute(it))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                val avatarModel = remember(avatarPath) {
                    ImageUtils.resolveModel(avatarPath)
                }
                if (avatarModel != null) {
                    AsyncImage(
                        model = avatarModel,
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
                Column {
                    if (message.attachedImagePath != null) {
                        val attachedModel = remember(message.attachedImagePath) {
                            ImageUtils.resolveModel(message.attachedImagePath)
                        }
                        AsyncImage(
                            model = attachedModel,
                            contentDescription = "Attached Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .padding(bottom = if (message.content.isNotBlank()) 8.dp else 0.dp)
                        )
                    }
                    if (message.content.isNotBlank()) {
                        Text(
                            text = message.content + if (isStreaming) " \u2588" else "",
                            color = if (isUser) Color.White else TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
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

            // Custom themed context menu
            AnimatedVisibility(
                visible = showOptions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Min)) {
                        if (!isUser) {
                            ContextMenuItem(
                                text = "Regenerate",
                                icon = Icons.Default.Refresh,
                                onClick = {
                                    showOptions = false
                                    onRegenerate()
                                }
                            )
                        }
                        ContextMenuItem(
                            text = "Copy Text",
                            icon = Icons.Default.ContentCopy,
                            onClick = { showOptions = false }
                        )
                        ContextMenuItem(
                            text = "Delete",
                            icon = Icons.Default.Delete,
                            color = ErrorRed,
                            onClick = {
                                showOptions = false
                                onDelete()
                            }
                        )
                    }
                }
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
                val avatarModel = remember(avatarPath) {
                    ImageUtils.resolveModel(avatarPath)
                }
                if (avatarModel != null) {
                    AsyncImage(
                        model = avatarModel,
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun ContextMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (color == ErrorRed) color else AccentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
