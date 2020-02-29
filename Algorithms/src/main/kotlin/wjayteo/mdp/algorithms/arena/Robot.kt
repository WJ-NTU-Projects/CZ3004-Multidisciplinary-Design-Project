package wjayteo.mdp.algorithms.arena

import wjayteo.mdp.algorithms.uicomponent.ArenaMapView

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
            if (Arena.isInvalidCoordinates(x, y, true)) return
            position.x = x
            position.y = y
            for (yOffset in -1..1) for (xOffset in -1..1) Arena.setExplored(x + yOffset, y + xOffset)
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

        fun isMovable(x: Int, y: Int): Boolean {
            if (Arena.isInvalidCoordinates(x, y, true)) return false

            for (yOffset in -1 .. 1) {
                for (xOffset in -1 .. 1) {
                    if (Arena.isObstacle(x + xOffset, y + yOffset)) return false
                }
            }

            return true
        }

        fun isFrontObstructed(): Boolean {
            return when (facing) {
                0    -> !isMovable(position.x, position.y + 1)
                90   -> !isMovable(position.x + 1, position.y)
                180  -> !isMovable(position.x, position.y - 1)
                270  -> !isMovable(position.x - 1, position.y)
                else -> true
            }
        }

        fun isLeftObstructed(): Boolean {
            return when (facing) {
                0    -> !isMovable(position.x - 1, position.y)
                90   -> !isMovable(position.x, position.y + 1)
                180  -> !isMovable(position.x + 1, position.y)
                270  -> !isMovable(position.x, position.y - 1)
                else -> true
            }
        }

        fun isRightObstructed(): Boolean {
            return when (facing) {
                0    -> !isMovable(position.x + 1, position.y)
                90   -> !isMovable(position.x, position.y - 1)
                180  -> !isMovable(position.x - 1, position.y)
                270  -> !isMovable(position.x, position.y + 1)
                else -> true
            }
        }
    }
}