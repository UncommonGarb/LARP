package com.airoleplay.app.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.ui.navigation.Screen
import com.airoleplay.app.ui.theme.*
import com.airoleplay.app.utils.ImageUtils
import java.io.File

@Composable
fun CharacterScreen(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: CharacterViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val characters by viewModel.characters.collectAsState()
    val recentCharacters by viewModel.recentCharacters.collectAsState()
    val tags by viewModel.allAvailableTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val activePersona by viewModel.activePersona.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CharacterTopBar(
                searchQuery = searchQuery,
                onSearchChange = viewModel::updateSearchQuery,
                personaAvatarPath = activePersona?.avatarImagePath
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = contentPadding.calculateBottomPadding())
        ) {
            // Tags Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tags) { tag ->
                    FilterChip(
                        selected = tag == selectedTag,
                        onClick = { viewModel.selectTag(tag) },
                        label = { Text(tag) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Recents Section (Only show if there are recents)
            if (recentCharacters.isNotEmpty()) {
                Text(
                    text = "Continue Chatting",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentCharacters) { char ->
                        RecentCharacterCard(char = char) {
                            navController.navigate(Screen.CharacterDetail.createRoute(char.id))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // All Characters Grid
            Text(
                text = "All Characters",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(characters) { char ->
                    CharacterGridCard(char = char) {
                        navController.navigate(Screen.CharacterDetail.createRoute(char.id))
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    personaAvatarPath: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Characters",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            // Persona Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
            ) {
                val model = remember(personaAvatarPath) {
                    ImageUtils.resolveModel(personaAvatarPath)
                }
                if (model != null) {
                    AsyncImage(
                        model = model,
                        contentDescription = "Active Persona",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search characters...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun RecentCharacterCard(char: CharacterEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
    ) {
        val model = remember(char.avatarImagePath) {
            ImageUtils.resolveModel(char.avatarImagePath)
        }
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = char.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
            }
        }
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 100f
                    )
                )
        )
        Text(
            text = char.name,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
    }
}

@Composable
fun CharacterGridCard(char: CharacterEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val model = remember(char.avatarImagePath) {
                ImageUtils.resolveModel(char.avatarImagePath)
            }
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = char.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                }
            }
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 200f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = char.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = char.personalitySummary.take(40) + if (char.personalitySummary.length > 40) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
