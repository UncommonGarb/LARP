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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airoleplay.app.utils.ImageUtils
import com.airoleplay.app.data.local.entity.ChatMessageEntity
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import com.airoleplay.app.domain.prompt.PromptBuilder
import com.airoleplay.app.ui.components.CharacterAvatar
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
    var showMemoriesPanel by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isOocMode by remember { mutableStateOf(false) }
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


    val isImeVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

    val isAtBottom by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                true
            } else {
                // In reverseLayout, index 0 is at the bottom.
                visibleItems.any { it.index == 0 }
            }
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.partialGeneration) {
        if (isAtBottom) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && uiState.messages.isNotEmpty()) {
            kotlinx.coroutines.delay(200) // Wait for keyboard animation
            listState.animateScrollToItem(0)
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
                        CharacterAvatar(
                            name = uiState.character?.name ?: "Character",
                            avatarPath = uiState.character?.avatarImagePath,
                            size = 40.dp
                        )

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
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${uiState.currentContextTokens}/${uiState.maxContextTokens} t",
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
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
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
                    IconButton(
                        onClick = { isOocMode = !isOocMode },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isOocMode) AccentColor else SurfaceCard)
                    ) {
                        Text("OOC", color = if (isOocMode) Color.White else TextSecondary, style = MaterialTheme.typography.labelMedium)
                    }

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

                    if (uiState.isGenerating) {
                        FloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.stopGeneration()
                            },
                            containerColor = ErrorRed,
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop Generation", modifier = Modifier.size(24.dp))
                        }
                    } else {
                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val finalMessage = if (isOocMode) "[OOC: $inputText]" else inputText
                                    viewModel.sendMessage(finalMessage, attachedImageUri?.toString())
                                    inputText = ""
                                    attachedImageUri = null
                                }
                            },
                            containerColor = if (inputText.isNotBlank()) AccentColor else SurfaceCard,
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                        }
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
            if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CharacterAvatar(
                        name = uiState.character?.name ?: "Character",
                        avatarPath = uiState.character?.avatarImagePath,
                        size = 100.dp // Changed to 100.dp to match original visual size, as 32.dp was too small for this context.
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Start Roleplaying with ${uiState.character?.name ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send a message to begin the scenario.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
                    reverseLayout = true
                ) {
                    if (uiState.isGenerating) {
                        item {
                            if (uiState.partialGeneration.isNotEmpty()) {
                                MessageBubble(
                                    message = ChatMessageEntity(
                                        sessionId = 0,
                                        role = "assistant",
                                        content = uiState.partialGeneration
                                    ),
                                    isUser = false,
                                    isPreviousSameRole = uiState.messages.lastOrNull()?.role == "assistant",
                                    isNextSameRole = false,
                                    avatarPath = uiState.character?.avatarImagePath,
                                    isStreaming = true,
                                    onRegenerate = { },
                                    onDelete = { }
                                )
                            } else {
                                TypingIndicatorBubble(
                                    avatarPath = uiState.character?.avatarImagePath,
                                    isPreviousSameRole = uiState.messages.lastOrNull()?.role == "assistant"
                                )
                            }
                        }
                    }

                    items(
                        count = uiState.messages.size,
                        key = { index -> uiState.messages[uiState.messages.size - 1 - index].id }
                    ) { index ->
                        val reversedIndex = uiState.messages.size - 1 - index
                        val message = uiState.messages[reversedIndex]
                        val isUser = message.role == "user"
                        val isPreviousSameRole = if (reversedIndex > 0) uiState.messages[reversedIndex - 1].role == message.role else false
                        val isNextSameRole = if (reversedIndex < uiState.messages.size - 1) uiState.messages[reversedIndex + 1].role == message.role else false

                        MessageBubble(
                            message = message,
                            isUser = isUser,
                            isPreviousSameRole = isPreviousSameRole,
                            isNextSameRole = isNextSameRole,
                            avatarPath = if (isUser) uiState.activePersona?.avatarImagePath else uiState.character?.avatarImagePath,
                            onRegenerate = { viewModel.regenerateLastMessage() },
                            onDelete = { viewModel.deleteMessage(message) },
                            onEdit = { newContent -> viewModel.editMessage(message, newContent) },
                            fetchAlternatives = { groupId -> viewModel.getSwipeAlternatives(groupId) },
                            onSwipeAlternative = { viewModel.switchSwipeAlternative(message.swipeGroupId!!, it) }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isAtBottom,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(uiState.messages.size)
                        }
                    },
                    containerColor = SurfaceCard,
                    contentColor = TextPrimary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom")
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
                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Memory Bank")
                                if (uiState.memories.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(containerColor = AccentColor, contentColor = Color.White) {
                                        Text(uiState.memories.size.toString())
                                    }
                                }
                            }
                        },
                        leadingContent = { Icon(Icons.Default.Book, contentDescription = null, tint = AccentColor) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            showMemoriesPanel = true
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Extract Memory") },
                        leadingContent = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentColor) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            viewModel.extractMemory()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (showMemoriesPanel) {
        MemoriesBottomSheet(
            memories = uiState.memories,
            onDismiss = { showMemoriesPanel = false },
            onToggleMemory = viewModel::toggleMemory,
            onPinMemory = viewModel::pinMemory,
            onDeleteMemory = viewModel::deleteMemory,
            onExtractNow = {
                showMemoriesPanel = false
                viewModel.extractMemory()
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessageEntity,
    isUser: Boolean,
    isPreviousSameRole: Boolean = false,
    isNextSameRole: Boolean = false,
    avatarPath: String?,
    isStreaming: Boolean = false,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit = {},
    fetchAlternatives: suspend (String) -> List<ChatMessageEntity> = { emptyList() },
    onSwipeAlternative: (Long) -> Unit = {}
) {
    var showOptions by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(message.content) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (isPreviousSameRole) 2.dp else 8.dp,
                bottom = if (isNextSameRole) 2.dp else 8.dp
            ),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            if (!isNextSameRole) {
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
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (!isUser && isPreviousSameRole) 4.dp else 16.dp,
                            topEnd = if (isUser && isPreviousSameRole) 4.dp else 16.dp,
                            bottomStart = if (!isUser && isNextSameRole) 4.dp else 16.dp,
                            bottomEnd = if (isUser && isNextSameRole) 4.dp else 16.dp
                        )
                    )
                    .background(if (message.isError) ErrorRed.copy(alpha = 0.8f) else if (isUser) UserBubbleColor else AssistantBubbleColor)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                // Trigger the options menu
                                showOptions = true
                            }
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
                    if (isEditing) {
                        TextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = if (isUser) Color.White else TextPrimary,
                                unfocusedTextColor = if (isUser) Color.White else TextPrimary,
                                focusedIndicatorColor = AccentColor,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                isEditing = false
                                editText = message.content
                            }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            TextButton(onClick = {
                                isEditing = false
                                onEdit(editText)
                            }) {
                                Text("Save", color = AccentColor)
                            }
                        }
                    } else if (message.content.isNotBlank()) {
                        Text(
                            text = parseMarkdown(message.content + if (isStreaming) " \u2588" else ""),
                            color = if (isUser || message.isError) Color.White else TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Timestamp
                if (showOptions && !isEditing) {
                    val formattedTime = remember(message.timestamp) {
                        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
                    }
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (message.isError) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { onDelete() }) {
                        Text("Dismiss", color = TextSecondary)
                    }
                    TextButton(onClick = {
                        onDelete()
                        onRegenerate()
                    }) {
                        Text("Retry", color = AccentColor)
                    }
                }
            }

            if (!isUser && !isStreaming && message.swipeGroupId != null && !isEditing && !message.isError) {
                var alternatives by remember { mutableStateOf<List<ChatMessageEntity>>(emptyList()) }
                var currentIndex by remember { mutableStateOf(0) }

                LaunchedEffect(message.swipeGroupId, message.id) {
                    val msgs = fetchAlternatives(message.swipeGroupId)
                    alternatives = msgs
                    currentIndex = msgs.indexOfFirst { it.id == message.id }.coerceAtLeast(0)
                }

                if (alternatives.isNotEmpty()) {
                    val haptic = LocalHapticFeedback.current
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
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSwipeAlternative(alternatives[currentIndex - 1].id)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
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
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSwipeAlternative(alternatives[currentIndex + 1].id)
                                } else if (currentIndex == alternatives.size - 1) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRegenerate()
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Alternative",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Custom themed context menu
            AnimatedVisibility(
                visible = showOptions,
                enter = scaleIn(initialScale = 0.9f) + fadeIn(),
                exit = scaleOut(targetScale = 0.9f) + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(top = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        if (!isUser) {
                            IconButton(onClick = {
                                showOptions = false
                                onRegenerate()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = AccentColor)
                            }
                        }
                        IconButton(onClick = {
                            showOptions = false
                            isEditing = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentColor)
                        }
                        IconButton(onClick = {
                            showOptions = false
                            // Add actual clipboard copy here if desired
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentColor)
                        }
                        IconButton(onClick = {
                            showOptions = false
                            onDelete()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesBottomSheet(
    memories: List<com.airoleplay.app.data.local.entity.MemoryEntity>,
    onDismiss: () -> Unit,
    onToggleMemory: (com.airoleplay.app.data.local.entity.MemoryEntity) -> Unit,
    onPinMemory: (com.airoleplay.app.data.local.entity.MemoryEntity) -> Unit,
    onDeleteMemory: (com.airoleplay.app.data.local.entity.MemoryEntity) -> Unit,
    onExtractNow: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Memory Bank", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                Button(
                    onClick = onExtractNow,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract Now")
                }
            }
            
            if (memories.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No memories extracted yet.", color = TextSecondary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(memories.sortedByDescending { it.createdAt }) { memory ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = memory.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (memory.isActive) TextPrimary else TextSecondary,
                                    fontStyle = if (memory.isActive) FontStyle.Normal else FontStyle.Italic
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (memory.isPinned) {
                                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = AccentColor, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (memory.isActive) "Active" else "Inactive",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (memory.isActive) SuccessGreen else TextSecondary
                                    )
                                }
                            }
                            
                            // Actions
                            Row {
                                IconButton(onClick = { onPinMemory(memory) }) {
                                    Icon(
                                        Icons.Default.PushPin, 
                                        contentDescription = "Pin", 
                                        tint = if (memory.isPinned) AccentColor else TextSecondary
                                    )
                                }
                                IconButton(onClick = { onToggleMemory(memory) }) {
                                    Icon(
                                        if (memory.isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff, 
                                        contentDescription = "Toggle Active", 
                                        tint = if (memory.isActive) SuccessGreen else TextSecondary
                                    )
                                }
                                IconButton(onClick = { onDeleteMemory(memory) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble(
    avatarPath: String?,
    isPreviousSameRole: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isPreviousSameRole) {
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
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(AssistantBubbleColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "typing")
                val animations = listOf(0, 1, 2).map { index ->
                    infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 600
                                0.0f at 0 with LinearOutSlowInEasing
                                1.0f at 200 with LinearOutSlowInEasing
                                0.0f at 400 with LinearOutSlowInEasing
                                0.0f at 600 with LinearOutSlowInEasing
                            },
                            repeatMode = RepeatMode.Restart,
                            initialStartOffset = StartOffset(index * 150)
                        ),
                        label = "dot$index"
                    )
                }

                animations.forEach { anim ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer {
                                translationY = -anim.value * 8f
                                alpha = 0.5f + (anim.value * 0.5f)
                            }
                            .clip(CircleShape)
                            .background(TextPrimary)
                    )
                }
            }
        }
    }
}

/**
 * A basic markdown segments parser that handles bold (**), italic (*), and strikethrough (~~).
 */
fun parseMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("*", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, background = Color.Black.copy(alpha = 0.2f))) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
