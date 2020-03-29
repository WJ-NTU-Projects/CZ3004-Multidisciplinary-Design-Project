package wjayteo.mdp.algorithms.arena

import wjayteo.mdp.algorithms.algorithm.Algorithm.Companion.COVERAGE_LIMIT
import wjayteo.mdp.algorithms.uicomponent.ArenaMapView
import wjayteo.mdp.algorithms.uicomponent.MasterView
import wjayteo.mdp.algorithms.wifi.WifiSocketController
import kotlin.math.exp

class Arena {
    companion object {
        private const val GRID_UNKNOWN      : Int = 0
        private const val GRID_EXPLORED     : Int = 1
        private const val GRID_SUSPECT      : Int = 2
        private const val GRID_OBSTACLE     : Int = 3

        val start = Coordinates(1, 1)
        val goal = Coordinates(13, 18)
        val waypoint = Coordinates(-1, -1)
        val exploreArray: Array<Array<Int>> = Array(20) { Array(15) { 0 } }
        val obstacleArray: Array<Array<Int>> = Array(20) { Array(15) { 0 } }
        private val gridStateArray: Array<Array<Int>> = Array(20) { Array(15) { 0 } }
        private val visitedArray: Array<Array<Int>> = Array(20) { Array(15) { 0 } }
        private val scannedArray: Array<Array<Int>> = Array(20) { Array(15) { 0 } }
        private val counterArray: Array<Array<Int>> = Array(20) { Array(15) { 0 } }
        private var attachedView: ArenaMapView? = null

        fun setAttachedView(view: ArenaMapView) {
            attachedView = view
        }

        fun reset() {
            for (y in 19 downTo 0) for (x in 0..14) setUnknown(x, y)
            waypoint.x = -1
            waypoint.y = -1
            attachedView?.removeWaypoint()
        }

        fun setScanned(x: Int, y: Int) {
            if (isInvalidCoordinates(x, y)) return

            if (isObstacle(x, y)) {
                scannedArray[y][x] = 1
                attachedView?.setScanned(x, y)
            }
        }

        fun isScanned(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y)) return true
            if (!isObstacle(x, y)) return true
            return (scannedArray[y][x] == 1)
        }


        fun setVisited(x: Int, y: Int) {
            if (isInvalidCoordinates(x, y)) return
            visitedArray[y][x]++
        }

        fun isVisitedRepeated(x: Int, y: Int): Boolean {
            return (visitedArray[y][x] >= 3)
        }

        fun loadMap(explore: Array<Array<Int>>, obstacle: Array<Array<Int>>) {
            reset()

            for (y in 19 downTo 0) {
                for (x in 0..14) {
                    val exploreBit: Int = explore[y][x]
                    val obstacleBit: Int = obstacle[y][x]
                    exploreArray[y][x] = exploreBit
                    obstacleArray[y][x] = obstacleBit
                    if (exploreBit == 1) setExplored(x, y)
                    if (obstacleBit == 1) setObstacle(x, y)
                }
            }
        }

        fun isInvalidCoordinates(x: Int, y: Int, robotSize: Boolean = false): Boolean {
            return if (robotSize) (x < 1 || x > 13 || y < 1 || y > 18)
            else (x < 0 || x > 14 || y < 0 || y > 19)
        }

        fun isInvalidCoordinates(coordinates: Coordinates, robotSize: Boolean = false): Boolean {
            val x: Int = coordinates.x
            val y: Int = coordinates.y
            return isInvalidCoordinates(x, y, robotSize)
        }

        fun isExplored(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y)) return true
            return (exploreArray[y][x] == 1)
        }

        fun setAllExplored() {
            for (y in 19 downTo 0) {
                for (x in 0..14) {
                    setExplored(x, y)
                }
            }
        }

        fun setAllUnknown() {
            for (y in 19 downTo 0) {
                for (x in 0..14) {
                    if (obstacleArray[y][x] == 0) setUnknown(x, y)
                }
            }

            Robot.move(Robot.position.x, Robot.position.y)
        }

        fun isEveryGridExplored(): Boolean {
            for (y in 19 downTo 0) {
                for (x in 0..14) {
                    if (exploreArray[y][x] == 0) return false
                }
            }

            return true
        }

        fun isMovable(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y, true)) return false

            for (yOffset in -1 .. 1) {
                for (xOffset in -1 .. 1) {
                    if (isObstacle(x + xOffset, y + yOffset)) return false
                }
            }

            return true
        }

        fun setStartPoint(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y)) return false
            if (x == start.x && y == start.y) return true

            for (yOffset in -1..1) {
                for (xOffset in -1..1) {
                    val x1: Int = x + xOffset
                    val y1: Int = y + yOffset
                    if (isObstacle(x1, y1) || isGoalPoint(x1, y1) || isWaypoint(x1, y1)) return false
                }
            }

            start.x = x
            start.y = y
            setExploredPoint(x, y)
            attachedView?.setStartPoint(x, y)
            return true
        }

        fun setGoalPoint(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y)) return false
            if (x == goal.x && y == goal.y) return true

            for (yOffset in -1..1) {
                for (xOffset in -1..1) {
                    val x1: Int = x + xOffset
                    val y1: Int = y + yOffset
                    if (isObstacle(x1, y1) || isStartPoint(x1, y1) || isWaypoint(x1, y1)) return false
                }
            }

            goal.x = x
            goal.y = y
            setExploredPoint(x, y)
            attachedView?.setGoalPoint(x, y)
            return true
        }

        fun setWaypoint(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y)) return false
            if (x == waypoint.x && y == waypoint.y) return true

            for (yOffset in -1..1) {
                for (xOffset in -1..1) {
                    val x1: Int = x + xOffset
                    val y1: Int = y + yOffset
                    if (isObstacle(x1, y1) || isGoalPoint(x1, y1) || isStartPoint(x1, y1)) return false
                }
            }

            waypoint.x = x
            waypoint.y = y
            setExploredPoint(x, y)
            attachedView?.setWaypoint(x, y)
            return true
        }

        private fun setExploredPoint(x: Int, y: Int) {
            for (yOffset in -1..1) {
                for (xOffset in -1..1) {
                    val x1: Int = x + xOffset
                    val y1: Int = y + yOffset
                    setExploredForced(x1, y1)
                }
            }
        }

        fun refreshPoints() {
            setExploredPoint(waypoint.x, waypoint.y)
            setExploredPoint(start.x, start.y)
            setExploredPoint(goal.x, goal.y)
        }

        fun setExplored(x: Int, y: Int) {
            if (isInvalidCoordinates(x, y)) return
            if (gridStateArray[y][x] >= GRID_OBSTACLE) return
            gridStateArray[y][x] = GRID_EXPLORED
            exploreArray[y][x] = 1
            attachedView?.setExplored(x, y)
        }

        fun setExploredForced(x: Int, y: Int) {
            if (isInvalidCoordinates(x, y)) return
            gridStateArray[y][x] = GRID_EXPLORED
            exploreArray[y][x] = 1
            obstacleArray[y][x] = 0
            attachedView?.setExplored(x, y)
        }

        fun endHug() {
            for (y in 19 downTo 0) {
                for (x in 0..14) {
                    if (gridStateArray[y][x] == GRID_SUSPECT) setObstacle(x, y)
                    if (gridStateArray[y][x] == GRID_UNKNOWN) setExplored(x, y)
                    if (visitedArray[y][x] > 0) setExploredForced(x, y)
                }
            }
        }

        fun setGridState(x: Int, y: Int, value: Int) {
            if (isInvalidCoordinates(x, y) || isGoalPoint(x, y) || isStartPoint(x, y) || isWaypoint(x, y)) return
            counterArray[y][x] += value

            if (counterArray[y][x] > 0) {
                gridStateArray[y][x] = GRID_OBSTACLE
                exploreArray[y][x] = 1
                obstacleArray[y][x] = 1
                if (scannedArray[y][x] == 1) attachedView?.setScanned(x, y)
                else attachedView?.setObstacle(x, y)
            } else {
                gridStateArray[y][x] = GRID_EXPLORED
                exploreArray[y][x] = 1
                obstacleArray[y][x] = 0
                scannedArray[y][x] = 0
                attachedView?.setExplored(x, y)
            }
        }

        private fun setObstacle(x: Int, y: Int) {
            if (isInvalidCoordinates(x, y)) return
            if (gridStateArray[y][x] >= GRID_OBSTACLE) return

            if (isOccupied(x, y)) {
                setExplored(x, y)
                return
            }

            gridStateArray[y][x] = GRID_OBSTACLE
            exploreArray[y][x] = 1
            obstacleArray[y][x] = 1
            attachedView?.setObstacle(x, y)
        }

        fun plotFastestPath() {
            for (y in 19 downTo 0) {
                for (x in 0..14) {
                    if (isObstacle(x, y)) continue
                    if (isExplored(x, y)) setExplored(x, y) else setUnknown(x, y)
                }
            }

            val pathList: List<IntArray> = MasterView.fastestPath.computeFastestPath()

            for (p in pathList) {
                val x = p[0]
                val y = p[1]
                setFastestPath(x, y)
            }
        }

        fun sendArena() {
            val descriptor: List<String> = MapDescriptor.fromArray(exploreArray, obstacleArray, 1)
            WifiSocketController.write("D", "#r:${descriptor[0]},${descriptor[1]},${Robot.position.x},${Robot.position.y},${Robot.facing}")
        }

        fun plotObstacle(x: Int, y: Int) {
            if (isInvalidCoordinates(x, y)) return

            if (isObstacle(x, y)) {
                setUnknown(x, y)
                setExplored(x, y)
                return
            }

            if (isOccupied(x, y)) return
            setObstacle(x, y)
        }

        fun coverageReached(): Boolean {
            var coveredCount = 0
            exploreArray.forEach { it.forEach { grid -> if (grid == 1) coveredCount++ } }
            return ((1.0 * coveredCount / 300) * 100 >= COVERAGE_LIMIT)
        }

        private fun setFastestPath(x: Int, y: Int) {
            if (isInvalidCoordinates(x, y)) return
            if (gridStateArray[y][x] >= GRID_OBSTACLE) return
            attachedView?.setFastestPath(x, y)
        }

        private fun setUnknown(x: Int, y: Int) {
            if (isInvalidCoordinates(x, y)) return
            gridStateArray[y][x] = GRID_UNKNOWN
            exploreArray[y][x] = 0
            obstacleArray[y][x] = 0
            scannedArray[y][x] = 0
            visitedArray[y][x] = 0
            attachedView?.setUnknown(x, y)
        }

        private fun isOccupied(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y, false)) return true
            if (gridStateArray[y][x] >= GRID_OBSTACLE) return true
            if (isStartPoint(x, y) || isGoalPoint(x, y) || isWaypoint(x, y)) return true
            return false
        }

        fun isObstacle(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y, false)) return true
            return (obstacleArray[y][x] == 1)
        }

        fun isObstacle2(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y, false)) return false
            return (obstacleArray[y][x] == 1)
        }

        private fun isStartPoint(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y, false)) return true
            if (isInvalidCoordinates(start)) return false
            return (intArrayOf(start.x - 1, start.x, start.x + 1).contains(x) && intArrayOf(start.y - 1, start.y, start.y + 1).contains(y))
        }

        private fun isGoalPoint(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y, false)) return true
            if (isInvalidCoordinates(goal)) return false
            return (intArrayOf(goal.x - 1, goal.x, goal.x + 1).contains(x) && intArrayOf(goal.y - 1, goal.y, goal.y + 1).contains(y))
        }

        private fun isWaypoint(x: Int, y: Int): Boolean {
            if (isInvalidCoordinates(x, y, false)) return true
            if (isInvalidCoordinates(waypoint)) return false
            return (intArrayOf(waypoint.x - 1, waypoint.x, waypoint.x + 1).contains(x) && intArrayOf(waypoint.y - 1, waypoint.y, waypoint.y + 1).contains(y))
        }
    }
}