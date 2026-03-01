package com.airoleplay.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.airoleplay.app.ui.screens.chat.LorebookScreen
import com.airoleplay.app.ui.screens.chats.ChatsScreen
import com.airoleplay.app.ui.screens.create.CreateScreen
import com.airoleplay.app.ui.screens.discover.CharacterScreen
import com.airoleplay.app.ui.screens.settings.*

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    // Determine if bottom bar should be shown
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarScreens = listOf(
        Screen.Characters.route,
        Screen.Chats.route,
        Screen.Create.route,
        Screen.Settings.route
    )
    val showBottomBar = currentRoute in bottomBarScreens

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Onboarding.route
            ) {
                composable(Screen.Onboarding.route) {
                    com.airoleplay.app.ui.screens.onboarding.OnboardingScreen(navController)
                }
                composable(Screen.Characters.route) {
                    CharacterScreen(navController, innerPadding)
                }
                composable(Screen.Chats.route) {
                    ChatsScreen(navController, innerPadding)
                }
                composable(
                    Screen.Create.route,
                    arguments = listOf(navArgument("characterId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) {
                    CreateScreen(navController)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(navController, innerPadding)
                }
                composable(Screen.CharacterDetail.route) { backStackEntry ->
                    val characterId = backStackEntry.arguments?.getString("characterId")?.toLongOrNull() ?: return@composable
                    com.airoleplay.app.ui.screens.detail.CharacterDetailScreen(navController, characterId)
                }
                composable(Screen.ChatSession.route) { backStackEntry ->
                    val sessionId = backStackEntry.arguments?.getString("sessionId")?.toLongOrNull() ?: return@composable
                    com.airoleplay.app.ui.screens.chat.ChatScreen(navController)
                }
                composable(Screen.ConnectionSettings.route) { backStackEntry ->
                     val connectionId = backStackEntry.arguments?.getString("connectionId")?.toLongOrNull() ?: return@composable
                     com.airoleplay.app.ui.screens.settings.ConnectionSettingsScreen(navController)
                }
                composable(Screen.PersonaSettings.route) {
                    com.airoleplay.app.ui.screens.persona.PersonaScreen(navController)
                }
                composable(Screen.BackendList.route) {
                    BackendListScreen(navController)
                }
                composable(Screen.GlobalGenerationSettings.route) {
                    GlobalGenerationSettingsScreen(navController)
                }
                composable(Screen.Lorebook.route) { backStackEntry ->
                    val characterId = backStackEntry.arguments?.getString("characterId")?.toLongOrNull() ?: return@composable
                    LorebookScreen(navController, characterId)
                }
            }
        }
    }
}
