package ntu.mdp.android.mdptestkotlin.utils

import android.util.Log
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.App.Companion.allowDiagonalExploration
import ntu.mdp.android.mdptestkotlin.App.Companion.plotPathChosen
import ntu.mdp.android.mdptestkotlin.App.Companion.plotSearch
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationDelay
import ntu.mdp.android.mdptestkotlin.MainActivityController
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 *
 * WELCOME TO THE SCRATCHPAD.
 *
 * THIS CLASS IS USED FOR TESTING WEIRD AND STUPID IDEAS AND EXPERIMENTAL STUFF.
 *
 */

class ScratchPad(activityController: MainActivityController, private val activityCallback: (stop: Boolean) -> Unit) {
    private val arena = activityController.getArena()
    private var stop = false
    private var fastestPath = false

    fun stop() {
        stop = true
    }

    suspend fun exploration() = withContext(Dispatchers.Main) {
        stop = false
        fastestPath = allowDiagonalExploration
        var robotPosition: IntArray
        var x: Int
        var y: Int
        var r: Int
        var counter = 0
        var haveDelay = true

        robotPosition = arena.getRobotPosition()
        x = robotPosition[0]
        y = robotPosition[1]

        arena.turnRobot(
            when {
                arena.isRobotMovable(x, y + 1) -> 0
                arena.isRobotMovable(x + 1, y) -> 90
                arena.isRobotMovable(x, y - 1) -> 180
                else -> 270
            }
        )

        while (true) {
            if (haveDelay) delay(simulationDelay)
            if (stop) return@withContext

            haveDelay = true
            robotPosition = arena.getRobotPosition()
            x = robotPosition[0]
            y = robotPosition[1]
            r = robotPosition[2]
            if (attemptMove(x, y, r)) continue
            if (!arena.hasUnexploredGrid()) break

            val nearestUnexplored = nearestUnexploredGrid()
            val xTarget = nearestUnexplored.first
            val yTarget = nearestUnexplored.second
            if (counter >= 15) break

            if (!arena.isValidCoordinates(xTarget, yTarget)) {
                if (xTarget == -1) {
                    x = (yTarget % 15)
                    y = (yTarget / 15)
                    if (arena.isValidCoordinates(x, y)) arena.setExplored(x, y)
                    haveDelay = false
                }

                counter++
                continue
            }

            counter = 0
            arena.resetPathing()
            val path: List<IntArray> = findShortestPath(x, y, r, xTarget, yTarget).second

            if (path.isEmpty()) {
                counter++
                continue
            }

            for ((i, p) in path.withIndex()) {
                if (i != 0) delay(simulationDelay)
                if (stop) return@withContext
                robotPosition = arena.getRobotPosition()
                x = robotPosition[0]
                y = robotPosition[1]
                r = robotPosition[2]
                if (attemptMove(x, y, r)) break
                arena.moveRobot(p[0], p[1])
                break
            }

        }

        arena.resetPathing()
        goHome()
    }

