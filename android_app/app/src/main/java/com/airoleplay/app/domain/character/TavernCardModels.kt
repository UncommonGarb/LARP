package com.airoleplay.app.domain.character

import com.google.gson.annotations.SerializedName

data class TavernCardV2(
    val spec: String = "chara_card_v2",
    val spec_version: String = "2.0",
    val data: TavernCardV2Data
)

data class TavernCardV2Data(
    val name: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val first_mes: String,
    val mes_example: String,
    val creator_notes: String = "",
    val system_prompt: String = "",
    val post_history_instructions: String = "",
    val alternate_greetings: List<String> = emptyList(),
    val character_book: CharacterBook? = null,
    val tags: List<String> = emptyList(),
    val creator: String = "",
    val character_version: String = ""
)

data class CharacterBook(
    val name: String? = null,
    val description: String? = null,
    val scan_depth: Int = 10,
    val token_budget: Int = 500,
    val recursive_scanning: Boolean = false,
    val extensions: Map<String, Any> = emptyMap(),
    val entries: List<CharacterBookEntry> = emptyList()
)

data class CharacterBookEntry(
    val keys: List<String>,
    val content: String,
    val extensions: Map<String, Any> = emptyMap(),
    val enabled: Boolean = true,
    val insertion_order: Int = 0,
    val case_sensitive: Boolean = false,
    val name: String? = null,
    val priority: Int = 10,
    val id: Int? = null,
    val constant: Boolean = false
)

// Legacy V1 Structure
data class TavernCardV1(
    val name: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val first_mes: String,
    val mes_example: String
)
