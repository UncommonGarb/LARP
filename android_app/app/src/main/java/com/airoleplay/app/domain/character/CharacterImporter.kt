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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class CharacterImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val characterDao: CharacterDao,
    private val settingsDao: SettingsDao,
    private val gson: Gson
) {

    suspend fun importCharacter(uri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            
            // Try to get filename to help with format detection
            var fileName = ""
            var mimeType = contentResolver.getType(uri) ?: ""
            
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: ""
                }
            }

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream == null) return@withContext Result.failure(Exception("Could not open file"))

            val bytes = inputStream.readBytes()
            
            // Check for PNG magic bytes
            val isPng = bytes.size >= 8 && 
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && 
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
                
            val jsonString = if (isPng) {
                // It's a PNG Tavern Card
                val charaDataStr = PngChunkReader.readTavernCardData(bytes.inputStream())
                    ?: return@withContext Result.failure(Exception("No 'chara' metadata found in PNG."))
                String(Base64.decode(charaDataStr, Base64.DEFAULT))
            } else {
                // It's likely JSON or YAML text
                val text = String(bytes)
                if (text.trimStart().startsWith("{")) {
                    text // It's JSON
                } else if (fileName.endsWith(".yaml", true) || fileName.endsWith(".yml", true) || mimeType.contains("yaml")) {
                    parseSimpleYamlToJson(text)
                } else {
                    return@withContext Result.failure(Exception("Unsupported file format. Must be PNG, JSON, or YAML."))
                }
            }
            
            // Re-route to the common JSON parser
            importFromJson(context, uri, jsonString, fileName.substringBeforeLast("."))

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun importFromJson(
        context: Context,
        uri: Uri,
        jsonString: String,
        nameHint: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val rootObj = gson.fromJson(jsonString, Map::class.java)
            val isV2 = rootObj.containsKey("spec") && rootObj["spec"] == "chara_card_v2"
            
            val name = if (isV2) {
                val data = rootObj["data"] as? Map<*, *>
                data?.get("name") as? String ?: "Unknown"
            } else {
                rootObj["name"] as? String ?: "Unknown"
            }
            
            // Optional: for JSON/YAML files, the avatar might not be embedded in the file, 
            // so we'll just save the file itself as "avatar" if it was a PNG, otherwise no avatar
            // (Users can edit it later, or we check if user wants to pick an avatar)
            var avatarPath: String? = null
            if (context.contentResolver.getType(uri)?.startsWith("image/") == true || uri.toString().endsWith(".png", true)) {
                avatarPath = saveAvatarToInternalStorage(uri, name)
            }

            val entity: CharacterEntity
            val lorebookEntries = mutableListOf<LorebookEntryEntity>()

            if (isV2) {
                val v2Card = gson.fromJson(jsonString, TavernCardV2::class.java)
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
                    avatarImagePath = avatarPath
                )

                // Process embedded lorebook
                data.character_book?.entries?.forEachIndexed { index, entry ->
                    lorebookEntries.add(
                        LorebookEntryEntity(
                            title = entry.name ?: "Entry ${index + 1}",
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
                    avatarImagePath = avatarPath
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
    
    // A very basic YAML to JSON converter for SillyTavern character profiles
    // Assumes flat key: value or simple nested structures
    private fun parseSimpleYamlToJson(yaml: String): String {
        val map = mutableMapOf<String, Any>()
        val lines = yaml.lines()
        
        var currentKey = ""
        var currentValue = StringBuilder()
        var isMultilineString = false
        var isV2Data = false
        
        val dataMap = mutableMapOf<String, Any>()
        
        for (line in lines) {
            val trimmedLine = line.trimEnd()
            
            if (trimmedLine.startsWith("spec: chara_card_v2")) {
                map["spec"] = "chara_card_v2"
                continue
            }
            if (trimmedLine.trim() == "data:") {
                isV2Data = true
                continue
            }
            
            if (isMultilineString) {
                val indent = line.takeWhile { it.isWhitespace() }.length
                if (indent == 0 && line.isNotEmpty() && !line.startsWith(" ")) {
                    // end of multiline string
                    isMultilineString = false
                    if (isV2Data) {
                        dataMap[currentKey] = currentValue.toString().trim()
                    } else {
                        map[currentKey] = currentValue.toString().trim()
                    }
                } else {
                    currentValue.append(line.trim()).append("\\n")
                    continue
                }
            }
            
            // Extremely basic parser: just look for first colon
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim()
                val valueStr = line.substring(colonIndex + 1).trim()
                
                if (valueStr == "|" || valueStr == "|-") {
                    currentKey = key
                    currentValue = StringBuilder()
                    isMultilineString = true
                } else {
                    // For string arrays like tags:, simple parsing
                    if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                        // Keep as text, let Gson figure it out later if needed
                        if (isV2Data) dataMap[key] = valueStr.removePrefix("[").removeSuffix("]").split(",").map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                        else map[key] = valueStr
                    } else {
                        val finalVal = valueStr.removeSurrounding("\"").removeSurrounding("'")
                        if (isV2Data) dataMap[key] = finalVal
                        else map[key] = finalVal
                    }
                }
            }
        }
        
        // Handle trailing multiline 
        if (isMultilineString) {
             if (isV2Data) {
                dataMap[currentKey] = currentValue.toString().trim()
            } else {
                map[currentKey] = currentValue.toString().trim()
            }
        }
        
        if (isV2Data) {
            map["data"] = dataMap
        }
        
        return gson.toJson(map)
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
