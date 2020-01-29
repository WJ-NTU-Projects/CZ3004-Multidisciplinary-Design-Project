package ntu.mdp.android.mdptestkotlin.utils

import android.content.Context
import android.util.Log
import ntu.mdp.android.mdptestkotlin.MainActivityController
import kotlin.math.pow

/**
 *
 * WELCOME TO THE SCRATCHPAD.
 *
 * THIS CLASS IS USED FOR TESTING WEIRD AND STUPID IDEAS AND EXPERIMENTAL STUFF.
 *
 */

class ScratchPad(private val context: Context, private val activityController: MainActivityController) {
    fun testRun() {
        //var currentPosition = activityController.arenaController.getRobotCurrentPosition()
    }

    fun testRun2() {
        /*
        val arenaStateArray = activityController.arenaController.getArenaState()
        var stringBuilder = StringBuilder()
        arenaStateArray.forEach { it ->
            it.forEach {
                var s = it
                if (s > 1) s = 1
                stringBuilder.append(s)
            }
        }

        activityController.displayInChat(MainActivityController.MessageType.SYSTEM, stringBuilder.toString())
        val s: CharArray = stringBuilder.toString().toCharArray()
        s[50] = '1'
        s[100] = '1'
        s[211] = '1'
        val finalString: String = s.joinToString("")

        var counter = 0
        var sum = 0
        stringBuilder = StringBuilder()

        for (i in finalString.length - 1 downTo 0) {
            val bit: Int = finalString[i].toString().toInt()
            sum += (bit * (2.0).pow(counter).toInt())
            counter++

            if (counter == 4) {
                stringBuilder.append(sum.toString(16))
                counter = 0
                sum = 0
            }
        }

        val explore = stringBuilder.toString().reversed()
        activityController.displayInChat(MainActivityController.MessageType.SYSTEM, explore)
        activityController.bluetoothMessageParser.parse("#grid::$explore//003")
        //s = s.toInt(2).toString(16)
        //s = s.replaceRange(30, 30, "1")
        //s = s.toInt(2).toString(16)
        //activityController.bluetoothMessageParser.parse(s)

         */
    }
}