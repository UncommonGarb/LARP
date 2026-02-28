package com.airoleplay.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Discover : Screen("discover")
    object Chats : Screen("chats")
    object Create : Screen("create")
    object Settings : Screen("settings")
    object CharacterDetail : Screen("character_detail/{characterId}") {
        fun createRoute(characterId: Long) = "character_detail/\$characterId"
    }
    object ChatSession : Screen("chat_session/{sessionId}") {
        fun createRoute(sessionId: Long) = "chat_session/\$sessionId"
    }
    object ConnectionSettings : Screen("connection_settings/{connectionId}") {
        fun createRoute(connectionId: Long) = "connection_settings/\$connectionId"
    }
    object PersonaSettings : Screen("persona_settings")
}
