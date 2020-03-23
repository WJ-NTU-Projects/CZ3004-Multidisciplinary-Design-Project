package wjayteo.mdp.algorithms.arena

class Sensor {
    companion object {

        fun updateArenaSensor(sensor: Int, x1: Int, y1: Int, facing: Int, reading1: Int) {
            var x: Int = x1
            var y: Int = y1
            val reading: Int = reading1.coerceIn(0, 9)
            if (reading <= 0) return

            if (sensor == 6) {
                updateArenaSensorLong(x, y, facing, reading)
                return
            }

            when (facing) {
                0 -> {
                    when (sensor) {
                        1 -> {
                            x += 1
                            y += 2
                        }

                        2 -> y += 2

                        3 -> {
                            x -= 1
                            y += 2
                        }

                        4 -> {
                            x -= 2
                            y += 1
                        }

                        5 -> {
                            x -= 2
                            y -= 1
                        }
                    }
                }

                90 -> {
                    when (sensor) {
                        1 -> {
                            x += 2
                            y -= 1
                        }

                        2 -> x += 2

                        3 -> {
                            x += 2
                            y += 1
                        }

                        4 -> {
                            x += 1
                            y += 2
                        }

                        5 -> {
                            x -= 1
                            y += 2
                        }
                    }
                }

                180 -> {
                    when (sensor) {
                        1 -> {
                            x -= 1
                            y -= 2
                        }

                        2 -> y -= 2

                        3 -> {
                            x += 1
                            y -= 2
                        }

                        4 -> {
                            x += 2
                            y -= 1
                        }

                        5 -> {
                            x += 2
                            y += 1
                        }
                    }
                }

                270 -> {
                    when (sensor) {
                        1 -> {
                            x -= 2
                            y += 1
                        }

                        2 -> x -= 2

                        3 -> {
                            x -= 2
                            y -= 1
                        }

                        4 -> {
                            x -= 1
                            y -= 2
                        }

                        5 -> {
                            x += 1
                            y -= 2
                        }
                    }
                }
            }

            if (reading == 1) Arena.setObstacle(x, y) else Arena.setExplored(x, y)
        }

        private fun updateArenaSensorLong(x1: Int, y1: Int, facing: Int, reading: Int) {
            var x: Int = x1
            var y: Int = y1
            val readingThreshold = 5

            when (facing) {
                0 -> {
                    x += 1
                    if (reading <= readingThreshold) Arena.setObstacle(x + reading, y)
                    for (offset in reading - 1 downTo 1) Arena.setExplored(x + offset, y)
                }

                90 -> {
                    y -= 1
                    if (reading <= readingThreshold) Arena.setObstacle(x, y - reading)
                    for (offset in 1 until reading) Arena.setExplored(x, y - offset)
                }

                180 -> {
                    x -= 1
                    if (reading <= readingThreshold) Arena.setObstacle(x - reading, y)
                    for (offset in reading - 1 downTo 1) Arena.setExplored(x - offset, y)
                }

                270 -> {
                    y += 1
                    if (reading <= readingThreshold) Arena.setObstacle(x, y + reading)
                    for (offset in reading - 1 downTo 1) Arena.setExplored(x, y + offset)
                }
            }
        }

        fun updateArenaSensor1(x1: Int, y1: Int, facing: Int, reading: Int) {
            var x: Int = x1
            var y: Int = y1
            val r: Int = reading.coerceIn(0, 3)
            if (r <= 0) return

            when (facing) {
                0   -> {
                    x += 1
                    y += 1
                    if (r == 1) Arena.setObstacle(x, y + r)
                    else for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
                }

                90   -> {
                    x += 1
                    y -= 1
                    if (r == 1) Arena.setObstacle(x + r, y)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
                    }
                }

                180   -> {
                    x -= 1
                    y -= 1
                    if (r == 1) Arena.setObstacle(x, y - r)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y - offset)
                    }
                }

