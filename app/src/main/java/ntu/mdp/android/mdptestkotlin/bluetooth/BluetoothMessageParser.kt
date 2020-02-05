package ntu.mdp.android.mdptestkotlin.bluetooth

import android.util.Log
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.usingAmd
import ntu.mdp.android.mdptestkotlin.arena.ArenaV2

class BluetoothMessageParser(private val callback: (status: MessageStatus, message: String) -> Unit) {
    enum class MessageStatus {
        ROBOT_STATUS,
        ROBOT_POSITION,
        IMAGE_POSITION,
        ARENA,
        GARBAGE,
        INFO
    }

    fun parse(message: String) {
        if (!message.contains("::") || !message.contains("#")) {
            callback(MessageStatus.GARBAGE, message)
            return
        }

        val s: ArrayList<String> = ArrayList(message.split("::"))

        if (s.size != 2) {
            callback(MessageStatus.GARBAGE, "Something went wrong.")
            return
        }

        if ((App.autoUpdateArena || ArenaV2.isWaitingUpdate) && s[0] == "#grid") {
            ArenaV2.isWaitingUpdate = false

            if (usingAmd) {
                var s2: String = "f".padEnd(75, 'f')
                s2 = "$s2//${s[1]}"
                Log.e("TEST", s[1])
                Log.e("TEST", s2)
                callback(MessageStatus.ARENA, s2)
            } else {
                callback(MessageStatus.ARENA, s[1])
            }

            return
        }

        if (s[0] == "#robotposition") {
            val s1 = s[1].split(", ")

            if (s1.size != 3) {
                callback(MessageStatus.INFO, "Something went wrong.")
                return
            }

            try {
                val x = s1[0].toInt() + 1
                val y = s1[1].toInt() - 1
                val r = s1[2].toInt()
                s[1] = "$x, $y, $r"
            }  catch (e: NumberFormatException) {
                Log.e(this::class.simpleName ?: "-", "Something went wrong.", e)
                callback(MessageStatus.INFO, "Something went wrong.")
                return
            }
        }

        when (s[0]) {
            "#grid" -> return
            "#robotposition" -> callback(MessageStatus.ROBOT_POSITION, s[1])
            "#robotstatus" -> callback(MessageStatus.ROBOT_STATUS, s[1])
            "#imageposition" -> callback(MessageStatus.IMAGE_POSITION, s[1])
            "#waypoint" -> return
            else -> callback(MessageStatus.GARBAGE, s[1])
        }
    }
}