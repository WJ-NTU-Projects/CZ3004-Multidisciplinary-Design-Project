package wjayteo.mdp.algorithms.arena

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import wjayteo.mdp.algorithms.algorithm.Algorithm.Companion.ACTIONS_PER_SECOND
import wjayteo.mdp.algorithms.uicomponent.ArenaMapView
import kotlin.math.abs

class Robot {
    companion object {
        val position = Coordinates(1, 1)
        var facing: Int = 0
        private var attachedView: ArenaMapView? = null

        fun setAttachedView(view: ArenaMapView) {
            attachedView = view
        }

        fun reset() {
            move(Arena.start.x, Arena.start.y)
            updateFacing(0)
        }

        fun checkRight(): Boolean {
            val x: Int = position.x
            val y: Int = position.y

            when (facing) {
                0 -> {
                    if (Arena.isInvalidCoordinates(x + 2, y)) return false
                    if (Arena.isScanned(x + 2, y)) return false
                    return (Arena.isObstacle(x + 2, y))
                }

                180 -> {
                    if (Arena.isInvalidCoordinates(x - 2, y)) return false
                    if (Arena.isScanned(x - 2, y)) return false
                    return (Arena.isObstacle(x - 2, y))
                }

                90 -> {
                    if (Arena.isInvalidCoordinates(x, y - 2)) return false
                    if (Arena.isScanned(x, y - 2)) return false
                    return (Arena.isObstacle(x, y - 2))
                }

                270 -> {
                    if (Arena.isInvalidCoordinates(x, y + 2)) return false
                    if (Arena.isScanned(x, y + 2)) return false
                    return (Arena.isObstacle(x, y + 2))
                }
            }

            return false
        }

        fun getContinuousMoveCount(imagesFound: Int): Int {
            var x: Int = position.x
            var y: Int = position.y
            var count = 0
            var imagesCount = 5

            when (facing) {
                0 -> {
                    y += 1
                    x -= 2

                    while (true) {
                        if (count >= 5) return count
                        if (imagesFound < imagesCount && Arena.isObstacle2(x, y)) return count
                        if (!Arena.isExplored(x, y)) return count
                        if (!isLeftObstructed(position.x, y)) return count
                        if (isFrontObstructed(position.x, y) || !Arena.isExplored(position.x, y + 2)|| !Arena.isExplored(position.x - 1, y + 2)|| !Arena.isExplored(position.x + 1, y + 2)) return count
                        if (rightHasUnexplored(position.x, y)) return count
                        count++
                        y++
                    }
                }

                90 -> {
                    x += 1
                    y += 2

                    while (true) {
                        if (count >= 5) return count
                        if (imagesFound < imagesCount && Arena.isObstacle2(x, y)) return count
                        if (!Arena.isExplored(x, y)) return count
                        if (!isLeftObstructed(x, position.y)) return count
                        if (isFrontObstructed(x, position.y) || !Arena.isExplored(x + 2, position.y) || !Arena.isExplored(x + 2, position.y - 1) || !Arena.isExplored(x + 2, position.y + 1)) return count
                        println("$x, ${position.y}")
                        if (rightHasUnexplored(x, position.y)) return count
                        count++
                        x++
                    }
                }

                180 -> {
                    y -= 1
                    x += 2

                    while (true) {
                        if (count >= 5) return count
                        if (imagesFound < imagesCount && Arena.isObstacle2(x, y)) return count
                        if (!Arena.isExplored(x, y)) return count
                        if (!isLeftObstructed(position.x, y)) return count
                        if (isFrontObstructed(position.x, y) || !Arena.isExplored(position.x, y - 2)|| !Arena.isExplored(position.x - 1, y - 2)|| !Arena.isExplored(position.x + 1, y - 2)) return count
                        if (rightHasUnexplored(position.x, y)) return count
                        count++
                        y--
                    }
                }

                270 -> {
                    x -= 1
                    y -= 2

                    while (true) {
                        if (count >= 5) return count
                        if (imagesFound < imagesCount && Arena.isObstacle2(x, y)) return count
                        if (!Arena.isExplored(x, y)) return count
                        if (!isLeftObstructed(x, position.y)) return count
                        if (isFrontObstructed(x, position.y) || !Arena.isExplored(x - 2, position.y) || !Arena.isExplored(x - 2, position.y - 1) || !Arena.isExplored(x - 2, position.y + 1)) return count
                        if (rightHasUnexplored(x, position.y)) return count
                        count++
                        x--
                    }
                }
            }

            return 0
        }

        private fun test() {
            val x: Int = position.x
            val y: Int = position.y

            when (facing) {
                0 -> {
                    for (x2 in x - 1 .. x + 1) Arena.setExplored(x2, y + 2)

                    for (y2 in y - 1 .. y + 1 step 2) {
                        Arena.setExplored(x - 2, y2)
                        Arena.setScanned(x - 2, y2)
                    }

                    for (x2 in x + 2..x + 6) {
                        if (Arena.isObstacle(x2, y)) return
                        Arena.setExplored(x2, y)
                    }
                }

                180 -> {
                    for (x2 in x - 1 .. x + 1) Arena.setExplored(x2, y - 2)

                    for (y2 in y - 1 .. y + 1 step 2) {
                        Arena.setExplored(x + 2, y2)
                        Arena.setScanned(x + 2, y2)
                    }

                    for (x2 in x - 2 downTo x - 6) {
                        if (Arena.isObstacle(x2, y)) return
                        Arena.setExplored(x2, y)
                    }
                }

                90 -> {
                    for (y2 in y - 1 .. y + 1) Arena.setExplored(x + 2, y2)

                    for (x2 in x - 1 .. x + 1 step 2) {
                        Arena.setExplored(x2, y + 2)
                        Arena.setScanned(x2, y + 2)
                    }

                    for (y2 in y - 2 downTo y - 6) {
                        if (Arena.isObstacle(x, y2)) return
                        Arena.setExplored(x, y2)
                    }
                }

                270 -> {
                    for (y2 in y - 1 .. y + 1) Arena.setExplored(x - 2, y2)

                    for (x2 in x - 1 .. x + 1 step 2) {
                        Arena.setExplored(x2, y - 2)
                        Arena.setScanned(x2, y - 2)
                    }

                    for (y2 in y + 2..y + 6) {
                        if (Arena.isObstacle(x, y2)) return
                        Arena.setExplored(x, y2)
                    }
                }
            }
        }

        fun setObstaclesScanned() {
            val x: Int = position.x
            val y: Int = position.y

            when (facing) {
                0 -> {
                    for (y2 in y - 1 .. y + 1 step 2) {
                        Arena.setScanned(x - 2, y2)
                    }
                }

                180 -> {
                    for (y2 in y - 1 .. y + 1 step 2) {
                        Arena.setScanned(x + 2, y2)
                    }
                }

                90 -> {
                    for (x2 in x - 1 .. x + 1 step 2) {
                        Arena.setScanned(x2, y + 2)
                    }
                }

                270 -> {
                    for (x2 in x - 1 .. x + 1 step 2) {
                        Arena.setScanned(x2, y - 2)
                    }
                }
            }
        }

        fun move(x: Int, y: Int) {
            if (!Arena.isMovable(x, y)) return
            position.x = x
            position.y = y

            for (yOffset in -1..1) {
                for (xOffset in -1..1) {
                    Arena.setExploredForced(x + xOffset, y + yOffset)
                    Arena.setVisited(x + xOffset, y + yOffset)
                }
            }

            //test2()
            attachedView?.setRobotPosition(x, y)
        }

        fun moveTemp() {
            var x: Int = position.x
            var y: Int = position.y

            when (facing) {
                0 -> y += 1
                90 -> x += 1
                180 -> y -= 1
                270 -> x -= 1
            }

            move(x, y)
        }

        suspend fun moveAdvanced(x: Int, y: Int) = withContext(Dispatchers.Default) {
            if (!Arena.isMovable(x, y)) return@withContext
            val positionDifferenceX: Int = (x - position.x)
            val positionDifferenceY: Int = (y - position.y)

            val direction: Int = when {
                (positionDifferenceX == 0 && positionDifferenceY == 1)  -> 0
                (positionDifferenceX == 1 && positionDifferenceY == 0)  -> 90
                (positionDifferenceX == 0 && positionDifferenceY == -1) -> 180
                (positionDifferenceX == -1 && positionDifferenceY == 0) -> 270
                else -> facing
            }

            var newFacing: Int = direction
            val facingDifference = abs(newFacing - facing)
            val delay: Long = (1000.0 / ACTIONS_PER_SECOND).toLong()

            if (facingDifference == 180) {
                for (i in 0..1) {
                    turn(90)
                    delay(delay / 2)
                }
                newFacing = facing
            }

            if (newFacing != facing) {
                updateFacing(newFacing)
                delay(delay / 2)
            }

            move(x, y)
        }

        fun updateFacing(angle1: Int) {
            var angle: Int = angle1
            if (angle >= 360) angle -= 360
            else if (angle < 0) angle += 360
            if (facing == angle) return
            facing = angle
            attachedView?.setRobotFacing(facing)
        }

        fun turn(angle: Int) {
            facing += angle
            if (facing >= 360) facing -= 360
            else if (facing < 0) facing += 360
            attachedView?.setRobotFacing(facing)
            //test()
        }

        fun isFrontObstructed(): Boolean {
            return when (facing) {
                0    -> !Arena.isMovable(position.x, position.y + 1)
                90   -> !Arena.isMovable(position.x + 1, position.y)
                180  -> !Arena.isMovable(position.x, position.y - 1)
                270  -> !Arena.isMovable(position.x - 1, position.y)
                else -> true
            }
        }

        fun isWallFront2(): Boolean {
            return when (facing) {
                0    -> Arena.isInvalidCoordinates(position.x, position.y + 3)
                90   -> Arena.isInvalidCoordinates(position.x + 3, position.y)
                180  -> Arena.isInvalidCoordinates(position.x, position.y - 3)
                270  -> Arena.isInvalidCoordinates(position.x - 3, position.y)
                else -> false
            }
        }

        fun isLeftCompletelyBlocked(): Boolean {
            when (facing) {
                0    -> {
                    if (Arena.isObstacle(position.x - 2, position.y - 1) && Arena.isObstacle(position.x - 2, position.y) && Arena.isObstacle(position.x - 2, position.y + 1)) return true
                }
                90   -> {
                    if (Arena.isObstacle(position.x - 1, position.y + 2) && Arena.isObstacle(position.x, position.y + 2) && Arena.isObstacle(position.x + 1, position.y + 2)) return true
                }
                180  -> {
                    if (Arena.isObstacle(position.x + 2, position.y - 1) && Arena.isObstacle(position.x + 2, position.y) && Arena.isObstacle(position.x + 2, position.y + 1)) return true
                }
                270  -> {
                    if (Arena.isObstacle(position.x - 1, position.y - 2) && Arena.isObstacle(position.x, position.y - 2) && Arena.isObstacle(position.x + 1, position.y - 2)) return true
                }
            }

            return false
        }

        fun isLeftCompletelyBlocked2(): Boolean {
            when (facing) {
                0    -> {
                    if (Arena.isObstacle(position.x - 3, position.y - 1) && Arena.isObstacle(position.x - 3, position.y) && Arena.isObstacle(position.x - 3, position.y + 1)) return true
                }
                90   -> {
                    if (Arena.isObstacle(position.x - 1, position.y + 3) && Arena.isObstacle(position.x, position.y + 3) && Arena.isObstacle(position.x + 1, position.y + 3)) return true
                }
                180  -> {
                    if (Arena.isObstacle(position.x + 3, position.y - 1) && Arena.isObstacle(position.x + 3, position.y) && Arena.isObstacle(position.x + 3, position.y + 1)) return true
                }
                270  -> {
                    if (Arena.isObstacle(position.x - 1, position.y - 3) && Arena.isObstacle(position.x, position.y - 3) && Arena.isObstacle(position.x + 1, position.y - 3)) return true
                }
            }

            return false
        }

        fun isLeftObstructed(): Boolean {
            return when (facing) {
                0    -> !Arena.isMovable(position.x - 1, position.y)
                90   -> !Arena.isMovable(position.x, position.y + 1)
                180  -> !Arena.isMovable(position.x + 1, position.y)
                270  -> !Arena.isMovable(position.x, position.y - 1)
                else -> true
            }
        }

        fun isLeftObstructed2(): Boolean {
            return when (facing) {
                0    -> !Arena.isMovable(position.x - 2, position.y)
                90   -> !Arena.isMovable(position.x, position.y + 2)
                180  -> !Arena.isMovable(position.x + 2, position.y)
                270  -> !Arena.isMovable(position.x, position.y - 2)
                else -> true
            }
        }

        fun isFrontObstructed(x: Int, y: Int): Boolean {
            return when (facing) {
                0    -> !Arena.isMovable(x, y + 1)
                90   -> !Arena.isMovable(x + 1, y)
                180  -> !Arena.isMovable(x, y - 1)
                270  -> !Arena.isMovable(x - 1, y)
                else -> true
            }
        }

        fun isLeftObstructed(x: Int, y: Int): Boolean {
            return when (facing) {
                0    -> !Arena.isMovable(x - 1, y)
                90   -> !Arena.isMovable(x, y + 1)
                180  -> !Arena.isMovable(x + 1, y)
                270  -> !Arena.isMovable(x, y - 1)
                else -> true
            }
        }

        fun leftHasUnexplored(): Boolean {
            val robotX: Int = position.x
            val robotY: Int = position.y

            when (facing) {
                0 -> {
                    for (y in robotY - 1..robotY + 1) {
                        for (x in 0..robotX - 2) {
                            if (!Arena.isExplored(x, y)) return true
                        }
                    }
                }

                180 -> {
                    for (y in robotY - 1..robotY + 1) {
                        for (x in 14 downTo robotX + 2) {
                            if (!Arena.isExplored(x, y)) return true
                        }
                    }
                }

                90 -> {
                    for (x in robotX - 1..robotX + 1) {
                        for (y in 19 downTo robotY + 2) {
                            if (!Arena.isExplored(x, y)) return true
                        }
                    }
                }

                270 -> {
                    for (x in robotX - 1..robotX + 1) {
                        for (y in 0..robotY - 2) {
                            if (!Arena.isExplored(x, y)) return true
                        }
                    }
                }
            }

            return false
        }

        private fun rightHasUnexplored(robotX: Int, robotY: Int): Boolean {
            when (facing) {
                0 -> {
                    for (x in robotX + 2 .. robotX + 6) {
                        if (!Arena.isExplored(x, robotY)) return true
                    }
                }

                180 -> {
                    for (x in robotX - 2 downTo robotX - 6) {
                        if (!Arena.isExplored(x, robotY)) return true
                    }
                }

                90 -> {
                    for (y in robotY - 2 downTo robotY - 6) {
                        if (!Arena.isExplored(robotX, y)) return true
                    }
                }

                270 -> {
                    for (y in robotY + 2 .. robotY + 6) {
                        if (!Arena.isExplored(robotX, y)) return true
                    }
                }
            }

            return false
        }

        fun rightHasObstacle(): Boolean {
            val robotX: Int = position.x
            val robotY: Int = position.y

            when (facing) {
                0 -> {
                    for (x in robotX + 2 .. robotX + 6) {
                        if (Arena.isObstacle2(x, robotY)) {
                            return true
                        }
                    }
                }

                180 -> {
                    for (x in robotX - 2 downTo robotX - 6) {
                        if (Arena.isObstacle2(x, robotY)) {
                            return true
                        }
                    }
                }

                90 -> {
                    for (y in robotY - 2 downTo robotY - 6) {
                        if (Arena.isObstacle2(robotX, y)) {
                            return true
                        }
                    }
                }

                270 -> {
                    for (y in robotY + 2 .. robotY + 6) {
                        if (Arena.isObstacle2(robotX, y)) {
                            return true
                        }
                    }
                }
            }

            return false
        }

        fun isRightObstructed(): Boolean {
            return when (facing) {
                0    -> !Arena.isMovable(position.x + 1, position.y)
                90   -> !Arena.isMovable(position.x, position.y - 1)
                180  -> !Arena.isMovable(position.x - 1, position.y)
                270  -> !Arena.isMovable(position.x, position.y + 1)
                else -> true
            }
        }

        fun isRightCompletelyBlocked(): Boolean {
            when (facing) {
                0    -> {
                    if (Arena.isObstacle(position.x + 2, position.y - 1) && Arena.isObstacle(position.x + 2, position.y) && Arena.isObstacle(position.x + 2, position.y + 1)) return true
                }
                90   -> {
                    if (Arena.isObstacle(position.x - 1, position.y - 2) && Arena.isObstacle(position.x, position.y - 2) && Arena.isObstacle(position.x + 1, position.y - 2)) return true
                }
                180  -> {
                    if (Arena.isObstacle(position.x - 2, position.y - 1) && Arena.isObstacle(position.x - 2, position.y) && Arena.isObstacle(position.x - 2, position.y + 1)) return true
                }
                270  -> {
                    if (Arena.isObstacle(position.x - 1, position.y + 2) && Arena.isObstacle(position.x, position.y + 2) && Arena.isObstacle(position.x + 1, position.y + 2)) return true
                }
            }

            return false
        }
    }
}