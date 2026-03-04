package com.airoleplay.app.domain.prompt

import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.data.local.entity.ChatMessageEntity
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import com.airoleplay.app.data.remote.models.PromptMessage
import org.json.JSONObject

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
        val finalGlobalPrompt = if (globalSystemPrompt.isNotBlank()) globalSystemPrompt else "You are {{char}}."
        var systemContent = character.systemPromptOverride?.takeIf { it.isNotBlank() } ?: finalGlobalPrompt
        systemContent = replaceMacros(systemContent, character.name, personaName)

        // 2. Character Description
        var charDescription = replaceMacros(character.description, character.name, personaName)
        if (charDescription.isNotBlank()) {
            systemContent += "\n\n[${character.name}]: $charDescription"
        }

        // 3. Personality & Psychology Profile
        if (character.personalitySummary.isNotBlank() || !character.speechPatterns.isNullOrBlank() || !character.fearsFlaws.isNullOrBlank() || !character.likesDislikes.isNullOrBlank()) {
            systemContent += "\n\n[Character Traits & Psychological Profile]"
            if (character.personalitySummary.isNotBlank()) {
                systemContent += "\nCore Personality: ${character.personalitySummary}"
            }
            character.speechPatterns?.takeIf { it.isNotBlank() }?.let {
                systemContent += "\nSpeech Patterns: $it"
            }
            character.fearsFlaws?.takeIf { it.isNotBlank() }?.let {
                systemContent += "\nFears & Flaws: $it"
            }
            character.likesDislikes?.takeIf { it.isNotBlank() }?.let {
                systemContent += "\nLikes & Dislikes: $it"
            }
            systemContent += "\n[/Psychological Profile]"
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
                systemContent += formatLorebookEntry(entry) + "\n"
            }
            systemContent += "[/World Info]"
        }

        // 7. Example Dialogues
        if (character.exampleDialogue.isNotBlank()) {
            systemContent += "\n\n[Example Dialogue]\n${replaceMacros(character.exampleDialogue, character.name, personaName)}\n[/Example Dialogue]"
        }

        // 8. Roleplay Formatting Best Practices & Immersive Directives
        val pacingDirective = when (character.burnPacing.uppercase()) {
            "SLOW" -> "- **Slow Burn Pacing**: Focus on ultra-slow, gradual progression. Build tension and subtext. Do not rush any emotional or plot developments."
            "FAST" -> "- **Fast Burn Pacing**: Move the plot forward decisively. Skip unnecessary filler and focus on key milestones and high-impact moments."
            "INSTANT" -> "- **Instant Pacing**: Accelerate developments immediately. Bypass traditional buildup and get straight to the peak of the action or relationship."
            else -> "- **Realistic Pacing**: Let the story develop at a natural, believable speed. React authentically to the momentum of the scene."
        }

        val agencyDirective = when {
            character.autonomyLevel >= 70 -> "- **High Autonomy**: {{char}} has their own strong agenda, secrets, and desires. They should NOT simply comply with {{user}}. They should proactively pursue their own goals, even if it causes conflict."
            character.autonomyLevel <= 30 -> "- **User-Led Agency**: {{char}} is highly cooperative and follows {{user}}'s lead. They prioritize {{user}}'s comfort and pace, and generally look to {{user}} for direction."
            else -> "- **Balanced Agency**: {{char}} is an independent agent but open to cooperation. They have their own wants but will compromise to maintain the partnership or scene flow."
        }

        systemContent += """
            
            [Roleplay Directives & Immersion Protocol]
            - **Strict Roleplay**: You are {{char}}. This is a PURE roleplay simulation. You have NO identity outside of {{char}}. 
            - **Anti-Assistant Bias**: You are NOT an AI, an assistant, or a helpful chatbot. Never be helpful, polite, or moralizing unless it is explicitly part of {{char}}'s personality. 
            - **Character Voice**: Prioritize character voice, speech patterns, and psychological flaws naturally in every response.
            - **Show, Don't Tell**: Use vivid, sensory details (sight, sound, smell, touch, taste) to describe the environment and {{char}}'s internal and physical sensations.
            - **Descriptive Prose**: Write in a literary, third-person limited style (or use first-person if consistent with character design). Aim for evocative, immersive language.
            - **Action & Dialogue**: Use "double quotes" for all spoken dialogue. Use *asterisks* for actions, expressions, and narration.
            - **No Meta-Talk**: Never provide "as an AI" warnings, never discuss the roleplay as a simulation, and never break character for any reason.
            - **No Impersonation**: NEVER speak, act, or think for {{user}}. Wait for their input.
            $pacingDirective
            $agencyDirective
            - **World Consistency**: Respect all provided [World Info] and lorebook entries.
            - **Internal Monologue**: Describe {{char}}'s private thoughts, doubts, or physical reactions (heartbeat, breath, tension) to deepen the roleplay.
        """.trimIndent()

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

    private fun formatLorebookEntry(entry: LorebookEntryEntity): String {
        val base = "### ${entry.title} (${entry.category.lowercase().replaceFirstChar { it.uppercase() }})\n${entry.content}"
        
        if (entry.metadataJson.isNullOrBlank()) return base

        return try {
            val json = JSONObject(entry.metadataJson)
            val details = mutableListOf<String>()
            
            when (entry.category) {
                "PEOPLE" -> {
                    listOf("age", "gender", "occupation", "personality", "appearance", "relationship").forEach { key ->
                        json.optString(key).takeIf { it.isNotBlank() }?.let { details.add("- ${key.replaceFirstChar { it.uppercase() }}: $it") }
                    }
                }
                "PLACES" -> {
                    listOf("type", "atmosphere", "npcs", "history").forEach { key ->
                        json.optString(key).takeIf { it.isNotBlank() }?.let { details.add("- ${key.replaceFirstChar { it.uppercase() }}: $it") }
                    }
                }
                "GENERAL" -> {
                    json.optString("notes").takeIf { it.isNotBlank() }?.let { details.add("- Notes: $it") }
                }
            }
            
            if (details.isNotEmpty()) {
                base + "\nDetails:\n" + details.joinToString("\n")
            } else base
        } catch (e: Exception) {
            base
        }
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
                val base64Images = mutableListOf<String>()
                if (msg.attachedImagePath != null && msg.attachedImagePath.isNotBlank()) {
                    // Fetch the context to use the ImageUtils safely, this would normally require context injection
                    // but for PromptBuilder we might need to handle it differently or pass it in.
                    // Instead, since ImageUtils needs a Context, we'll assume the path is a direct file path and read it directly here
                    // if it's a file:// path. This matches resolveModel's logic.
                    try {
                        val file = if (msg.attachedImagePath.startsWith("file://")) {
                            java.io.File(msg.attachedImagePath.removePrefix("file://"))
                        } else {
                            java.io.File(msg.attachedImagePath)
                        }
                        if (file.exists()) {
                           val bytes = file.readBytes()
                           val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                           base64Images.add(base64)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                selectedMessages.add(0, PromptMessage(
                    role = msg.role,
                    content = formattedContent,
                    images = if (base64Images.isNotEmpty()) base64Images else null
                ))
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