    suspend fun fastestPath() = withContext(Dispatchers.Main) {
        stop = false
        fastestPath = allowDiagonalExploration

        val startPosition: IntArray = arena.getStartPosition()
        var startX = startPosition[0]
        var startY = startPosition[1]
        val waypointPosition: IntArray = arena.getWaypointPosition()
        var endX = waypointPosition[0]
        var endY = waypointPosition[1]
        var bestWaypointPath: List<IntArray> = listOf()

        val pathToWaypoint1: Pair<Double, List<IntArray>> = findShortestPath(startX, startY, 0, endX, endY)
        var cost1 = pathToWaypoint1.first
        var path1 = pathToWaypoint1.second
        val pathToWaypoint2: Pair<Double, List<IntArray>> = findShortestPath(startX, startY, 90, endX, endY)
        var cost2 = pathToWaypoint2.first
        var path2 = pathToWaypoint2.second

        startX = endX
        startY = endY
        val startR = if (bestWaypointPath.isEmpty()) 0 else bestWaypointPath.last()[2]
        val goalPosition: IntArray = arena.getGoalPosition()
        endX = goalPosition[0]
        endY = goalPosition[1]
        val goalPointPath = findShortestPath(startX, startY, startR, endX, endY).second

        path1 = path1 + goalPointPath
        path2 = path2 + goalPointPath
        var turns1 = 0
        var turns2 = 0
        var previousPathFacing = path1.first()[2]

        for (p in path1) {
            if (p[2] != previousPathFacing) turns1++
            previousPathFacing = p[2]
        }

        previousPathFacing = path2.first()[2]

        for (p in path2) {
            if (p[2] != previousPathFacing) turns2++
            previousPathFacing = p[2]
        }

        if (turns2 > turns1) cost2 += 100
        else if (turns1 > turns2) cost1 += 100
        bestWaypointPath = if (turns2 <= turns1) path2 else path1
        Log.e("TEST", "$turns1, $turns2, $cost1, $cost2")

        tracePath(bestWaypointPath)
        if (!stop) activityCallback(true)
    }

    private suspend fun attemptMove(x: Int, y: Int, r: Int): Boolean = withContext(Dispatchers.Main) {
        // CHECK IN CURRENT DIRECTION FIRST
        if (r == 0 || r == 180) {
            if (y <= 1) {
                if (checkBottom(x, y)) {
                    handleMove(180)
                    return@withContext true
                }

                if (checkUp(x, y)) {
                    handleMove(0)
                    return@withContext true
                }
            } else {
                if (checkUp(x, y)) {
                    handleMove(0)
                    return@withContext true
                }

                if (checkBottom(x, y)) {
                    handleMove(180)
                    return@withContext true
                }
            }
        }

        if (r == 90 || r == 270) {
            if (x >= 7) {
                if (checkRight(x, y)) {
                    handleMove(90)
                    return@withContext true
                }

                if (checkLeft(x, y)) {
                    handleMove(270)
                    return@withContext true
                }
            } else {
                if (checkLeft(x, y)) {
                    handleMove(270)
                    return@withContext true
                }

                if (checkRight(x, y)) {
                    handleMove(90)
                    return@withContext true
                }
            }
        }

        if (y <= 1) {
            // CHECK IN OTHER DIRECTIONS, WITH PRIORITY
            if (x >= 7) {
                if (r != 90 && checkRight(x, y)) {
                    handleMove(90)
                    return@withContext true
                }
            } else {
                if (r != 270 && checkLeft(x, y)) {
                    handleMove(270)
                    return@withContext true
                }
            }

            if (r != 180 && checkBottom(x, y)) {
                handleMove(180)
                return@withContext true
            }

            if (x < 7) {
                if (r != 90 && checkRight(x, y)) {
                    handleMove(90)
                    return@withContext true
                }
            } else {
                if (r != 270 && checkLeft(x, y)) {
                    handleMove(270)
                    return@withContext true
                }
            }

            if (r != 0 && checkUp(x, y)) {
                handleMove(0)
                return@withContext true
            }
        } else {
            if (x >= 7) {
                if (r != 90 && checkRight(x, y)) {
                    handleMove(90)
                    return@withContext true
                }
            } else {
                if (r != 270 && checkLeft(x, y)) {
                    handleMove(270)
                    return@withContext true
                }
            }

            if (r != 0 && checkUp(x, y)) {
                handleMove(0)
                return@withContext true
            }

            if (x < 7) {
                if (r != 90 && checkRight(x, y)) {
                    handleMove(90)
                    return@withContext true
                }
            } else {
                if (r != 270 && checkLeft(x, y)) {
                    handleMove(270)
                    return@withContext true
                }
            }

            if (r != 180 && checkBottom(x, y)) {
                handleMove(180)
                return@withContext true
            }
        }

        return@withContext false
    }

