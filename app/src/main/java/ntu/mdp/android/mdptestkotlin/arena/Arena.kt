package ntu.mdp.android.mdptestkotlin.arena

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import ntu.mdp.android.mdptestkotlin.App.Companion.testExplore
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.utils.GestureImageView

class Arena (private val context: Context,
             gridSize: Int,
             private val robotSize: Int,
             gestureCallback: (view: GestureImageView, gesture: GestureImageView.Gesture) -> Unit,
             private val controllerCallback: (status: Callback, message: String) -> Unit) {

    companion object {
        const val unexploredBit = 0
        const val exploredBit = 1
    }

    enum class Callback {
        INFO,
        WRITE,
        COORDINATES,
        ROBOT
    }

    enum class GridType {
        UNEXPLORED,
        EXPLORED,
        WAYPOINT,
        WAYPOINT_TOUCHED,
        START_POINT,
        GOAL_POINT,
        GOAL_POINT_TOUCHED,
        IMAGE,
        OBSTACLE,
        SEARCH_ADJACENT,
        SEARCH_PICKED
    }

    var robotCoordinates: IntArray = intArrayOf(-1, -1, 0)
    var wayPointCoordinates: Pair<Int, Int> = Pair(-1, -1)
    var startPointCoordinates: Pair<Int, Int> = Pair(-1, -1)
    var goalPointCoordinates: Pair<Int, Int> = Pair(-1, -1)
    val gridStateArray: Array<Array<Int>> = Array(20) { Array(15) { unexploredBit } }

    private val gridLayout: GridLayout = (context as Activity).findViewById(R.id.main_grid_arena)
    private val gridParent: RelativeLayout = (context as Activity).findViewById(R.id.gridParent)
    private var robot: ImageView = ImageView(context)
    private val gridArray: Array<Array<GestureImageView>> = Array(20) { Array(15) { GestureImageView(context) } }
    private val obstacleArray: Array<Array<Int>> = Array(20) { Array(15) { 0 } }

    init {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                val gestureImageView = GestureImageView(context)
                gestureImageView.setCallback(gestureCallback)
                gestureImageView.layoutParams = LinearLayout.LayoutParams(gridSize, gridSize).apply {
                    bottomMargin = 1
                    leftMargin = 1
                    rightMargin = 1
                    topMargin = 1
                }

                gestureImageView.tag = GridType.UNEXPLORED
                gestureImageView.setBackgroundColor(context.getColor(R.color.arena_unexplored))
                gestureImageView.isClickable = false
                gridArray[y][x] = gestureImageView
                gridLayout.addView(gestureImageView)
            }
        }

        robot = ImageView(context)
        robot.layoutParams = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3)
        robot.setBackgroundColor(Color.TRANSPARENT)
        robot.alpha = 0.0f
        gridParent.addView(robot)
    }

    fun reset() {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                gridArray[y][x].setImageResource(android.R.color.transparent)
                setGridColor(x, y, GridType.UNEXPLORED)
                gridStateArray[y][x] = unexploredBit
                obstacleArray[y][x] = 0
            }
        }

        setRobotStartGoalPoint(1, 1, isStart = true)
        setRobotStartGoalPoint(13, 18, isStart = false)
        wayPointCoordinates = Pair(-1, -1)
    }

    fun resetObstacles() {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                if (gridArray[y][x].tag == GridType.OBSTACLE)
                if (gridStateArray[y][x] == exploredBit) setGridColor(x, y, GridType.EXPLORED)
                else setGridColor(x, y, GridType.UNEXPLORED)
            }
        }
    }

    fun moveRobot(xInput: Int, yInput: Int, facing: Int) {
        val coordinates: Pair<Int, Int> = getValidCoordinates(xInput, yInput, isRobot = true)
        val x: Int = coordinates.first
        val y: Int = coordinates.second

        val yAnchor: Int = y + 1
        val xAnchor: Int = x - 1
        setRobotImage(facing)
        robot.layoutParams  = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3).apply {
            leftMargin = (xAnchor * robotSize)
            topMargin = (19 - yAnchor) * robotSize
        }

        robot.requestLayout()
        robot.alpha = 1.0f
        robotCoordinates[0] = x
        robotCoordinates[1] = y
        robotCoordinates[2] = facing
        controllerCallback(Callback.COORDINATES, "$x, $y")

        for (yOffset in -1..1) {
            for (xOffset in -1..1) {
                val xNew = x + xOffset
                val yNew = y + yOffset
                gridStateArray[yNew][xNew] = exploredBit
                plot(xNew, yNew, GridType.EXPLORED)
            }
        }

        if (testExplore) setExplored(x, y)

        if (x == wayPointCoordinates.first && y == wayPointCoordinates.second) {
            setWaypointTouched()
            controllerCallback(Callback.ROBOT, "Waypoint touched.")
        }

        if (x == goalPointCoordinates.first && y == goalPointCoordinates.second) {
            setGoalPointTouched()
            controllerCallback(Callback.ROBOT, "Waypoint touched.")
        }
    }

    private fun setExplored(x: Int, y: Int) {
        var xNew: Int
        var yNew: Int

        // CHECK TOP & BOTTOM
        for (offset in -1 .. 1) {
            for (count in -1 .. 1 step 2) {
                xNew = x + offset
                yNew = y + (2 * count)

                if (isValidCoordinates(xNew, yNew)) {
                    if (obstacleArray[yNew][xNew] == 1) {
                        plot(xNew, yNew, GridType.OBSTACLE)
                        gridStateArray[yNew][xNew] = 1
                    }
                }
            }
        }

        // CHECK LEFT & RIGHT
        for (offset in -1 .. 1) {
            for (count in -1 .. 1 step 2) {
                xNew = x + (2 * count)
                yNew = y + offset

                if (isValidCoordinates(xNew, yNew)) {
                    if (obstacleArray[yNew][xNew] == 1) {
                        plot(xNew, yNew, GridType.OBSTACLE)
                        gridStateArray[yNew][xNew] = 1
                    }
                }
            }
        }
    }

    fun setRobotStartGoalPoint(xInput: Int, yInput: Int, isStart: Boolean) {
        val coordinates: Pair<Int, Int> = getValidCoordinates(xInput, yInput, isRobot = true)
        val x: Int = coordinates.first
        val y: Int = coordinates.second
        val gridType: GridType = if (isStart) GridType.START_POINT else GridType.GOAL_POINT

        for (yOffset in -1 .. 1) {
            for (xOffset in -1 .. 1) {
                val xNew = x + xOffset
                val yNew = y + yOffset

                if (!isGridOfType(xNew, yNew, gridType) && isGridOccupied(xNew, yNew)) {
                    controllerCallback(Callback.INFO, context.getString(R.string.obstructed))
                    return
                }
            }
        }

        if ((isStart && isValidCoordinates(startPointCoordinates)) || (!isStart && isValidCoordinates(goalPointCoordinates))) {
            for (yOffset in -1..1) {
                for (xOffset in -1..1) {
                    val xOld: Int = if (isStart) startPointCoordinates.first + xOffset else goalPointCoordinates.first + xOffset
                    val yOld: Int = if (isStart) startPointCoordinates.second + yOffset else goalPointCoordinates.second + yOffset
                    if (isStart) {
                        //setGridColor(xOld, yOld, GridType.UNEXPLORED)
                        setGridColor(xOld, yOld, if (gridStateArray[yOld][xOld] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
                    } else {
                        setGridColor(xOld, yOld, if (gridStateArray[yOld][xOld] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
                    }
                }
            }
        }

        for (yOffset in -1 .. 1) {
            for (xOffset in -1 .. 1) {
                val xNew = x + xOffset
                val yNew = y + yOffset
                setGridColor(xNew, yNew, gridType)
            }
        }

        if (isStart) {
            moveRobot(x, y, 0)
            startPointCoordinates = Pair(x, y)
            controllerCallback(Callback.WRITE, "#startposition::$x, $y, 0")
        } else {
            goalPointCoordinates = Pair(x, y)
            controllerCallback(Callback.WRITE, "#goalposition::$x, $y")
        }
    }

    fun setWaypoint(xInput: Int, yInput: Int) {
        val coordinates: Pair<Int, Int> = getValidCoordinates(xInput, yInput, isRobot = true)
        val x: Int = coordinates.first
        val y: Int = coordinates.second

        if (x == wayPointCoordinates.first && y == wayPointCoordinates.second) {
            for (yOffset in -1 .. 1) {
                for (xOffset in -1 .. 1) {
                    val xOld: Int = wayPointCoordinates.first + xOffset
                    val yOld: Int = wayPointCoordinates.second + yOffset
                    setGridColor(xOld, yOld, if (gridStateArray[yOld][xOld] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
                }
            }

            wayPointCoordinates = Pair(-1, -1)
            controllerCallback(Callback.WRITE, "#waypoint::-1, -1")
            return
        }

        for (yOffset in -1 .. 1) {
            for (xOffset in -1 .. 1) {
                val xNew = x + xOffset
                val yNew = y + yOffset

                if (!isGridOfType(xNew, yNew, GridType.WAYPOINT) && isGridOccupied(xNew, yNew)) {
                    controllerCallback(Callback.INFO, context.getString(R.string.obstructed))
                    return
                }
            }
        }

        if (isValidCoordinates(wayPointCoordinates.first, wayPointCoordinates.second)) {
            for (yOffset in -1..1) {
                for (xOffset in -1..1) {
                    val xOld: Int = wayPointCoordinates.first + xOffset
                    val yOld: Int = wayPointCoordinates.second + yOffset
                    setGridColor(xOld, yOld, if (gridStateArray[yOld][xOld] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
                }
            }
        }

        for (yOffset in -1 .. 1) {
            for (xOffset in -1 .. 1) {
                val xNew = x + xOffset
                val yNew = y + yOffset
                setGridColor(xNew, yNew, GridType.WAYPOINT)
            }
        }

        wayPointCoordinates = Pair(x, y)
        controllerCallback(Callback.WRITE, "#waypoint::$x, $y")
    }

    fun plot(xInput: Int, yInput: Int, gridType: GridType) {
        if (gridType == GridType.START_POINT || gridType == GridType.WAYPOINT) return
        val coordinates: Pair<Int, Int> = getValidCoordinates(xInput, yInput, isRobot = false)
        val x = coordinates.first
        val y = coordinates.second

        when (gridType) {
            GridType.UNEXPLORED, GridType.EXPLORED -> {
                if (isGridOccupied(x, y)) return
                setGridColor(x, y, gridType)
            }

            GridType.OBSTACLE -> {
                if (isGridOfType(x, y, GridType.OBSTACLE)) return

                if (isGridOfType(x, y, GridType.SEARCH_PICKED) || isGridOfType(x, y, GridType.SEARCH_ADJACENT)) {
                    setGridColor(x, y, gridType)
                    return
                }

                if (isGridOccupied(x, y)) {
                    controllerCallback(Callback.INFO, context.getString(R.string.obstructed))
                    return
                }

                setGridColor(x, y, gridType)
            }

            GridType.SEARCH_ADJACENT, GridType.SEARCH_PICKED -> {
                if (isGridOfType(x, y, GridType.OBSTACLE)) {
                    return
                }

                setGridColor(x, y, gridType)
            }

            else -> return
        }
    }

    fun removeObstacle(xInput: Int, yInput: Int) {
        val coordinates: Pair<Int, Int> = getValidCoordinates(xInput, yInput, isRobot = false)
        val x = coordinates.first
        val y = coordinates.second
        if (!isGridOfType(x, y, GridType.OBSTACLE)) return
        setGridColor(x, y, if (gridStateArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
    }

    fun getCoordinatesOfView(view: GestureImageView): Pair<Int, Int> {
        var x = -1
        var y = -1

        for (testY in 0 until 20) {
            x = gridArray[testY].indexOf(view)

            if (x != -1) {
                y = testY
                break
            }
        }

        return Pair(x, y)
    }

    fun setImage(xInput: Int, yInput: Int, id: Int) {
        if (id < 1 || id > 15) return
        val coordinates: Pair<Int, Int> = getValidCoordinates(xInput, yInput, isRobot = false)
        val x = coordinates.first
        val y = coordinates.second
        val grid: GestureImageView = gridArray[y][x]
        grid.tag = GridType.IMAGE

        when (id) {
            1 -> grid.setImageResource(R.drawable.ic_01)
            2 -> grid.setImageResource(R.drawable.ic_02)
            3 -> grid.setImageResource(R.drawable.ic_03)
            4 -> grid.setImageResource(R.drawable.ic_04)
            5 -> grid.setImageResource(R.drawable.ic_05)
            6 -> grid.setImageResource(R.drawable.ic_06)
            7 -> grid.setImageResource(R.drawable.ic_07)
            8 -> grid.setImageResource(R.drawable.ic_08)
            9 -> grid.setImageResource(R.drawable.ic_09)
            10 -> grid.setImageResource(R.drawable.ic_10)
            11 -> grid.setImageResource(R.drawable.ic_11)
            12 -> grid.setImageResource(R.drawable.ic_12)
            13 -> grid.setImageResource(R.drawable.ic_13)
            14 -> grid.setImageResource(R.drawable.ic_14)
            15 -> grid.setImageResource(R.drawable.ic_15)
        }
    }

    private fun getValidCoordinates(xInput: Int, yInput: Int, isRobot: Boolean = false): Pair<Int, Int> {
        val lowerBound = if (isRobot) 1 else 0
        val yUpperBound = if (isRobot) 18 else 19
        val xUpperBound = if (isRobot) 13 else 14
        var x = xInput
        var y = yInput

        when {
            (xInput < lowerBound) -> x = lowerBound
            (yInput < lowerBound) -> y = lowerBound
            (xInput > xUpperBound) -> x = xUpperBound
            (yInput > yUpperBound) -> y = yUpperBound
        }

        return Pair(x, y)
    }

    private fun setGridColor(x: Int, y: Int, gridType: GridType) {
        if (!isValidCoordinates(x, y)) return
        val grid: GestureImageView = gridArray[y][x]
        grid.tag = gridType

        when (gridType) {
            GridType.START_POINT -> grid.setBackgroundColor(context.getColor(R.color.arena_start_point))
            GridType.GOAL_POINT -> grid.setBackgroundColor(context.getColor(R.color.arena_goal_point))
            GridType.WAYPOINT -> grid.setBackgroundColor(context.getColor(R.color.arena_way_point))
            GridType.OBSTACLE -> grid.setBackgroundColor(context.getColor(R.color.arena_obstacle))
            GridType.WAYPOINT_TOUCHED -> grid.setBackgroundColor(context.getColor(R.color.arena_way_point_touched))
            GridType.GOAL_POINT_TOUCHED -> grid.setBackgroundColor(context.getColor(R.color.arena_goal_point_touched))

            GridType.UNEXPLORED -> {
                gridStateArray[y][x] = unexploredBit
                grid.setBackgroundColor(context.getColor(R.color.arena_unexplored))
            }

            GridType.EXPLORED -> {
                gridStateArray[y][x] = exploredBit
                grid.setBackgroundColor(context.getColor(R.color.arena_explored))
            }

            GridType.SEARCH_ADJACENT -> grid.setBackgroundColor(Color.RED)
            GridType.SEARCH_PICKED -> grid.setBackgroundColor(Color.GREEN)

            else -> return
        }
    }

    private fun setWaypointTouched() {
        val x = wayPointCoordinates.first
        val y = wayPointCoordinates.second

        for (yOffset in -1 .. 1) {
            for (xOffset in -1 .. 1) {
                val xNew = x + xOffset
                val yNew = y + yOffset
                setGridColor(xNew, yNew, GridType.WAYPOINT_TOUCHED)
            }
        }
    }

    private fun setGoalPointTouched() {
        val x = goalPointCoordinates.first
        val y = goalPointCoordinates.second

        for (yOffset in -1 .. 1) {
            for (xOffset in -1 .. 1) {
                val xNew = x + xOffset
                val yNew = y + yOffset
                setGridColor(xNew, yNew, GridType.GOAL_POINT_TOUCHED)
            }
        }
    }

    fun saveObstacles() {
        for (y in 19 downTo 0) {
            for (x in 0 .. 14) {
                if (gridArray[y][x].tag == GridType.OBSTACLE) {
                    obstacleArray[y][x] = 1
                    setGridColor(x, y, GridType.UNEXPLORED)
                } else {
                    obstacleArray[y][x] = 0
                }
            }
        }
    }

    fun setRobotImage(facing: Int) {
        val drawable: Int = when (facing) {
            0 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_0_connected else R.drawable.ic_0
            90 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_90_connected else R.drawable.ic_90
            180 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_180_connected else R.drawable.ic_180
            else -> if (BluetoothController.isSocketConnected()) R.drawable.ic_270_connected else R.drawable.ic_270
        }

        robot.setImageResource(drawable)
    }

    fun isGridMovable(x: Int, y: Int): Boolean {
        if (!isValidCoordinates(x, y)) return false
        if (isGridOfType(x, y, GridType.OBSTACLE)) return false
        return true
    }

    fun isRobotMovable(x: Int, y: Int): Boolean {
        if (!isValidCoordinates(x, y)) return false
        if (isGridOfType(x, y, GridType.OBSTACLE)) return false

        for (yOffset in -1 .. 1) {
            for (xOffset in -1 .. 1) {
                if (!isValidCoordinates(x + xOffset, y + yOffset)) return false
                if (isGridOfType(x + xOffset, y + yOffset, GridType.OBSTACLE)) return false
            }
        }

        return true
    }

    fun isExplored(x: Int, y: Int, facing: Int, checkSingle: Boolean = false): Boolean {
        if (facing == 0 || facing == 180) {
            for (offset in -1 .. 1) {
                if (!isValidCoordinates(x + offset, y)) return true

                if (checkSingle) {
                    if (gridStateArray[y][x + offset] == 0) return false
                } else {
                    if (gridStateArray[y][x + offset] == 1) return true
                }
            }

            return checkSingle
        } else {
            for (offset in -1 .. 1) {
                if (!isValidCoordinates(x, y + offset)) return true

                if (checkSingle) {
                    if (gridStateArray[y + offset][x] == 0) return false
                } else {
                    if (gridStateArray[y + offset][x] == 1) return true
                }
            }

            return checkSingle
        }
    }

    fun resetGoalPoint() {
        val x = goalPointCoordinates.first
        val y = goalPointCoordinates.second

        for (yOffset in -1 .. 1) {
            for (xOffset in -1 .. 1) {
                val xNew = x + xOffset
                val yNew = y + yOffset
                setGridColor(xNew, yNew, GridType.GOAL_POINT)
            }
        }
    }

    private fun isGridOccupied(x: Int, y: Int): Boolean {
        val grid: GestureImageView = gridArray[y][x]
        return !(grid.tag == GridType.UNEXPLORED || grid.tag == GridType.EXPLORED)
    }

    private fun isGridOfType(x: Int, y: Int, gridType: GridType): Boolean = (gridArray[y][x].tag == gridType)
    private fun isValidCoordinates(coordinates: Pair<Int, Int>): Boolean = isValidCoordinates(coordinates.first, coordinates.second)
    fun isValidCoordinates(x: Int, y: Int): Boolean = !(x < 0 || y < 0 || x > 14 || y > 19)
    fun isWaypointSet(): Boolean = (wayPointCoordinates.first >= 0 && wayPointCoordinates.second >= 0)

    fun resetPathing() {
        for (y in 19 downTo 0) {
            for (x in 0 .. 14) {
                val grid = gridArray[y][x]
                if (grid.tag == GridType.SEARCH_PICKED || grid.tag == GridType.SEARCH_ADJACENT) {
                    setGridColor(x, y, if (gridStateArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
                }
            }
        }
    }
}