package ntu.mdp.android.mdptestkotlin.utils

import android.util.Log
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.MainActivityController
import ntu.mdp.android.mdptestkotlin.arena.Arena
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import kotlin.math.abs

/**
 *
 * WELCOME TO THE SCRATCHPAD.
 *
 * THIS CLASS IS USED FOR TESTING WEIRD AND STUPID IDEAS AND EXPERIMENTAL STUFF.
 *
 */

class ScratchPad(activityController: MainActivityController) {
    private val arenaController = activityController.arenaController
    private val arena = arenaController.arena
    private var stop = false
    private var bestFacing = 0
    private var plot = false
    private var plotSearch = false

    fun stop() {
        stop = true
    }

    fun exploration() {
        stop = false
        var x: Int
        var y: Int
        var r: Int
        var counter = 0
        findBestStartFacing()

        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(250L)
                if (stop) return@launch

                x = arena.robotCoordinates[0]
                y = arena.robotCoordinates[1]
                r = arena.robotCoordinates[2]
                if (attemptMove(x, y, r)) continue
                if (!hasUnexploredGrid()) break

                val nearestUnexplored = nearestUnexploredGrid()
                val xTarget = nearestUnexplored.first
                val yTarget = nearestUnexplored.second
                if (counter >= 15) break

                if (!arena.isValidCoordinates(xTarget, yTarget)) {
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
                    if (i != 0) delay(250L)
                    if (stop) return@launch
                    if (!arena.isRobotMovable(p[0], p[1])) break

                    x = arena.robotCoordinates[0]
                    y = arena.robotCoordinates[1]
                    r = arena.robotCoordinates[2]
                    if (attemptMove(x, y, r)) break

                    val requestedFacing: Int = getRequestedFacing(p[0], p[1])
                    val robotFacing = arenaController.getRobotFacing()
                    if (requestedFacing == -1) return@launch // gg

                    if (requestedFacing != robotFacing && abs(requestedFacing - robotFacing) != 180) {
                        handleMove(requestedFacing)
                        delay(500L)
                    }

                    handleMove(requestedFacing)
                }

                continue
            }