    private fun checkRight(x: Int, y: Int): Boolean {
        if (arena.isRobotMovable(x + 1, y) && !arena.isExplored(x + 2, y, 90, false)) {
            return true
        } else if (arena.isRobotMovable(x + 1, y) && !arena.isExplored(x + 2, y, 90, true)) {
            return true
        }

        return false
    }

    private fun checkBottom(x: Int, y: Int): Boolean {
        if (arena.isRobotMovable(x, y - 1) && !arena.isExplored(x, y - 2, 180, false)) {
            return true
        } else if (arena.isRobotMovable(x, y - 1) && !arena.isExplored(x, y - 2, 180, true)) {
            return true
        }

        return false
    }

    private fun checkUp(x: Int, y: Int): Boolean {
        if (arena.isRobotMovable(x, y + 1) && !arena.isExplored(x, y + 2, 0, false)) {
            return true
        } else if (arena.isRobotMovable(x, y + 1) && !arena.isExplored(x, y + 2, 0, true)) {
            return true
        }

        return false
    }

    private fun checkLeft(x: Int, y: Int): Boolean {
        if (arena.isRobotMovable(x - 1, y) && !arena.isExplored(x - 2, y, 270, false)) {
            return true
        } else if (arena.isRobotMovable(x - 1, y) && !arena.isExplored(x - 2, y, 270, true)) {
            return true
        }

        return false
    }

    private suspend fun handleMove(r: Int) = withContext(Dispatchers.Main) {
        val robotPosition: IntArray = arena.getRobotPosition()
        val x = robotPosition[0]
        val y = robotPosition[1]

        when (r) {
            0 -> arena.moveRobot(x, y + 1)
            45  -> arena.moveRobot(x + 1, y + 1)
            90 -> arena.moveRobot(x + 1, y)
            135 -> arena.moveRobot(x + 1, y - 1)
            180 -> arena.moveRobot(x, y - 1)
            225 -> arena.moveRobot(x - 1, y - 1)
            270 -> arena.moveRobot(x - 1, y)
            315 -> arena.moveRobot(x - 1, y + 1)
        }
    }

    private fun nearestUnexploredGrid(): Pair<Int, Int> {
        var shortestDistance = 999
        var coordinates: Pair<Int, Int> = Pair(-1, -1)

        for (y in 19 downTo 0) {
            for (x in 0 .. 14) {
                if (arena.isGridExplored(x, y)) continue
                val robotPosition: IntArray = arena.getRobotPosition()
                val distance = abs(x - robotPosition[0]) + abs(y - robotPosition[1])
                if (distance >= shortestDistance) continue
                shortestDistance = distance
                coordinates = Pair(x, y)
                continue
            }
        }

        if (arena.isRobotMovable(coordinates.first, coordinates.second)) return coordinates

        val x = coordinates.first
        val y = coordinates.second
        var found = false
        shortestDistance = 999

        for (yOffset in -1..1) {
            for (xOffset in -1..1) {
                val xNew: Int = x + xOffset
                val yNew: Int = y + yOffset
                val robotPosition: IntArray = arena.getRobotPosition()
                val distance = abs(xNew - robotPosition[0]) + abs(yNew - robotPosition[1])

                if (arena.isRobotMovable(xNew, yNew) && distance < shortestDistance) {
                    shortestDistance = distance
                    coordinates = Pair(xNew, yNew)
                    found = true
                }
            }
        }

        if (!found) coordinates = Pair(-1, 15 * y + x)
        return coordinates
    }

    private suspend fun tracePath(path: List<IntArray>) = withContext(Dispatchers.Main) {
        for ((i, p) in path.withIndex()) {
            if (i != 0) delay(simulationDelay)
            if (stop) return@withContext
            if (plotPathChosen) arena.setSelectedPath(p[0], p[1])
            arena.moveRobot(p[0], p[1])
        }
    }

