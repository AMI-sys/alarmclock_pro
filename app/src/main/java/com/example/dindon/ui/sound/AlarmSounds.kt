package com.example.dindon.ui.sound

import com.example.dindon.R

object AlarmSounds {
    data class Sound(val id: String, val title: String, val resId: Int? = null)

    private val builtIn = listOf(
        Sound("china", "China", R.raw.china),
        Sound("american", "American", R.raw.american),
        Sound("iphone_apex", "iPhone Apex", R.raw.iphone_apex)
    )

    private val customSounds = mutableMapOf<String, String>()

    val all: List<Sound>
        get() = builtIn + customSounds.map { Sound(it.key, it.value, null) }

    fun byId(id: String): Sound? {
        return builtIn.find { it.id == id } ?: customSounds[id]?.let { Sound(id, it, null) }
    }

    fun registerCustomSound(id: String, title: String) {
        customSounds[id] = title
    }
}