                270   -> {
                    x -= 1
                    y += 1
                    if (r == 1) Arena.setObstacle(x - r, y)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
                    }
                }
            }
        }

        fun updateArenaSensor2(x1: Int, y1: Int, facing: Int, reading: Int) {
            var x: Int = x1
            var y: Int = y1
            val r: Int = reading.coerceIn(0, 3)
            if (r <= 0) return

            when (facing) {
                0   -> {
                    y += 1
                    if (r == 1) Arena.setObstacle(x, y + r)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
                    }
                }

                90   -> {
                    x += 1
                    if (r == 1) Arena.setObstacle(x + r, y)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
                    }
                }

                180   -> {
                    y -= 1
                    if (r == 1) Arena.setObstacle(x, y - r)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y - offset)
                    }
                }

                270   -> {
                    x -= 1
                    if (r == 1) Arena.setObstacle(x - r, y)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
                    }
                }
            }
        }

        fun updateArenaSensor3(x1: Int, y1: Int, facing: Int, reading: Int) {
            var x: Int = x1
            var y: Int = y1
            val r: Int = reading.coerceIn(0, 3)
            if (r <= 0) return

            when (facing) {
                0   -> {
                    x -= 1
                    y += 1
                    if (r == 1) Arena.setObstacle(x, y + r)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
                    }
                }

                90   -> {
                    x += 1
                    y += 1
                    if (r == 1) Arena.setObstacle(x + r, y)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
                    }
                }

                180   -> {
                    x += 1
                    y -= 1
                    if (r == 1) Arena.setObstacle(x, y - r)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y - offset)
                    }
                }

                270   -> {
                    x -= 1
                    y -= 1
                    if (r == 1) Arena.setObstacle(x - r, y)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
                    }
                }
            }
        }

        fun updateArenaSensor4(x1: Int, y1: Int, facing: Int, reading: Int) {
            var x: Int = x1
            var y: Int = y1
            val r: Int = reading.coerceIn(0, 3)
            if (r <= 0) return

            when (facing) {
                0 -> {
                    x -= 1
                    y += 1
                    if (r == 1) Arena.setObstacle(x - r, y)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
                    }
                }

                90 -> {
                    x += 1
                    y += 1
                    if (r == 1) Arena.setObstacle(x, y + r)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
                    }
                }

                180 -> {
                    x += 1
                    y -= 1
                    if (r == 1) Arena.setObstacle(x + r, y)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
                    }
                }

                270 -> {
                    x -= 1
                    y -= 1
                    if (r == 1) Arena.setObstacle(x, y - r)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y - offset)
                    }
                }
            }
        }

        fun updateArenaSensor5(x1: Int, y1: Int, facing: Int, reading: Int) {
            var x: Int = x1
            var y: Int = y1
            val r: Int = reading.coerceIn(0, 3)
            if (r <= 0) return

            when (facing) {
                0 -> {
                    x -= 1
                    y -= 1
                    if (r == 1) Arena.setObstacle(x - r, y)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
                    }
                }

                90 -> {
                    x -= 1
                    y += 1
                    if (r == 1) Arena.setObstacle(x, y + r)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
                    }
                }

                180 -> {
                    x += 1
                    y += 1
                    if (r == 1) Arena.setObstacle(x + r, y)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
                    }
                }

                270 -> {
                    x += 1
                    y -= 1
                    if (r == 1) Arena.setObstacle(x, y - r)
                    else {
                        for (offset in r - 1 downTo 1) Arena.setExplored(x, y - offset)
                    }
                }
            }
        }

        fun updateArenaSensor6(x1: Int, y1: Int, facing: Int, reading: Int) {
            var x: Int = x1
            var y: Int = y1
            val r: Int = reading.coerceIn(0, 6)
            if (r <= 0) return

            when (facing) {
                0 -> {
                    x += 1
                    if (r <= 5) Arena.setObstacle(x + r, y)
                    for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)
                }

                90 -> {
                    y -= 1
                    if (r <= 5) Arena.setObstacle(x, y - r)
                    for (offset in 1 until r) Arena.setExplored(x, y - offset)
                }

                180 -> {
                    x -= 1
                    if (r <= 5) Arena.setObstacle(x - r, y)
                    for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)
                }

                270 -> {
                    y += 1
                    if (r <= 5) Arena.setObstacle(x, y + r)
                    for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)
                }
            }
        }
    }
}