    private suspend fun goHome() = withContext(Dispatchers.Main) {
        while (true) {
            if (stop) return@withContext
            val robotPosition: IntArray = arena.getRobotPosition()
            val startX = robotPosition[0]
            val startY = robotPosition[1]
            val startR = robotPosition[2]
            val startPosition: IntArray = arena.getStartPosition()
            val endX = startPosition[0]
            val endY = startPosition[1]
            val pathHome: Pair<Double, List<IntArray>> = findShortestPath(startX, startY, startR, endX, endY)
            val list = pathHome.second
            tracePath(list)
            val currentPosition = arena.getRobotPosition()
            if (currentPosition[0] != endX || currentPosition[1] != endY) continue
            break
        }

        if (!stop) activityCallback(true)
    }

    private fun findShortestPath(startX: Int, startY: Int, startR: Int, endX: Int, endY: Int): Pair<Double, List<IntArray>> {
        if (!arena.isValidCoordinates(startX, startY)) return Pair(-1.0, listOf())
        if (!arena.isValidCoordinates(endX, endY)) return Pair(-1.0, listOf())
        val openList: ArrayList<GridNode> = arrayListOf()
        val closedList: ArrayList<GridNode> = arrayListOf()
        val successors: ArrayList<GridNode> = arrayListOf()
        var parentNode = GridNode(startX, startY, 0.0, 0.0, 0.0, -1, -1, startR, startR)
        var smallestF: Double
        var found = false
        openList.add(parentNode)

        while (openList.isNotEmpty()) {
            smallestF = 999.0
            successors.clear()

            for (node in openList) {
                if (node.f <= smallestF) {
                    smallestF = node.f
                    parentNode = node
                }
            }

            if (plotSearch) arena.setSearchPath(parentNode.x, parentNode.y)
            openList.remove(parentNode)
            closedList.add(parentNode)

            if (fastestPath) {
                for (yOffset in -1 .. 1) {
                    for (xOffset in -1 .. 1) {
                        var continueToNext = false

                        var facing = when {
                            (yOffset == xOffset && yOffset == -1)   -> 225
                            (yOffset == xOffset && yOffset == 1)    -> 45
                            (xOffset == -1 && yOffset == 1)         -> 315
                            (xOffset == 1 && yOffset == -1)         -> 135
                            (xOffset == 0 && yOffset == -1)         -> 180
                            (xOffset == 0 && yOffset == 1)          -> 0
                            (yOffset == 0 && xOffset == -1)         -> 270
                            (yOffset == 0 && xOffset == 1)          -> 90
                            else                                    -> parentNode.facing
                        }

                        val direction = facing
                        if (facing - parentNode.facing == 180) facing = Math.floorMod(facing + 180, 360)
                        val gridNode = GridNode(parentNode.x + xOffset, parentNode.y + yOffset, 0.0, 0.0, 0.0, parentNode.x, parentNode.y, facing, direction)

                        if (arena.isRobotMovable(gridNode.x, gridNode.y)) {
                            for (node in closedList) if (node.x == gridNode.x && node.y == gridNode.y) {
                                continueToNext = true
                                break
                            }

                            if (!continueToNext) successors.add(gridNode)
                        }
                    }
                }
            } else {
                for (offset in -1..1 step 2) {
                    for (count in 0..1) {
                        var continueToNext = false
                        var facing = if (offset == -1) (180 + (count * 90)) else (0 + (count * 90))
                        val direction = facing
                        if (abs(facing - parentNode.facing) == 180) facing = Math.floorMod(facing + 180, 360)
                        val gridNode =
                            if (count == 0) GridNode(parentNode.x, parentNode.y + offset, 0.0, 0.0, 0.0, parentNode.x, parentNode.y, facing, direction)
                            else            GridNode(parentNode.x + offset, parentNode.y, 0.0, 0.0, 0.0, parentNode.x, parentNode.y, facing, direction)

                        if (arena.isRobotMovable(gridNode.x, gridNode.y)) {
                            for (node in closedList) if (node.x == gridNode.x && node.y == gridNode.y) {
                                continueToNext = true
                                break
                            }

                            if (!continueToNext) successors.add(gridNode)
                        }
                    }
                }
            }

            for (successor in successors) {
                var continueToNext = false

                if (successor.x == endX && successor.y == endY) {
                    parentNode = successor
                    found = true
                    openList.clear()
                    break
                }

                val penalty: Double = when {
                    parentNode.facing == successor.facing               -> 1.0
                    abs(parentNode.facing - successor.facing) <= 45     -> 10.0
                    abs(parentNode.facing - successor.facing) >= 315    -> 10.0
                    abs(parentNode.facing - successor.facing) == 90     -> 10.0
                    abs(parentNode.facing - successor.facing) == 270    -> 10.0
                    else                                                -> 10.0
                }

                if (fastestPath) {
                    successor.g = sqrt((1.0 * abs(successor.x - successor.parentX) + 1.0 * abs(successor.y - successor.parentY))) + parentNode.g + penalty
                    successor.h = max(abs(successor.x - endX), abs(successor.y - endY)).toDouble()
                    //successor.h = sqrt((1.0 * successor.x - endX).pow(2) + (1.0 * successor.y - endY).pow(2))
                } else {
                    successor.g = (abs(successor.x - successor.parentX) + abs(successor.y - successor.parentY) * penalty) + parentNode.g + (penalty - 1)
                    successor.h = 1.0 * (abs(successor.x - endX) + abs(successor.y - endY))
                }

                successor.f = successor.g + successor.h

                for (node in openList) {
                    if (node.x == successor.x && node.y == successor.y) {
                        if (successor.f < node.f) {
                            node.f = successor.f
                            node.g = successor.g
                            node.h = successor.h
                            node.parentX = successor.parentX
                            node.parentY = successor.parentY
                            continueToNext = true
                        }
                    }
                }

                if (continueToNext) continue
                openList.add(successor)
            }
        }

        val pathList: ArrayList<IntArray> = arrayListOf()
        var totalCost = 0.0

        if (found) {
            while (true) {
                val x = parentNode.x
                val y = parentNode.y
                if (plotPathChosen) arena.setSelectedPath(x, y)

                if (parentNode.parentX == -1 || parentNode.parentY == -1) break
                pathList.add(intArrayOf(x, y, parentNode.facing, parentNode.direction))
                totalCost += parentNode.f

                for (node in closedList) {
                    if (node.x == parentNode.parentX && node.y == parentNode.parentY) {
                        parentNode = node
                        break
                    }
                }
            }
        }

        val ret: List<IntArray> = pathList.reversed()
        return Pair(totalCost, ret)
    }
}



