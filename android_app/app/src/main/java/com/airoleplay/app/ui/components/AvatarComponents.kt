package com.airoleplay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.airoleplay.app.ui.theme.DarkBackground
import com.airoleplay.app.ui.theme.SurfaceCard
import com.airoleplay.app.ui.theme.TextPrimary
import com.airoleplay.app.utils.ImageUtils

@Composable
fun CharacterAvatar(
    name: String,
    avatarPath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = CircleShape
) {
    val model = remember(avatarPath) {
        ImageUtils.resolveModel(avatarPath)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(SurfaceCard)
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            InitialsPlaceholder(name = name, size = size)
        }
    }
}

@Composable
fun InitialsPlaceholder(
    name: String,
    size: Dp
) {
    val initials = remember(name) {
        name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
            .uppercase()
    }

    val gradientBrush = remember(name) {
        val colorPairs = listOf(
            listOf(Color(0xFF6A11CB), Color(0xFF2575FC)), // Deep Purple to Blue
            listOf(Color(0xFFFF512F), Color(0xFFDD2476)), // Red to Pink
            listOf(Color(0xFF00b09b), Color(0xFF96c93d)), // Greenish
            listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), // Vivid Purple
            listOf(Color(0xFFf953c6), Color(0xFFb91d73)), // Pink to Dark Pink
            listOf(Color(0xFF1fa2ff), Color(0xFF12d8fa), Color(0xFFa6ffcb)), // Sky Tri-tone
            listOf(Color(0xFFff9966), Color(0xFFff5e62)), // Orange to Red
            listOf(Color(0xFF4568DC), Color(0xFFB06AB3))  // Indigo to Lavender
        )
        val selectedColors = colorPairs[Math.abs(name.hashCode()) % colorPairs.size]
        Brush.linearGradient(colors = selectedColors)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Thin,
                fontSize = (size.value * 0.45f).sp,
                letterSpacing = 1.sp
            )
        )
    }
}