            goHome()
            arena.resetPathing()
        }
    }

    private fun findBestStartFacing() {
        val x = arena.robotCoordinates[0]
        val y = arena.robotCoordinates[1]

        // ALWAYS CHECK RIGHT FIRST
        bestFacing = when {
            arena.isRobotMovable(x + 1, y)  -> 90
            arena.isRobotMovable(x, y - 1)  -> 180
            arena.isRobotMovable(x, y + 1)  -> 0
            else                            -> 270
        }

        arena.moveRobot(x, y, bestFacing)
    }

    private fun attemptMove(x: Int, y: Int, r: Int): Boolean {
        Log.e("ATTEMPT MOVE", "$x, $y, $r")
        var requestedFacing = r

        // CHECK IN CURRENT DIRECTION FIRST
        var move = when (r) {
            180 -> checkBottom(x, y)
            90 -> checkRight(x, y)
            0 -> checkUp(x, y)
            else -> checkLeft(x, y)
        }

        if (move) {
            handleMove(requestedFacing)
            return move
        }

        // CHECK IN OTHER DIRECTIONS, WITH PRIORITY
        if (!move && r != 90) {
            requestedFacing = 90
            move = checkRight(x, y)
        }

        if (!move && r != 0) {
            requestedFacing = 0
            move = checkUp(x, y)
        }

        if (!move && r != 270) {
            requestedFacing = 270
            move = checkLeft(x, y)
        }

        if (!move && r != 180) {
            requestedFacing = 180
            move = checkBottom(x, y)
        }

        if (move) handleMove(requestedFacing)
        return move
    }

    private fun checkRight(x: Int, y: Int): Boolean {
        var move = false
        move = (arena.isRobotMovable(x + 1, y) && !arena.isExplored(x + 2, y, 90, false) && !move)
        if (!move) move = (arena.isRobotMovable(x + 1, y) && !arena.isExplored(x + 2, y, 90, true) && !move)
        return move
    }

    private fun checkBottom(x: Int, y: Int): Boolean {
        var move = false

        if (arena.isRobotMovable(x, y - 1) && !arena.isExplored(x, y - 2, 180, false) && !move) {
            move = true
        } else if (arena.isRobotMovable(x, y - 1) && !arena.isExplored(x, y - 2, 180, true) && !move) {
            move = true
        }

        return move
    }

    private fun checkUp(x: Int, y: Int): Boolean {
        var move = false

        if (arena.isRobotMovable(x, y + 1) && !arena.isExplored(x, y + 2, 0, false) && !move) {
            move = true
        } else if (arena.isRobotMovable(x, y + 1) && !arena.isExplored(x, y + 2, 0, true) && !move) {
            move = true
        }

        return move
    }

    private fun checkLeft(x: Int, y: Int): Boolean {
        var move = false

        if (arena.isRobotMovable(x - 1, y) && !arena.isExplored(x - 2, y, 270, false) && !move) {
            move = true
        } else if (arena.isRobotMovable(x - 1, y) && !arena.isExplored(x - 2, y, 270, true) && !move) {
            move = true
        }

        return move
    }

    private fun getRequestedFacing(endX: Int, endY: Int): Int {
        val startX = arena.robotCoordinates[0]
        val startY = arena.robotCoordinates[1]
        val xDiff = startX - endX
        val yDiff = startY - endY
        var requestedFacing = 0

        if (xDiff != 0 && yDiff != 0) return -1
        if (xDiff == 1) requestedFacing = 270
        if (xDiff == -1) requestedFacing = 90
        if (yDiff == 1) requestedFacing = 180
        if (yDiff == -1) requestedFacing = 0
        return requestedFacing
    }

    private fun handleMove(requestedFacing: Int) {
        val robotFacing = arenaController.getRobotFacing()
        val facingOffset: Int = robotFacing - requestedFacing
        if (requestedFacing == robotFacing)                     arenaController.moveRobot(1, BluetoothController.isSocketConnected())
        else if (abs(requestedFacing - robotFacing) == 180)     arenaController.moveRobot(-1, BluetoothController.isSocketConnected())
        else if (facingOffset == 90 || facingOffset == -270)    arenaController.turnRobot(-1, BluetoothController.isSocketConnected())
        else                                                    arenaController.turnRobot(1, BluetoothController.isSocketConnected())
    }

    private fun hasUnexploredGrid(): Boolean {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                if (arena.gridStateArray[y][x] == 0) return true
            }
        }

        return false
    }

    private fun nearestUnexploredGrid(): Pair<Int, Int> {
        var shortestDistance = 999
        var coordinates: Pair<Int, Int> = Pair(-1, -1)

        for (y in 19 downTo 0) {
            for (x in 0 .. 14) {
                if (arena.gridStateArray[y][x] != 0) continue
                val distance = abs(x - arena.robotCoordinates[0]) + abs(y - arena.robotCoordinates[1])
                if (distance >= shortestDistance) continue
                shortestDistance = distance
                coordinates = Pair(x, y)
                continue
            }
        }

        if (arena.isRobotMovable(coordinates.first, coordinates.second)) return coordinates

        val x = coordinates.first
        val y = coordinates.second
        shortestDistance = 999

        for (yOffset in -1..1) {
            for (xOffset in -1..1) {
                val xNew: Int = x + xOffset
                val yNew: Int = y + yOffset
                val distance = abs(xNew -  arena.robotCoordinates[0]) + abs(yNew - arena.robotCoordinates[1])

                if (arena.isRobotMovable(xNew, yNew) && distance < shortestDistance) {
                    shortestDistance = distance
                    coordinates = Pair(xNew, yNew)
                }
            }
        }

        return coordinates
    }

    private suspend fun tracePath(path: List<IntArray>) = withContext(Dispatchers.Main) {
        for ((i, p) in path.withIndex()) {
            if (i != 0) delay(250L)
            if (stop) return@withContext
            val requestedFacing: Int = getRequestedFacing(p[0], p[1])
            if (requestedFacing == -1) return@withContext // gg

            val robotFacing = arenaController.getRobotFacing()
            if (plot) arena.plot(p[0], p[1], Arena.GridType.SEARCH_PICKED)

            if (requestedFacing != robotFacing && abs(requestedFacing - robotFacing) != 180) {
                handleMove(requestedFacing)
                delay(500L)
            }

            handleMove(requestedFacing)
        }
    }

    fun fastestPath() {
        stop = false
        var startX = arena.startPointCoordinates.first
        var startY = arena.startPointCoordinates.second
        var endX = arena.wayPointCoordinates.first
        var endY = arena.wayPointCoordinates.second
        var smallestCost = 99999.0
        var bestWaypointPath: List<IntArray> = listOf()

        for (i in 0 .. 270 step 90) {
            arena.resetPathing()
            val pathToWaypoint: Pair<Double, List<IntArray>> = findShortestPath(startX, startY, i, endX, endY)
            val cost = pathToWaypoint.first

            if (cost < smallestCost) {
                smallestCost = cost
                bestWaypointPath = pathToWaypoint.second
            }
        }

        startX = endX
        startY = endY
        val startR = if (bestWaypointPath.isEmpty()) 0 else bestWaypointPath.last()[2]
        endX = arena.goalPointCoordinates.first
        endY = arena.goalPointCoordinates.second
        val goalPointPath = findShortestPath(startX, startY, startR, endX, endY).second

        CoroutineScope(Dispatchers.Main).launch {
            tracePath(bestWaypointPath)
            tracePath(goalPointPath)
            arena.resetPathing()
        }
    }

    private fun goHome() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                val startX = arena.robotCoordinates[0]
                val startY = arena.robotCoordinates[1]
                val startR = arena.robotCoordinates[2]
                val endX = arena.startPointCoordinates.first
                val endY = arena.startPointCoordinates.second
                val pathHome: Pair<Double, List<IntArray>> = findShortestPath(startX, startY, startR, endX, endY)
                val list = pathHome.second
                tracePath(list)
                val currentPosition = arena.robotCoordinates
                if (currentPosition[0] != endX || currentPosition[1] != endY) continue
                break
            }
        }
    }

    private fun findShortestPath(startX: Int, startY: Int, startR: Int, endX: Int, endY: Int): Pair<Double, List<IntArray>> {
        if (!arena.isValidCoordinates(startX, startY)) return Pair(-1.0, listOf())
        if (!arena.isValidCoordinates(endX, endY)) return Pair(-1.0, listOf())
        val openList: ArrayList<GridNode> = arrayListOf()
        val closedList: ArrayList<GridNode> = arrayListOf()
        val successors: ArrayList<GridNode> = arrayListOf()
        var parentNode = GridNode(startX, startY, 0.0, 0.0, 0, -1, -1, startR, startR)
        var smallestF: Double
        var found = false
        openList.add(parentNode)

        while (openList.isNotEmpty()) {
            smallestF = 999.0
            successors.clear()

            for (node in openList) {
                if (node.f < smallestF) {
                    smallestF = node.f
                    parentNode = node
                }
            }

            if (plotSearch) arena.plot(parentNode.x, parentNode.y, Arena.GridType.SEARCH_ADJACENT)
            openList.remove(parentNode)
            closedList.add(parentNode)

            for (offset in -1 .. 1 step 2) {
                for (count in 0 .. 1) {
                    var continueToNext = false
                    var facing = if (offset == -1) (180 + (count * 90)) else (0 + (count * 90))
                    val direction = facing
                    if (facing - parentNode.facing == 180) facing = Math.floorMod(facing + 180, 360)
                    val gridNode =
                        if (count == 0) GridNode(parentNode.x, parentNode.y + offset, 0.0, 0.0, 0, parentNode.x, parentNode.y, facing, direction)
                        else            GridNode(parentNode.x + offset, parentNode.y, 0.0, 0.0, 0, parentNode.x, parentNode.y, facing, direction)

                    if (arena.isRobotMovable(gridNode.x, gridNode.y)) {
                        for (node in closedList) if (node.x == gridNode.x && node.y == gridNode.y) continueToNext = true
                        if (!continueToNext) successors.add(gridNode)
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

                val penalty: Int = when {
                    parentNode.facing == successor.facing -> 0
                    abs(parentNode.facing - successor.facing) == 90 -> 3
                    abs(parentNode.facing - successor.facing) == 270 -> 3
                    else -> 0
                }

                successor.g = abs(successor.x - successor.parentX) + abs(successor.y - successor.parentY) + parentNode.g + penalty
                successor.h = abs(successor.x - endX) + abs(successor.y - endY)
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
                if (plot) arena.plot(x, y, Arena.GridType.SEARCH_PICKED)

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