/*
IF DIAGONAL MOVEMENT IS ALLOWED, REPLACE WITH THIS

for (yOffset in -1 .. 1) {
    for (xOffset in -1 .. 1) {
        var con2 = false
        val gridNode = GridNode(parentNode.x + xOffset, parentNode.y + yOffset, 0.0, 0.0, 0, parentNode.x, parentNode.y)
        if (arena.isRobotMovable(gridNode.x, gridNode.y)) {
            for (node in closedList) {
                if (node.x == gridNode.x && node.y == gridNode.y) con2 = true
            }

            if (!con2) successors.add(gridNode)
        }

        con2 = false
        val gridNode2 = GridNode(parentNode.x + yOffset, parentNode.y + xOffset, 0.0, 0.0, 0, parentNode.x, parentNode.y)
        if (arena.isRobotMovable(gridNode2.x, gridNode2.y)) {
            for (node in closedList) {
                if (node.x == gridNode2.x && node.y == gridNode2.y) con2 = true
            }

            if (!con2) successors.add(gridNode2)
        }
    }
}


//successor.g = sqrt(1.0 * abs(successor.x - successor.parentX) + 1.0 * abs(successor.y - successor.parentY)) + parentNode.g
//successor.h = max(abs(successor.x - goalX), abs(successor.y - goalY))

 */