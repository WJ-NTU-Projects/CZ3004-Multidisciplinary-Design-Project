package wjayteo.mdp.android.arena

import android.util.Log
import wjayteo.mdp.android.App.Companion.DESCRIPTOR_DIVIDER
import wjayteo.mdp.android.App.Companion.simulationMode
import wjayteo.mdp.android.App.Companion.usingAmd

class MapDescriptor {
    companion object {
        fun fromString(descriptor: String, exploredBit: Int): ArrayList<Array<Array<Int>>> {
            var counter = 0
            var s: ArrayList<String> = arrayListOf(descriptor)
            val exploreArray    : Array<Array<Int>> = Array(20) { Array(15) { 0 }}
            val obstacleArray    : Array<Array<Int>> = Array(20) { Array(15) { 0 }}
            val exploredList    : ArrayList<IntArray> = arrayListOf()

            if (descriptor.contains(DESCRIPTOR_DIVIDER)) {
                s = ArrayList(descriptor.split(DESCRIPTOR_DIVIDER))
            }

            var skip = 2

            for (i in s[0].indices) {
                var binary: String = s[0][i].toString().toInt(16).toString(2)
                binary = binary.padStart(4, '0')

                for (j in binary.indices) {
                    if (!usingAmd && skip > 0) {
                        skip--
                        continue
                    }

                    if (!usingAmd && i == s[0].length - 1 && j >= 2) continue

                    val bit: Int = binary[j].toString().toInt()
                    val y = Math.floorDiv(counter, 15)
                    val x = (counter % 15)
                    if (!(x in 0..14 && y in 0..19)) break
                    exploreArray[y][x] = bit
                    if (bit == exploredBit) exploredList.add(intArrayOf(x, y))
                    //Log.e("K", "X = $x, Y = $y, BIT = $bit")
                    counter++
                }
            }

            if (s.size != 2) {
                return arrayListOf(exploreArray, obstacleArray)
            }

            counter = 0

            for (i in s[1].indices) {
                var binary: String = s[1][i].toString().toInt(16).toString(2)
                binary = binary.padStart(4, '0')

                for (j in binary.indices) {
                    if (!simulationMode && counter >= exploredList.size) break
                    val bit: Int = binary[j].toString().toInt()

                    val x = if (simulationMode) Math.floorMod(counter, 15) else exploredList[counter][0]
                    val y = if (simulationMode) Math.floorDiv(counter, 15) else exploredList[counter][1]
                    Log.e("K", "$x, $y")
                    obstacleArray[y][x] = bit
                    counter++
                }
            }

            return arrayListOf(exploreArray, obstacleArray)
        }

        fun fromArray(exploreArray: Array<Array<Int>>, obstacleArray: Array<Array<Int>>, exploredBit: Int): ArrayList<String> {
            var exploreString = "11"
            var obstacleString = ""

            for (y in 0..19) {
                for (x in 0..14) {
                    val exploreBit: Int = exploreArray[y][x]
                    exploreString += "$exploreBit"
                    if (exploreBit != exploredBit) continue

                    val obstacleBit: Int = obstacleArray[y][x]
                    obstacleString += "$obstacleBit"
                }
            }

            exploreString += "11"
            if (exploreString.length % 4 != 0) return arrayListOf()

            var padLength: Int = 4 - (Math.floorMod(obstacleString.length, 4))
            if (padLength == 4) padLength = 0
            obstacleString += "".padEnd(padLength, '0')

            var explorationDescriptor = ""
            var hex = ""
            var counter = 0

            exploreString.indices.forEach { i ->
                hex += exploreString[i]
                counter++

                if (counter == 4) {
                    hex = hex.toInt(2).toString(16)
                    explorationDescriptor += hex
                    hex = ""
                    counter = 0
                }
            }

            var obstacleDescriptor = ""
            hex = ""
            counter = 0

            obstacleString.indices.forEach { i ->
                hex += obstacleString[i]
                counter++

                if (counter == 4) {
                    hex = hex.toInt(2).toString(16)
                    obstacleDescriptor += hex
                    hex = ""
                    counter = 0
                }
            }

            //explorationDescriptor = "f8007000e0000000000000000000000000000000000000000000000000000000000000000003"
            Log.e(this::class.simpleName, "\n")
            Log.e(this::class.simpleName, "Exploration Descriptor: $explorationDescriptor")
            Log.e(this::class.simpleName, "Obstacle Descriptor: $obstacleDescriptor")
            return arrayListOf(explorationDescriptor, obstacleDescriptor)
        }
    }
}