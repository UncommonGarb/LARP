package com.airoleplay.app.domain.prompt

import com.airoleplay.app.data.local.entity.LorebookEntryEntity

object LorebookScanner {
    fun scan(recentText: String, entries: List<LorebookEntryEntity>): List<LorebookEntryEntity> {
        val triggeredEntries = mutableListOf<LorebookEntryEntity>()

        entries.filter { it.isEnabled }.forEach { entry ->
            if (entry.isConstant) {
                triggeredEntries.add(entry)
            } else {
                val keywords = entry.keywords.split(",").map { it.trim().lowercase() }
                if (keywords.any { keyword -> recentText.lowercase().contains(keyword) }) {
                    triggeredEntries.add(entry)
                }
            }
        }

        // Sort by insertion order ascending
        triggeredEntries.sortBy { it.insertionOrder }
        return triggeredEntries
    }
}
