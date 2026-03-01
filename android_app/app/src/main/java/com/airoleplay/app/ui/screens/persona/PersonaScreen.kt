package com.airoleplay.app.ui.screens.persona

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import com.airoleplay.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(
    navController: NavController,
    viewModel: PersonaViewModel = hiltViewModel()
) {
    val personas by viewModel.personas.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Personas", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add Persona")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(personas) { persona ->
                    PersonaCard(
                        persona = persona,
                        onActivate = { viewModel.setActivePersona(persona.id) },
                        onDelete = { viewModel.deletePersona(persona) },
                        canDelete = personas.size > 1
                    )
                }
            }
        }

    val context = androidx.compose.ui.platform.LocalContext.current
        if (showAddDialog) {
            AddPersonaDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, desc, uri ->
                viewModel.addPersona(context, name, desc, uri)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun PersonaCard(
    persona: UserPersonaEntity,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onActivate),
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
                if (persona.avatarImagePath != null) {
                    AsyncImage(
                        model = persona.avatarImagePath,
                        contentDescription = persona.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = persona.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (persona.description.isNotBlank()) {
                    Text(
                        text = persona.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (persona.isActive) {
                RadioButton(
                    selected = true,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = AccentColor)
                )
            } else if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun AddPersonaDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        avatarUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Add Persona", color = TextPrimary) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(DarkBackground)
                        .align(Alignment.CenterHorizontally)
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Add Avatar", tint = TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = AccentColor,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = AccentColor,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, description, avatarUri) },
                enabled = name.isNotBlank()
            ) {
                Text("Add", color = AccentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
