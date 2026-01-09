package com.example.dindon.ui.sound

import com.example.dindon.R

object AlarmSounds {

    data class Sound(
        val id: String,
        val title: String,
        val resId: Int
    )

    val all = listOf(
        Sound("american", "American", R.raw.american),
        Sound("china", "China", R.raw.china),
        Sound("apex", "iPhone Apex", R.raw.iphone_apex)
    )

    fun byId(id: String): Sound =
        all.firstOrNull { it.id == id } ?: all.first()
}
