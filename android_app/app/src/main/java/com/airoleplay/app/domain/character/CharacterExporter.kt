package com.airoleplay.app.domain.character

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import com.airoleplay.app.utils.PngChunkWriter
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class CharacterExporter @Inject constructor(
    private val context: Context,
    private val characterDao: CharacterDao,
    private val settingsDao: SettingsDao,
    private val gson: Gson
) {

    suspend fun exportTavernCard(characterId: Long): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val character = characterDao.getCharacterById(characterId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Character not found"))

            val lorebookEntries = settingsDao.getLorebookEntriesForCharacter(characterId).firstOrNull() ?: emptyList()

            // Construct V2 Data
            val v2Data = TavernCardV2Data(
                name = character.name,
                description = character.description,
                personality = character.personalitySummary,
                scenario = character.scenario,
                first_mes = character.firstMessage,
                mes_example = character.exampleDialogue,
                system_prompt = character.systemPromptOverride ?: "",
                post_history_instructions = character.postHistoryInstructions ?: "",
                tags = character.tags.split(",").filter { it.isNotBlank() },
                character_book = if (lorebookEntries.isNotEmpty()) buildCharacterBook(character.name, lorebookEntries) else null
            )

            val v2Card = TavernCardV2(data = v2Data)
            val jsonStr = gson.toJson(v2Card)
            val base64Str = Base64.encodeToString(jsonStr.toByteArray(), Base64.NO_WRAP)

            // Write to PNG
            val sourceAvatarFile = if (character.avatarImagePath != null) File(character.avatarImagePath) else null
            val outputFile = File(context.cacheDir, "exported_${character.name.replace(" ", "_")}.png")

            PngChunkWriter.writeTavernCard(
                sourceAvatarPath = sourceAvatarFile?.absolutePath,
                destinationPath = outputFile.absolutePath,
                charaDataBase64 = base64Str,
                characterName = character.name
            )

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
            Result.success(uri)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun buildCharacterBook(name: String, entries: List<LorebookEntryEntity>): CharacterBook {
        val bookEntries = entries.map {
            CharacterBookEntry(
                name = it.title,
                keys = it.keywords.split(","),
                content = it.content,
                insertion_order = it.insertionOrder,
                enabled = it.isEnabled,
                constant = it.isConstant
            )
        }
        return CharacterBook(
            name = "$name Lorebook",
            entries = bookEntries
        )
    }
}
