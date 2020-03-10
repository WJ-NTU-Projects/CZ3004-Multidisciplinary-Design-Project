package wjayteo.mdp.algorithms.algorithm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wjayteo.mdp.algorithms.algorithm.AStarSearch.Companion.GridNode
import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.arena.Coordinates
import wjayteo.mdp.algorithms.arena.Robot
import wjayteo.mdp.algorithms.uicomponent.ControlsView
import wjayteo.mdp.algorithms.wifi.WifiSocketController
import java.util.*


class FastestPath : Algorithm() {
    private var started: Boolean = false
    private var simulationStarted: Boolean = false
    private var delay: Long = 100L

    override fun messageReceived(message: String) {
        when (message) {
            "pause" -> stop()
        }
    }

    fun start() {
        WifiSocketController.setListener(this)
        Robot.reset()
        step()

        if (ACTUAL_RUN) {
            started = true
        } else {
            delay = (1000.0 / ACTIONS_PER_SECOND).toLong()
            simulationStarted = true
            simulate(computeFastestPath())
        }
    }

    fun stop() {
        if (!started && !simulationStarted) return
        started = false
        simulationStarted = false
        if (ACTUAL_RUN) WifiSocketController.write("A", "B")
    }

    private fun simulate(pathList: List<IntArray>) {
        CoroutineScope(Dispatchers.Default).launch {
            Robot.updateFacing(pathList[0][2])

            while (simulationStarted) {
                if (Robot.position.x == Arena.goal.x && Robot.position.y == Arena.goal.y) {
                    stop()
                    ControlsView.stop()
                    return@launch
                }

                for (path in pathList) {
                    if (!simulationStarted) return@launch
                    delay(delay)
                    Robot.moveAdvanced(path[0], path[1])
                }
            }
        }
    }

    private fun step() {
        val pathList = computeFastestPath()
        var robotFacing = 0
        var s = ""

        for ((i, step) in pathList.withIndex()) {
            val facing = step[2]
            val diff = facing - robotFacing

            when (diff) {
                90 -> {
                    s += "R"
                    s += "M"
                }

                -90 -> {
                    s += "L"
                    s += "M"
                }

                0 -> {
                    s += "M"
                }

                180 -> {
                    s += "T"
                    s += "M"
                }
            }

            robotFacing += diff

            if (robotFacing < 0) robotFacing += 360
            else if (robotFacing >= 360) robotFacing -= 360
        }

        WifiSocketController.write("A", s)
    }

    fun computeFastestPath(): List<IntArray> {
        val startX = Arena.start.x
        val startY = Arena.start.y
        var startFacing1 = 0
        var startFacing2 = 90

        if (startX == 13 && startY == 1) {
            startFacing2 = 270
        } else if (startX == 1 && startY == 18) {
            startFacing1 = 180
        } else if (startX == 13 && startY == 18) {
            startFacing1 = 180
            startFacing2 = 270
        }

        val waypointX: Int = Arena.waypoint.x
        val waypointY: Int = Arena.waypoint.y
        val goalX: Int = Arena.goal.x
        val goalY: Int = Arena.goal.y
        val waypointEntranceList = ArrayList<Coordinates>()
        val goalEntranceList = ArrayList<Coordinates>()

        // FIND VIABLE ENTRANCES TO WAYPOINT
        for (yOffset in -1..1) {
            for (xOffset in -1..1) {
                if (yOffset == xOffset && yOffset == 0 || xOffset != 0 && yOffset != 0) continue
                var x = waypointX + xOffset
                var y = waypointY + yOffset
                if (Arena.isMovable(x, y)) waypointEntranceList.add(Coordinates(x, y))
                x = goalX + xOffset
                y = goalY + yOffset
                if (Arena.isMovable(x, y)) goalEntranceList.add(Coordinates(x, y))
            }
        }

        var finalPath: ArrayList<GridNode> = arrayListOf()
        var previousCost = Double.MAX_VALUE

        for (waypointEntrance in waypointEntranceList) {
            val path1: ArrayList<GridNode> = AStarSearch.run(startX, startY, startFacing1, waypointEntrance.x, waypointEntrance.y)
            var path1End = path1[path1.size - 1]
            val path1ToWp: ArrayList<GridNode> = AStarSearch.run(path1End.x, path1End.y, path1End.facing, waypointX, waypointY)
            path1.addAll(path1ToWp)
            path1End = path1[path1.size - 1]

            var finalGoalPath: ArrayList<GridNode> = arrayListOf()
            var previousGoalCost = Double.MAX_VALUE

            for (goalEntrance in goalEntranceList) {
                val goalPath: ArrayList<GridNode> = AStarSearch.run(path1End.x, path1End.y, path1End.facing, goalEntrance.x, goalEntrance.y)
                val (x, y, facing) = goalPath[goalPath.size - 1]
                val pathToGoal: ArrayList<GridNode> = AStarSearch.run(x, y, facing, goalX, goalY)
                goalPath.addAll(pathToGoal)

                var pathCost = 0.0
                var previousFacing = goalPath[0].facing

                for (node in goalPath) {
                    pathCost += node.f
                    if (node.facing != previousFacing) pathCost += 500;
                    previousFacing = node.facing;
                }

                if (pathCost <= previousGoalCost) {
                    finalGoalPath = goalPath
                    previousGoalCost = pathCost
                }
            }

            path1.addAll(finalGoalPath);
            var cost1 = 0.0
            var previousFacing = path1[0].facing

            for (node in path1) {
                cost1 += node.f;
                if (node.facing != previousFacing) cost1 += 500;
                previousFacing = node.facing;
            }

            val path2: ArrayList<GridNode> = AStarSearch.run(startX, startY, startFacing2, waypointEntrance.x, waypointEntrance.y)
            var path2End = path2[path2.size - 1]
            val path2ToWp: ArrayList<GridNode> = AStarSearch.run(path2End.x, path2End.y, path2End.facing, waypointX, waypointY)
            path2.addAll(path2ToWp)
            path2End = path2[path2.size - 1]

            finalGoalPath = arrayListOf()
            previousGoalCost = Double.MAX_VALUE

            for (goalEntrance in goalEntranceList) {
                val goalPath: ArrayList<GridNode> = AStarSearch.run(path2End.x, path2End.y, path2End.facing, goalEntrance.x, goalEntrance.y)
                val (x, y, facing) = goalPath[goalPath.size - 1]
                val pathToGoal: ArrayList<GridNode> = AStarSearch.run(x, y, facing, goalX, goalY)
                goalPath.addAll(pathToGoal)

                var pathCost = 0.0
                previousFacing = goalPath[0].facing

                for (node in goalPath) {
                    pathCost += node.f;
                    if (node.facing != previousFacing) pathCost += 500;
                    previousFacing = node.facing;
                }

                if (pathCost <= previousGoalCost) {
                    finalGoalPath = goalPath;
                    previousGoalCost = pathCost;
                }
            }

            path2.addAll(finalGoalPath)

            var cost2 = 0.0
            previousFacing = path2[0].facing

            for (node in path2) {
                cost2 += node.f;
                if (node.facing != previousFacing) cost2 += 500;
                previousFacing = node.facing;
            }

            var path: ArrayList<GridNode>
            var cost: Double

            if (cost2 <= cost1) {
                path = path2
                cost = cost2
            } else {
                path = path1
                cost = cost1
            }

            if (cost <= previousCost) {
                finalPath = path
                previousCost = cost
            }
        }

        val path = ArrayList<IntArray>()

        for ((x, y, facing) in finalPath) {
            path.add(intArrayOf(x, y, facing))
        }

        return path
    }
}