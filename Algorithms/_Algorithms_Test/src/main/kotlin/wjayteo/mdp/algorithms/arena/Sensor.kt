package wjayteo.mdp.algorithms.arena

import tornadofx.multi

class Sensor {
    companion object {

        private fun updateMap(x: Int, y: Int, reading: Int, reliability: Int, hasObstacle: Boolean, offsetX: Boolean, positive: Boolean, lastSensor: Boolean = false) {
            val multiplier = if (positive) 1 else -1

            for (i in 1 until reading) {
                if (offsetX) {
                    //Arena.setExplored(x + (i * multiplier), y)
                    Arena.setGridState(x + (i * multiplier), y, -reliability)
                } else {
                    //Arena.setExplored(x, y + (i * multiplier))
                    Arena.setGridState(x, y + (i * multiplier), -reliability)
                }
            }

            if (hasObstacle) {
                if (offsetX) Arena.setGridState(x + (reading * multiplier), y, reliability)
                else Arena.setGridState(x, y + (reading * multiplier), reliability)
            }

//            else {
//                if (!lastSensor) {
//                    //if (offsetX) Arena.setGridState(x + ((reading - 1) * multiplier), y, -reliability)
//                    //else Arena.setGridState(x, y + ((reading - 1) * multiplier), -reliability)
//                    return
//                }
//
//                if (reading > 5) return
//                if (offsetX) Arena.setGridState(x + (reading * multiplier), y, -reliability)
//                else Arena.setGridState(x, y + (reading * multiplier), -reliability)
//            }
        }

        fun updateArenaSensor1(x1: Int, y1: Int, facing: Int, r: Int) {
            var x: Int = x1
            var y: Int = y1
            if (r <= 0) return

            when (facing) {
                0   -> {
                    x += 1
                    y += 1
                    updateMap(x, y, r, 3, r == 1, offsetX = false, positive = true)
                }

                90   -> {
                    x += 1
                    y -= 1
                    updateMap(x, y, r, 3, r == 1, offsetX = true, positive = true)
                }

                180   -> {
                    x -= 1
                    y -= 1
                    updateMap(x, y, r, 3, r == 1, offsetX = false, positive = false)
                }

                270   -> {
                    x -= 1
                    y += 1
                    updateMap(x, y, r, 3, r == 1, offsetX = true, positive = false)
                }
            }
        }

        fun updateArenaSensor2(x1: Int, y1: Int, facing: Int, r: Int) {
            var x: Int = x1
            var y: Int = y1
            if (r <= 0) return

            when (facing) {
                0   -> {
                    y += 1
                    updateMap(x, y, r, 3, r == 1, offsetX = false, positive = true)
                }

                90   -> {
                    x += 1
                    updateMap(x, y, r, 3, r == 1, offsetX = true, positive = true)
                }

                180   -> {
                    y -= 1
                    updateMap(x, y, r, 3, r == 1, offsetX = false, positive = false)
                }

                270   -> {
                    x -= 1
                    updateMap(x, y, r, 3, r == 1, offsetX = true, positive = false)
                }
            }
        }

        fun updateArenaSensor3(x1: Int, y1: Int, facing: Int, r: Int) {
            var x: Int = x1
            var y: Int = y1
            if (r <= 0) return

            when (facing) {
                0   -> {
                    x -= 1
                    y += 1
                    updateMap(x, y, r, 3, r == 1, offsetX = false, positive = true)
                }

                90   -> {
                    x += 1
                    y += 1
                    updateMap(x, y, r, 3, r == 1, offsetX = true, positive = true)
                }

                180   -> {
                    x += 1
                    y -= 1
                    updateMap(x, y, r, 3, r == 1, offsetX = false, positive = false)
                }

                270   -> {
                    x -= 1
                    y -= 1
                    updateMap(x, y, r, 3, r == 1, offsetX = true, positive = false)
                }
            }
        }

        fun updateArenaSensor4(x1: Int, y1: Int, facing: Int, r: Int) {
            var x: Int = x1
            var y: Int = y1
            if (r <= 0) return

            when (facing) {
                0 -> {
                    x -= 1
                    y += 1
                    updateMap(x, y, r, 3, r == 1, offsetX = true, positive = false)
                }

                90 -> {
                    x += 1
                    y += 1
                    updateMap(x, y, r, 3, r == 1, offsetX = false, positive = true)
                }

                180 -> {
                    x += 1
                    y -= 1
                    updateMap(x, y, r, 3, r == 1, offsetX = true, positive = true)
                }

                270 -> {
                    x -= 1
                    y -= 1
                    updateMap(x, y, r, 3, r == 1, offsetX = false, positive = false)
                }
            }
        }

        fun updateArenaSensor5(x1: Int, y1: Int, facing: Int, r: Int) {
            var x: Int = x1
            var y: Int = y1
            if (r <= 0) return

            when (facing) {
                0 -> {
                    x -= 1
                    y -= 1
                    updateMap(x, y, r, 3, r == 1, offsetX = true, positive = false)
                }

                90 -> {
                    x -= 1
                    y += 1
                    updateMap(x, y, r, 3, r == 1, offsetX = false, positive = true)
                }

                180 -> {
                    x += 1
                    y += 1
                    updateMap(x, y, r, 3, r == 1, offsetX = true, positive = true)
                }

                270 -> {
                    x += 1
                    y -= 1
                    updateMap(x, y, r, 3, r == 1, offsetX = false, positive = false)
                }
            }
        }

        fun updateArenaSensor6(x1: Int, y1: Int, facing: Int, r: Int) {
            var x: Int = x1
            var y: Int = y1
            if (r <= 0) return

            when (facing) {
                0 -> {
                    x += 1
                    updateMap(x, y, r, 1, r <= 5, offsetX = true, positive = true, lastSensor = true)
                }

                90 -> {
                    y -= 1
                    updateMap(x, y, r, 1, r <= 5, offsetX = false, positive = false, lastSensor = true)
                }

                180 -> {
                    x -= 1
                    updateMap(x, y, r, 1, r <= 5, offsetX = true, positive = false, lastSensor = true)
                }

                270 -> {
                    y += 1
                    updateMap(x, y, r, 1, r <= 5, offsetX = false, positive = true, lastSensor = true)
                }
            }
        }

//        fun updateArenaSensor1(x1: Int, y1: Int, facing: Int, reading: Int) {
//            var x: Int = x1
//            var y: Int = y1
//            val r: Int = reading.coerceIn(0, 2)
//            if (r <= 0) return
//
//            when (facing) {
//                0   -> {
//                    x += 1
//                    y += 1
//                    if (r == 1) Arena.setObstacle(x, y + r)
//                    else for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
//                }
//
//                90   -> {
//                    x += 1
//                    y -= 1
//                    if (r == 1) Arena.setObstacle(x + r, y)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
//                    }
//                }
//
//                180   -> {
//                    x -= 1
//                    y -= 1
//                    if (r == 1) Arena.setObstacle(x, y - r)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y - offset)
//                    }
//                }
//
//                270   -> {
//                    x -= 1
//                    y += 1
//                    if (r == 1) Arena.setObstacle(x - r, y)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
//                    }
//                }
//            }
//        }
//
//        fun updateArenaSensor2(x1: Int, y1: Int, facing: Int, reading: Int) {
//            var x: Int = x1
//            var y: Int = y1
//            val r: Int = reading.coerceIn(0, 2)
//            if (r <= 0) return
//
//            when (facing) {
//                0   -> {
//                    y += 1
//                    if (r == 1) Arena.setObstacle(x, y + r)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
//                    }
//                }
//
//                90   -> {
//                    x += 1
//                    if (r == 1) Arena.setObstacle(x + r, y)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
//                    }
//                }
//
//                180   -> {
//                    y -= 1
//                    if (r == 1) Arena.setObstacle(x, y - r)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y - offset)
//                    }
//                }
//
//                270   -> {
//                    x -= 1
//                    if (r == 1) Arena.setObstacle(x - r, y)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
//                    }
//                }
//            }
//        }
//
//        fun updateArenaSensor3(x1: Int, y1: Int, facing: Int, reading: Int) {
//            var x: Int = x1
//            var y: Int = y1
//            val r: Int = reading.coerceIn(0, 2)
//            if (r <= 0) return
//
//            when (facing) {
//                0   -> {
//                    x -= 1
//                    y += 1
//                    if (r == 1) Arena.setObstacle(x, y + r)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
//                    }
//                }
//
//                90   -> {
//                    x += 1
//                    y += 1
//                    if (r == 1) Arena.setObstacle(x + r, y)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
//                    }
//                }
//
//                180   -> {
//                    x += 1
//                    y -= 1
//                    if (r == 1) Arena.setObstacle(x, y - r)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y - offset)
//                    }
//                }
//
//                270   -> {
//                    x -= 1
//                    y -= 1
//                    if (r == 1) Arena.setObstacle(x - r, y)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
//                    }
//                }
//            }
//        }
//
//        fun updateArenaSensor4(x1: Int, y1: Int, facing: Int, reading: Int) {
//            var x: Int = x1
//            var y: Int = y1
//            val r: Int = reading.coerceIn(0, 2)
//            if (r <= 0) return
//
//            when (facing) {
//                0 -> {
//                    x -= 1
//                    y += 1
//                    if (r == 1) Arena.setObstacle(x - r, y)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
//                    }
//                }
//
//                90 -> {
//                    x += 1
//                    y += 1
//                    if (r == 1) Arena.setObstacle(x, y + r)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
//                    }
//                }
//
//                180 -> {
//                    x += 1
//                    y -= 1
//                    if (r == 1) Arena.setObstacle(x + r, y)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
//                    }
//                }
//
//                270 -> {
//                    x -= 1
//                    y -= 1
//                    if (r == 1) Arena.setObstacle(x, y - r)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y - offset)
//                    }
//                }
//            }
//        }
//
//        fun updateArenaSensor5(x1: Int, y1: Int, facing: Int, reading: Int) {
//            var x: Int = x1
//            var y: Int = y1
//            val r: Int = reading.coerceIn(0, 2)
//            if (r <= 0) return
//
//            when (facing) {
//                0 -> {
//                    x -= 1
//                    y -= 1
//                    if (r == 1) Arena.setObstacle(x - r, y)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
//                    }
//                }
//
//                90 -> {
//                    x -= 1
//                    y += 1
//                    if (r == 1) Arena.setObstacle(x, y + r)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
//                    }
//                }
//
//                180 -> {
//                    x += 1
//                    y += 1
//                    if (r == 1) Arena.setObstacle(x + r, y)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
//                    }
//                }
//
//                270 -> {
//                    x += 1
//                    y -= 1
//                    if (r == 1) Arena.setObstacle(x, y - r)
//                    else {
//                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y - offset)
//                    }
//                }
//            }
//        }
//
//        fun updateArenaSensor6(x1: Int, y1: Int, facing: Int, reading: Int) {
//            var x: Int = x1
//            var y: Int = y1
//            val r: Int = reading.coerceIn(0, 6)
//            if (r <= 0) return
//
//            when (facing) {
//                0 -> {
//                    x += 1
//                    for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
//
//                    if (r <= 5) {
//                        for (offset in r - 1 downTo 1) if (Arena.isObstacle(x + offset, y)) return
//                        Arena.setObstacle(x + r, y)
//                    }
//                }
//
//                90 -> {
//                    y -= 1
//                    for (offset in 1 until r) Arena.setExplored(x, y - offset)
//
//                    if (r <= 5) {
//                        for (offset in 1 until r) if (Arena.isObstacle(x, y - offset)) return
//                        Arena.setObstacle(x, y - r)
//                    }
//                }
//
//                180 -> {
//                    x -= 1
//                    for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
//
//                    if (r <= 5) {
//                        for (offset in r - 1 downTo 1) if (Arena.isObstacle(x - offset, y)) return
//                        Arena.setObstacle(x - r, y)
//                    }
//                }
//
//                270 -> {
//                    y += 1
//                    for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
//
//                    if (r <= 5) {
//                        for (offset in r - 1 downTo 1) if (Arena.isObstacle(x, y + offset)) return
//                        Arena.setObstacle(x, y + r)
//                    }
//                }
//            }
//        }
    }
}
