package com.airoleplay.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.airoleplay.app.ui.theme.AccentColor
import com.airoleplay.app.ui.theme.SurfaceCard
import com.airoleplay.app.ui.theme.TextPrimary
import com.airoleplay.app.ui.theme.TextSecondary

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    NavigationBar(
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        val screens = listOf(
            Screen.Chats,
            Screen.Create,
            Screen.Characters,
            Screen.Settings
        )
        val icons = listOf(
            Icons.Default.Chat,
            Icons.Default.Add,
            Icons.Default.Home,
            Icons.Default.Settings
        )
        val labels = listOf("Chats", "Create", "Characters", "Settings")

        screens.forEachIndexed { index, screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Chats.route) {
                                saveState = true
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = labels[index]
                    )
                },
                label = { Text(labels[index]) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentColor,
                    unselectedIconColor = TextSecondary,
                    selectedTextColor = AccentColor,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = SurfaceCard // No pill background
                )
            )
        }
    }
}
