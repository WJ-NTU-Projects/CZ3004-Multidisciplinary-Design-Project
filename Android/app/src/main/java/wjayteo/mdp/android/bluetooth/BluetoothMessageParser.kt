package wjayteo.mdp.android.bluetooth

import android.util.Log
import wjayteo.mdp.android.App
import wjayteo.mdp.android.App.Companion.COMMAND_DIVIDER
import wjayteo.mdp.android.App.Companion.COMMAND_PREFIX
import wjayteo.mdp.android.App.Companion.DESCRIPTOR_DIVIDER
import wjayteo.mdp.android.App.Companion.GRID_IDENTIFIER
import wjayteo.mdp.android.App.Companion.ROBOT_POSITION_IDENTIFIER
import wjayteo.mdp.android.App.Companion.ROBOT_STATUS_IDENTIFIER
import wjayteo.mdp.android.App.Companion.SET_IMAGE_IDENTIFIER
import wjayteo.mdp.android.App.Companion.USING_AMD
import wjayteo.mdp.android.arena.ArenaMap

class BluetoothMessageParser(private val callback: (status: MessageStatus, message: String) -> Unit) {
    enum class MessageStatus {
        ROBOT_STATUS,
        ROBOT_POSITION,
        IMAGE_POSITION,
        ARENA,
        GARBAGE,
        RUN_ENDED,
        INFO
    }

    private var previousMessage: String = ""

    fun parse(message: String) {
        callback(MessageStatus.GARBAGE, message)

        if (message == "exe" || message == "fe") {
            callback(MessageStatus.RUN_ENDED, message)
            return
        }

        if (!message.contains(COMMAND_DIVIDER) || !message.contains(COMMAND_PREFIX)) return

        val s: ArrayList<String> = ArrayList(message.split(COMMAND_DIVIDER))

        if (s.size != 2) {
            callback(MessageStatus.GARBAGE, "Something went wrong.")
            return
        }

        if (s[0] == "${COMMAND_PREFIX}${SET_IMAGE_IDENTIFIER}") {
            callback(MessageStatus.IMAGE_POSITION, s[1])
            return
        }

        // Integration use
        if ((App.AUTO_UPDATE_ARENA || ArenaMap.isWaitingUpdate) && s[0] == "${COMMAND_PREFIX}r") {
            ArenaMap.isWaitingUpdate = false;

            val strings = s[1].split(",")

            if (strings.size != 5) {
                callback(MessageStatus.INFO, "Something went wrong.")
                return
            }

            val exploreDescriptor: String = strings[0]
            val obstacleDescriptor: String = strings[1]
            val s2 = "${exploreDescriptor}${DESCRIPTOR_DIVIDER}${obstacleDescriptor}"
            callback(MessageStatus.ARENA, s2)


            try {
                val x = strings[2].trim().toInt()
                val y = strings[3].trim().toInt()
                val r = strings[4].trim().toInt()
                s[1] = "$x, $y, $r"
                callback(MessageStatus.ROBOT_POSITION, s[1])
            }  catch (e: NumberFormatException) {
                Log.e(this::class.simpleName ?: "-", "Something went wrong.", e)
                callback(MessageStatus.INFO, "Something went wrong.")
                return
            }

            return
        }

        if ((App.AUTO_UPDATE_ARENA || ArenaMap.isWaitingUpdate) && s[0] == "${COMMAND_PREFIX}${GRID_IDENTIFIER}") {
            ArenaMap.isWaitingUpdate = false
            val strings = s[1].split(",")

            if (strings.size != 2) {
                callback(MessageStatus.INFO, "Something went wrong.")
                return
            }

            val exploreDescriptor: String = strings[0]
            val obstacleDescriptor: String = strings[1]
            val s2 = "${exploreDescriptor}${DESCRIPTOR_DIVIDER}${obstacleDescriptor}"
            callback(MessageStatus.ARENA, s2)

//            if (usingAmd) {
//                var s2: String = "f".padEnd(75, 'f')
//                s2 = "${s2}${DESCRIPTOR_DIVIDER}${s[1]}"
//                Log.e("TEST", s[1])
//                Log.e("TEST", s2)
//                callback(MessageStatus.ARENA, s2)
//            } else {
//                callback(MessageStatus.ARENA, s[1])
//            }

            return
        }

        if (s[0] == "${COMMAND_PREFIX}${ROBOT_POSITION_IDENTIFIER}") {
            Log.e("MESSAGE3", message)
            //if (s[1] == previousMessage) return
            previousMessage = s[1]

            val s1 = s[1].split(",")

            if (s1.size != 3) {
                callback(MessageStatus.INFO, "Something went wrong.")
                return
            }

            try {
                val x = if (USING_AMD) s1[0].toInt() + 1 else s1[0].toInt()
                val y = if (USING_AMD) s1[1].toInt() - 1 else s1[1].toInt()
                val r = s1[2].toInt()
                s[1] = "$x, $y, $r"
            }  catch (e: NumberFormatException) {
                Log.e(this::class.simpleName ?: "-", "Something went wrong.", e)
                callback(MessageStatus.INFO, "Something went wrong.")
                return
            }
        }

        when (s[0]) {
            "${COMMAND_PREFIX}${ROBOT_POSITION_IDENTIFIER}" -> callback(MessageStatus.ROBOT_POSITION, s[1])
            "${COMMAND_PREFIX}${ROBOT_STATUS_IDENTIFIER}" -> callback(MessageStatus.ROBOT_STATUS, s[1])
            //"${COMMAND_PREFIX}${SET_IMAGE_IDENTIFIER}" -> callback(MessageStatus.IMAGE_POSITION, s[1])
            //else -> callback(MessageStatus.GARBAGE, s[1])
        }
    }
}