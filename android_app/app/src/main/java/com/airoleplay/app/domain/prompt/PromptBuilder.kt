package com.airoleplay.app.domain.prompt

import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.data.local.entity.ChatMessageEntity
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import com.airoleplay.app.data.remote.models.PromptMessage

class PromptBuilder @javax.inject.Inject constructor(
    private val tokenCounter: TokenCounter
) {

    fun buildPromptMessages(
        character: CharacterEntity,
        persona: UserPersonaEntity?,
        globalSystemPrompt: String,
        messages: List<ChatMessageEntity>,
        lorebookEntries: List<LorebookEntryEntity>,
        authorsNote: String?,
        authorsNoteDepth: Int = 2,
        contextSizeLimit: Int
    ): List<PromptMessage> {

        val personaName = persona?.name ?: "User"

        // 1. System Prompt
        var systemContent = character.systemPromptOverride ?: globalSystemPrompt
        systemContent = replaceMacros(systemContent, character.name, personaName)

        // 2. Character Description
        var charDescription = replaceMacros(character.description, character.name, personaName)
        if (charDescription.isNotBlank()) {
            systemContent += "\n\n[${character.name}]: $charDescription"
        }

        // 3. Personality
        if (character.personalitySummary.isNotBlank()) {
            systemContent += "\n\nPersonality: ${character.personalitySummary}"
        }

        // 4. Scenario
        if (character.scenario.isNotBlank()) {
            systemContent += "\n\nScenario: ${replaceMacros(character.scenario, character.name, personaName)}"
        }

        // 5. Persona Description
        if (persona != null && persona.description.isNotBlank()) {
            systemContent += "\n\nThe person talking with ${character.name} is $personaName: ${persona.description}."
        }

        // 6. Lorebook
        val triggeredLorebook = LorebookScanner.scan(
            messages.map { it.content }.joinToString(" "),
            lorebookEntries
        )
        if (triggeredLorebook.isNotEmpty()) {
            systemContent += "\n\n[World Info]\n"
            triggeredLorebook.forEach { entry ->
                systemContent += "${entry.content}\n"
            }
            systemContent += "[/World Info]"
        }

        // 7. Example Dialogues
        if (character.exampleDialogue.isNotBlank()) {
            systemContent += "\n\n[Example Dialogue]\n${replaceMacros(character.exampleDialogue, character.name, personaName)}\n[/Example Dialogue]"
        }

        val fullSystemMessage = PromptMessage(role = "system", content = systemContent)

        // Trim History
        val validHistory = trimHistoryToFitContext(
            fullSystemMessage,
            messages,
            character.postHistoryInstructions,
            contextSizeLimit
        )

        // 8. Author's Note Injection
        val finalHistory = injectAuthorsNote(validHistory, authorsNote, authorsNoteDepth)

        // Final Assembly
        val finalMessages = mutableListOf<PromptMessage>()
        finalMessages.add(fullSystemMessage)
        finalMessages.addAll(finalHistory)

        // 9. Post-History Instructions
        if (!character.postHistoryInstructions.isNullOrBlank()) {
             // Add a system message at the very end
             finalMessages.add(PromptMessage(role = "system", content = character.postHistoryInstructions))
        }

        return finalMessages
    }

    private fun replaceMacros(text: String, charName: String, userName: String): String {
        return text.replace("{{char}}", charName, ignoreCase = true)
                   .replace("{{user}}", userName, ignoreCase = true)
    }

    private fun trimHistoryToFitContext(
        systemMessage: PromptMessage,
        messages: List<ChatMessageEntity>,
        postHistory: String?,
        limit: Int
    ): List<PromptMessage> {
        val staticContent = systemMessage.content + (postHistory ?: "")
        var availableTokens = limit - tokenCounter.estimateTokens(staticContent)

        // Always reserve some space for generation (e.g. 200 tokens)
        availableTokens -= 200

        val selectedMessages = mutableListOf<PromptMessage>()

        // Go from newest to oldest
        for (i in messages.indices.reversed()) {
            val msg = messages[i]
            val formattedContent = msg.content
            val msgTokens = tokenCounter.estimateTokens(formattedContent)

            // We always want to keep at least the last 4 exchanges (if possible)
            if (msgTokens <= availableTokens || selectedMessages.size < 4) {
                selectedMessages.add(0, PromptMessage(role = msg.role, content = formattedContent))
                availableTokens -= msgTokens
            } else {
                break
            }
        }

        return selectedMessages
    }

    private fun injectAuthorsNote(history: List<PromptMessage>, authorsNote: String?, depth: Int): List<PromptMessage> {
        if (authorsNote.isNullOrBlank() || history.isEmpty()) return history

        val mutableHistory = history.toMutableList()
        // Depth 0 means very end, depth 1 means before the last message, etc.
        val index = (mutableHistory.size - depth).coerceIn(0, mutableHistory.size)

        val noteMessage = PromptMessage(role = "system", content = "[Author's Note: $authorsNote]")
        mutableHistory.add(index, noteMessage)

        return mutableHistory
    }
}
