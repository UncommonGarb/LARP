package com.airoleplay.app.domain.character

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import com.airoleplay.app.utils.PngChunkReader
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class CharacterImporter @Inject constructor(
    private val context: Context,
    private val characterDao: CharacterDao,
    private val settingsDao: SettingsDao,
    private val gson: Gson
) {

    suspend fun importTavernCard(uri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)

            if (inputStream == null) return@withContext Result.failure(Exception("Could not open file"))

            val charaDataStr = PngChunkReader.readTavernCardData(inputStream)
                ?: return@withContext Result.failure(Exception("No 'chara' metadata found in PNG."))

            // Decode base64
            val decodedJson = String(Base64.decode(charaDataStr, Base64.DEFAULT))

            // Check V2 vs V1
            val rootObj = gson.fromJson(decodedJson, Map::class.java)
            val isV2 = rootObj.containsKey("spec") && rootObj["spec"] == "chara_card_v2"

            val entity: CharacterEntity
            val lorebookEntries = mutableListOf<LorebookEntryEntity>()

            if (isV2) {
                val v2Card = gson.fromJson(decodedJson, TavernCardV2::class.java)
                val data = v2Card.data

                entity = CharacterEntity(
                    name = data.name,
                    description = data.description,
                    personalitySummary = data.personality,
                    scenario = data.scenario,
                    firstMessage = data.first_mes,
                    exampleDialogue = data.mes_example,
                    systemPromptOverride = data.system_prompt.takeIf { it.isNotBlank() },
                    postHistoryInstructions = data.post_history_instructions.takeIf { it.isNotBlank() },
                    tags = data.tags.joinToString(","),
                    avatarImagePath = saveAvatarToInternalStorage(uri, data.name)
                )

                // Process embedded lorebook
                data.character_book?.entries?.forEachIndexed { index, entry ->
                    lorebookEntries.add(
                        LorebookEntryEntity(
                            title = entry.name ?: "Entry \${index + 1}",
                            keywords = entry.keys.joinToString(","),
                            content = entry.content,
                            insertionOrder = entry.insertion_order,
                            isEnabled = entry.enabled,
                            isConstant = entry.constant
                        )
                    )
                }
            } else {
                // Legacy V1 format
                val name = rootObj["name"] as? String ?: "Unknown"
                val desc = rootObj["description"] as? String ?: ""
                val personality = rootObj["personality"] as? String ?: ""
                val scenario = rootObj["scenario"] as? String ?: ""
                val firstMes = rootObj["first_mes"] as? String ?: ""
                val mesExample = rootObj["mes_example"] as? String ?: ""

                entity = CharacterEntity(
                    name = name,
                    description = desc,
                    personalitySummary = personality,
                    scenario = scenario,
                    firstMessage = firstMes,
                    exampleDialogue = mesExample,
                    tags = "",
                    avatarImagePath = saveAvatarToInternalStorage(uri, name)
                )
            }

            // Save to DB
            val characterId = characterDao.insertCharacter(entity)

            // Save lorebooks
            lorebookEntries.forEach { entry ->
                settingsDao.insertLorebookEntry(entry.copy(characterId = characterId))
            }

            Result.success(characterId)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun saveAvatarToInternalStorage(uri: Uri, characterName: String): String? {
        return try {
            val fileName = "avatar_${System.currentTimeMillis()}_${characterName.replace(" ", "_")}.png"
            val file = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
