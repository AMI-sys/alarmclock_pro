package com.example.dindon.data

import android.content.Context
import com.example.dindon.model.Alarm
import com.example.dindon.model.WeekDay
import org.json.JSONArray
import org.json.JSONObject

class AlarmStore(context: Context) {

    private val prefs = context.getSharedPreferences("alarms_prefs", Context.MODE_PRIVATE)

    fun getAlarms(): List<Alarm> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.getJSONObject(idx)
                fromJson(o)
            }
        }.getOrDefault(emptyList())
    }

    fun setAlarms(alarms: List<Alarm>) {
        val arr = JSONArray()
        alarms.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun toJson(a: Alarm): JSONObject {
        val o = JSONObject()
        o.put("id", a.id)
        o.put("hour", a.hour)
        o.put("minute", a.minute)
        o.put("label", a.label)
        o.put("groupName", a.groupName)
        o.put("enabled", a.enabled)
        o.put("sound", a.sound)
        o.put("snoozeMinutes", a.snoozeMinutes)
        o.put("vibrate", a.vibrate)

        val days = JSONArray()
        a.days.forEach { days.put(it.name) }
        o.put("days", days)

        return o
    }

    private fun fromJson(o: JSONObject): Alarm {
        val daysArr = o.optJSONArray("days") ?: JSONArray()
        val days = (0 until daysArr.length()).mapNotNull { idx ->
            runCatching { WeekDay.valueOf(daysArr.getString(idx)) }.getOrNull()
        }.toSet()

        return Alarm(
            id = o.getInt("id"),
            hour = o.getInt("hour"),
            minute = o.getInt("minute"),
            label = o.optString("label", "Alarm"),
            groupName = o.optString("groupName", "Default"),
            enabled = o.optBoolean("enabled", true),
            days = days,
            sound = o.optString("sound", "default"),
            snoozeMinutes = o.optInt("snoozeMinutes", 10),
            vibrate = o.optBoolean("vibrate", true)
        )
    }

    companion object {
        private const val KEY = "alarms_json"
    }
}
