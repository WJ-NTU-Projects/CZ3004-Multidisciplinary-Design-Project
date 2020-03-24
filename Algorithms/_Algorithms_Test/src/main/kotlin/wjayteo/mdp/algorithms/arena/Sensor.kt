package wjayteo.mdp.algorithms.arena

class Sensor {
    companion object {

        fun updateArenaSensor1(x1: Int, y1: Int, facing: Int, reading: Int) {
            var x: Int = x1
            var y: Int = y1
            val r: Int = reading.coerceIn(0, 2)
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
            val r: Int = reading.coerceIn(0, 2)
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
            val r: Int = reading.coerceIn(0, 2)
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
            val r: Int = reading.coerceIn(0, 2)
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
            val r: Int = reading.coerceIn(0, 2)
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
                    for (offset in r - 1 downTo 1) Arena.setExplored(x + offset, y)

                    if (r <= 5) {
                        for (offset in r - 1 downTo 1) if (Arena.isObstacle(x + offset, y)) return
                        Arena.setObstacle(x + r, y)
                    }
                }

                90 -> {
                    y -= 1
                    for (offset in 1 until r) Arena.setExplored(x, y - offset)

                    if (r <= 5) {
                        for (offset in 1 until r) if (Arena.isObstacle(x, y - offset)) return
                        Arena.setObstacle(x, y - r)
                    }
                }

                180 -> {
                    x -= 1
                    for (offset in r - 1 downTo 1) Arena.setExplored(x - offset, y)

                    if (r <= 5) {
                        for (offset in r - 1 downTo 1) if (Arena.isObstacle(x - offset, y)) return
                        Arena.setObstacle(x - r, y)
                    }
                }

                270 -> {
                    y += 1
                    for (offset in r - 1 downTo 1) Arena.setExplored(x, y + offset)

                    if (r <= 5) {
                        for (offset in r - 1 downTo 1) if (Arena.isObstacle(x, y + offset)) return
                        Arena.setObstacle(x, y + r)
                    }
                }
            }
        }
    }
}
