package com.airoleplay.app.domain.prompt

import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.data.local.entity.ChatMessageEntity
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import com.airoleplay.app.data.remote.models.PromptMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private val promptBuilder = PromptBuilder(TokenCounter())

    @Test
    fun `test macros are replaced correctly`() {
        val character = CharacterEntity(
            name = "Aria",
            description = "{{user}} is talking to {{char}}.",
            personalitySummary = "",
            scenario = "",
            firstMessage = "",
            exampleDialogue = "",
            tags = "",
            systemPromptOverride = "You are {{char}} talking to {{user}}."
        )

        val persona = UserPersonaEntity(name = "John", description = "A test user", isActive = true)

        val messages = promptBuilder.buildPromptMessages(
            character = character,
            persona = persona,
            globalSystemPrompt = "",
            messages = emptyList(),
            lorebookEntries = emptyList(),
            authorsNote = character.authorsNote,
            authorsNoteDepth = character.authorsNoteDepth,
            contextSizeLimit = 4096
        )

        val systemMessage = messages.find { it.role == "system" }
        println("MACROS TEST OUTPUT: " + systemMessage?.content)
        assertTrue(systemMessage != null)
        val content = systemMessage!!.content
        assertTrue(content.contains("John is talking to Aria"))
        assertTrue(content.contains("You are Aria talking to John"))
    }

    @Test
    fun `test context trims oldest messages first`() {
        val character = CharacterEntity(
            name = "Aria",
            description = "Test char",
            personalitySummary = "",
            scenario = "",
            firstMessage = "",
            exampleDialogue = "",
            tags = "",
            systemPromptOverride = "Test Prompt"
        )

        val persona = UserPersonaEntity(name = "John", description = "", isActive = true)

        // Create 20 messages. A very small context limit (e.g. 50 tokens) should drop oldest ones.
        val chatHistory = (1..20).map { i ->
            ChatMessageEntity(sessionId = 1, role = if (i % 2 == 0) "assistant" else "user", content = "Message $i", timestamp = i.toLong())
        }

        val messages = promptBuilder.buildPromptMessages(
            character = character,
            persona = persona,
            globalSystemPrompt = "Test Prompt",
            messages = chatHistory,
            lorebookEntries = emptyList(),
            authorsNote = character.authorsNote,
            authorsNoteDepth = character.authorsNoteDepth,
            contextSizeLimit = 200 // Very small limit
        )

        val historyMessages = messages.filter { it.role != "system" }
        println("TRIM TEST HISTORY SIZE: " + historyMessages.size)
        historyMessages.forEach { println("TRIM TEST MSG: " + it.content) }

        // Ensure oldest messages were trimmed
        assertTrue(historyMessages.none { it.content == "Message 1" })

        // Ensure newest messages ARE present
        assertTrue(historyMessages.any { it.content.contains("Message 20") })
    }

    @Test
    fun `test lorebook entries injected correctly`() {
        val character = CharacterEntity(
            name = "Aria",
            description = "Test char",
            personalitySummary = "",
            scenario = "",
            firstMessage = "",
            exampleDialogue = "",
            tags = "",
            systemPromptOverride = "Test Prompt"
        )

        val lorebookEntries = listOf(
            LorebookEntryEntity(
                title = "Magic System",
                keywords = "magic,spells",
                content = "Magic in this world is based on elements.",
                isEnabled = true
            )
        )

        val chatHistory = listOf(
            ChatMessageEntity(sessionId = 1, role = "user", content = "Tell me about magic.")
        )

        val messages = promptBuilder.buildPromptMessages(
            character = character,
            persona = null,
            globalSystemPrompt = "Test Prompt",
            messages = chatHistory,
            lorebookEntries = lorebookEntries,
            authorsNote = character.authorsNote,
            authorsNoteDepth = character.authorsNoteDepth,
            contextSizeLimit = 4096
        )

        val systemMessage = messages.find { it.role == "system" }
        println("LOREBOOK TEST OUTPUT: " + systemMessage?.content)
        assertTrue(systemMessage != null)
        assertTrue(systemMessage!!.content.contains("Magic in this world is based on elements."))
    }

    @Test
    fun `test authors note depth injection`() {
        val character = CharacterEntity(
            name = "Aria",
            description = "Test char",
            personalitySummary = "",
            scenario = "",
            firstMessage = "",
            exampleDialogue = "",
            tags = "",
            systemPromptOverride = "Test Prompt"
        )

        val chatHistory = listOf(
            ChatMessageEntity(sessionId = 1, role = "user", content = "Message 1"),
            ChatMessageEntity(sessionId = 1, role = "assistant", content = "Message 2"),
            ChatMessageEntity(sessionId = 1, role = "user", content = "Message 3"),
            ChatMessageEntity(sessionId = 1, role = "assistant", content = "Message 4")
        )

        val messages = promptBuilder.buildPromptMessages(
            character = character,
            persona = null,
            globalSystemPrompt = "Test Prompt",
            messages = chatHistory,
            lorebookEntries = emptyList(),
            authorsNote = "Speak in rhymes.",
            authorsNoteDepth = 2,
            contextSizeLimit = 4096
        )

        val allMessages = messages.toMutableList()
        println("AUTHORS NOTE TEST MSGS:")
        allMessages.forEachIndexed { idx, m -> println("  $idx: [${m.role}] ${m.content}") }
        val index = allMessages.indexOfFirst { it.content.contains("Speak in rhymes.") }

        assertEquals(3, index)
    }

    @Test
    fun `test authors note depth limit`() {
        val character = CharacterEntity(
            name = "Aria",
            description = "Test char",
            personalitySummary = "",
            scenario = "",
            firstMessage = "",
            exampleDialogue = "",
            tags = "",
            systemPromptOverride = "Test Prompt"
        )

        val chatHistory = listOf(
            ChatMessageEntity(sessionId = 1, role = "user", content = "Message 1"),
        )

        val messages = promptBuilder.buildPromptMessages(
            character = character,
            persona = null,
            globalSystemPrompt = "Test Prompt",
            messages = chatHistory,
            lorebookEntries = emptyList(),
            authorsNote = "Speak in rhymes.",
            authorsNoteDepth = 5,
            contextSizeLimit = 4096
        )

        val allMessages = messages.toMutableList()
        val index = allMessages.indexOfFirst { it.content.contains("Speak in rhymes.") }

        assertEquals(1, index) // Should cap at top of history
    }
}
