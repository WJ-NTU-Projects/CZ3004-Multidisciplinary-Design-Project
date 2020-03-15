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
                    delay(delay)
                }
                newFacing = facing
            }

            if (newFacing != facing) {
                updateFacing(newFacing)
                delay(delay)
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

        fun isLeftObstructed(): Boolean {
            return when (facing) {
                0    -> !Arena.isMovable(position.x - 1, position.y)
                90   -> !Arena.isMovable(position.x, position.y + 1)
                180  -> !Arena.isMovable(position.x + 1, position.y)
                270  -> !Arena.isMovable(position.x, position.y - 1)
                else -> true
            }
        }

//        fun isRightObstructed(): Boolean {
//            return when (facing) {
//                0    -> !Arena.isMovable(position.x + 1, position.y)
//                90   -> !Arena.isMovable(position.x, position.y - 1)
//                180  -> !Arena.isMovable(position.x - 1, position.y)
//                270  -> !Arena.isMovable(position.x, position.y + 1)
//                else -> true
//            }
//        }
    }
}