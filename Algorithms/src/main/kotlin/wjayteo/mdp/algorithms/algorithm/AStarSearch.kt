package wjayteo.mdp.algorithms.algorithm

import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.arena.Robot
import kotlin.math.abs

class AStarSearch {
    companion object {
        data class GridNode(val x: Int, val y: Int, var facing: Int, var direction: Int, var f: Double, var g: Double, var h: Double, var parentX: Int, var parentY: Int)

        fun run(startX: Int, startY: Int, startFacing: Int, goalX: Int, goalY: Int): ArrayList<GridNode> {
            if (Arena.isInvalidCoordinates(startX, startY, false)) return arrayListOf()
            if (Arena.isInvalidCoordinates(goalX, goalY, false)) return arrayListOf()

            var found = false
            val openedList: ArrayList<GridNode> = arrayListOf()
            val closedList: ArrayList<GridNode> = arrayListOf()
            val successors: ArrayList<GridNode> = arrayListOf()
            var parentNode = GridNode(startX, startY, startFacing, startFacing, 0.0, 0.0, 0.0, -1, -1)
            openedList.add(parentNode)

            while (openedList.isNotEmpty()) {
                successors.clear()
                var lowestCost: Double = Double.MAX_VALUE

                for (node in openedList) {
                    if (node.f >= lowestCost) continue
                    lowestCost = node.f
                    parentNode = node
                }

                openedList.remove(parentNode)
                closedList.add(parentNode)

                for (offset in -1..1 step 2) {
                    for (select in 0..1 step 1) {
                        var continueToNext = false
                        var facing: Int = if (offset == -1) (180 + (select * 90)) else (select * 90)
                        val direction = facing
                        if (abs(facing - parentNode.facing) == 180) facing = parentNode.facing
                        val x: Int = if (select == 0) parentNode.x else parentNode.x + offset
                        val y: Int = if (select == 0) parentNode.y + offset else parentNode.y
                        if (!Arena.isMovable(x, y)) continue

                        for (node in closedList) {
                            if (node.x == x && node.y == y) {
                                continueToNext = true
                                break
                            }
                        }

                        if (continueToNext) continue
                        successors.add(GridNode(x, y, facing, direction, 0.0, 0.0, 0.0, parentNode.x, parentNode.y))
                    }
                }

                for (successor in successors) {
                    var continueToNext = false

                    if (successor.x == goalX && successor.y == goalY) {
                        parentNode = successor
                        found = true
                        openedList.clear()
                        break
                    }

                    val penalty: Double = if (parentNode.facing == successor.facing) 1.0 else 1.5
                    successor.g = ((abs(successor.x - successor.parentX) + abs(successor.y - successor.parentY)) * penalty) + parentNode.g + (penalty - 1)
                    successor.h = 1.0 * (abs(successor.x - goalX) + abs(successor.y - goalY))
                    successor.f = successor.g + successor.h

                    for (node in openedList) {
                        if (node.x == successor.x && node.y == successor.y && successor.f < node.f) {
                            node.facing = successor.facing
                            node.direction = successor.direction
                            node.f = successor.f
                            node.g = successor.g
                            node.h = successor.h
                            node.parentX = successor.parentX
                            node.parentY = successor.parentY
                            continueToNext = true
                            break
                        }
                    }

                    if (continueToNext) continue
                    openedList.add(successor)
                }
            }

            val pathNodeList: ArrayList<GridNode> = arrayListOf()
            if (!found) return pathNodeList

            while (parentNode.parentX != -1 && parentNode.parentY != -1) {
                pathNodeList.add(parentNode)

                for (node in closedList) {
                    if (node.x == parentNode.parentX && node.y == parentNode.parentY) {
                        parentNode = node
                        break
                    }
                }
            }

            pathNodeList.reverse()
            return pathNodeList
        }
    }
}