package ntu.mdp.android.mdptestkotlin.main

import android.content.Context
import android.util.Log
import ntu.mdp.android.mdptestkotlin.App

class MessageParser(private val context: Context, private val callback: (status: Status, message: String) -> Unit) {
    enum class Status {
        ROBOT_STATUS,
        ROBOT_POSITION,
        IMAGE_POSITION,
        ARENA,
        GARBAGE,
        INFO
    }

    fun parse(message: String) {
        if (!message.contains("::") || !message.contains("#")) {
            callback(Status.GARBAGE, message)
            return
        }

        val s: ArrayList<String> = ArrayList(message.split("::"))
        Log.e("MESSAGE", message)

        if (s.size != 2) {
            callback(Status.GARBAGE, "Something went wrong.")
            return
        }

        if ((App.autoUpdateArena || MainActivity.isUpdating) && s[0] == "#grid") {
            MainActivity.isUpdating = false
            callback(Status.ARENA, s[1])
            return
        }

        if (s[0] == "#robotposition") {
            val s1 = s[1].split(", ")

            if (s1.size != 3) {
                callback(Status.INFO, "Something went wrong.")
                return
            }

            try {
                val x = s1[0].toInt() + 1
                val y = s1[1].toInt() - 1
                val r = s1[2].toInt()
                s[1] = "$x, $y, $r"
            }  catch (e: NumberFormatException) {
                Log.e(this::class.simpleName ?: "-", "Something went wrong.", e)
                callback(Status.INFO, "Something went wrong.")
                return
            }
        }

        when (s[0]) {
            "#grid" -> return
            "#robotposition" -> callback(Status.ROBOT_POSITION, s[1])
            "#robotstatus" -> callback(Status.ROBOT_STATUS, s[1])
            "#imageposition" -> callback(Status.IMAGE_POSITION, s[1])
            "#waypoint" -> return
            else -> callback(Status.GARBAGE, s[1])
        }
    }
}