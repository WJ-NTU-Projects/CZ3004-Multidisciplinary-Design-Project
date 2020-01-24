package ntu.mdp.android.mdptestkotlin.main

import android.content.Context
import android.util.Log

class MessageParser(private val context: Context, private val callback: (status: Status, message: String) -> Unit) {
    enum class Status {
        ROBOT_STATUS,
        ROBOT_POSITION,
        IMAGE_POSITION,
        ARENA,
        GARBAGE
    }

    fun parse(message: String) {
        if (!message.contains("::") || !message.contains("#")) {
            callback(Status.GARBAGE, message)
            return
        }

        val s: List<String> = message.split("::")
        Log.e("MESSAGE", message)

        if (s.size != 2) {
            callback(Status.GARBAGE, "Something went wrong.")
            return
        }

        if ((MainActivity.autoUpdate || MainActivity.isUpdating) && s[0] == "#grid") {
            MainActivity.isUpdating = false
            callback(Status.ARENA, s[1])
            return
        }

        when (s[0]) {
            "#grid" -> return
            "#robotposition" -> callback(Status.ROBOT_POSITION, s[1])
            "#robotstatus" -> callback(Status.ROBOT_STATUS, s[1])
            "#imageposition" -> callback(Status.IMAGE_POSITION, s[1])
            else -> callback(Status.GARBAGE, s[1])
        }
    }
}