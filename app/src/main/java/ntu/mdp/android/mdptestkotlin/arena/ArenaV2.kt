package ntu.mdp.android.mdptestkotlin.arena

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.FAST_SIM_DELAY
import ntu.mdp.android.mdptestkotlin.App.Companion.FORWARD_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.REVERSE_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.SLOW_SIM_DELAY
import ntu.mdp.android.mdptestkotlin.App.Companion.TURN_LEFT_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.TURN_RIGHT_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.fastSimulation
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationDelay
import ntu.mdp.android.mdptestkotlin.App.Companion.testExplore
import ntu.mdp.android.mdptestkotlin.App.Companion.usingAmd
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.utils.GestureImageView
import kotlin.math.abs

class ArenaV2 (private val context: Context, private val callback: (status: Callback, message: String) -> Unit) {

    companion object {
        const val unexploredBit = 0
        const val exploredBit = 1

        var isPlotting = false
        var isWaitingUpdate = false
        var currentPlotFunction: PlotFunction = PlotFunction.NONE
    }

    enum class PlotFunction {
        NONE,
        PLOT_OBSTACLE,
        REMOVE_OBSTACLE
    }

    enum class Callback {
        MESSAGE,
        SEND_COMMAND,
        UPDATE_COORDINATES,
        UPDATE_STATUS,
        LONG_PRESS_CHOICE,
        RESET_ARENA
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

    private val scale           : Double    = if (App.isSimple) 0.81 else 0.66
    private val displayPixels   : Int       = (context.resources.displayMetrics.widthPixels * scale).toInt()
    private val gridSize        : Int       = ((displayPixels - 30) / 15)
    private val robotSize       : Int       = (displayPixels / 15)

    private val robotPosition   : IntArray = intArrayOf(1, 1, 0)
    private val startPosition   : IntArray = intArrayOf(1, 1)
    private val goalPosition    : IntArray = intArrayOf(13, 18)
    private val waypointPosition: IntArray = intArrayOf(-1, -1)

    private val gridParent      : RelativeLayout    = (context as Activity).findViewById(R.id.gridParent)
    private val gridLayout      : GridLayout        = (context as Activity).findViewById(R.id.main_grid_arena)
    private val robotDisplay    : ImageView         = ImageView(context)

    private val gridTypeArray   : Array<Array<GridType>>            = Array(20) { Array(15) { GridType.UNEXPLORED }}
    private val exploreArray    : Array<Array<Int>>                 = Array(20) { Array(15) { unexploredBit }}
    private val obstacleArray   : Array<Array<Int>>                 = Array(20) { Array(15) { unexploredBit }}
    private val gridArray       : Array<Array<GestureImageView>>    = Array(20) { Array(15) { GestureImageView(context) }}


    private val undoActionList  : ArrayList<Pair<PlotFunction, IntArray>>   = arrayListOf()
    private val redoActionList  : ArrayList<Pair<PlotFunction, IntArray>>   = arrayListOf()
    private var viewOnHold      : GestureImageView                          = GestureImageView(context)

    private val gestureCallback : (view: GestureImageView, gesture: GestureImageView.Gesture) -> Unit = { view, gesture ->
        when (gesture) {
            GestureImageView.Gesture.SINGLE_TAP -> gridSingleTap(view)
            GestureImageView.Gesture.DOUBLE_TAP -> gridDoubleTap(view)
            GestureImageView.Gesture.LONG_PRESS -> gridLongPress(view)
            GestureImageView.Gesture.FLING_LEFT, GestureImageView.Gesture.FLING_RIGHT, GestureImageView.Gesture.FLING_DOWN, GestureImageView.Gesture.FLING_UP -> gridFling(view, gesture)
        }
    }

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

                gridTypeArray[y][x] = GridType.UNEXPLORED
                gestureImageView.setBackgroundColor(context.getColor(R.color.arena_unexplored))
                gestureImageView.isClickable = false
                gridArray[y][x] = gestureImageView
                gridLayout.addView(gestureImageView)
            }
        }

        robotDisplay.layoutParams = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3)
        robotDisplay.setBackgroundColor(Color.TRANSPARENT)
        robotDisplay.alpha = 1.0f
        gridParent.addView(robotDisplay)
        setStartPoint(1, 1)
        setGoalPoint(13, 18)
        updateRobotImage(0)
    }

    fun resetArena() {
        if (isValidCoordinates(waypointPosition, true)) setWaypoint(waypointPosition[0], waypointPosition[1])

        for (y in 19 downTo 0) {
            for (x in 0..14) {
                setUnexploredForced(x, y)
            }
        }

        setStartPoint(1, 1)
        setGoalPoint(13, 18)
        updateRobotImage(0)
    }

    fun updateArena(explorationData: String) {
        var counter = 0
        val exploredIndices: ArrayList<Pair<Int, Int>> = arrayListOf()
        var s: ArrayList<String> = arrayListOf(explorationData)

        if (explorationData.contains("//")) {
            s = ArrayList(explorationData.split("//"))
        }

        var skip = 2

        for (i in s[0].indices) {
            var binary: String = s[0][i].toString().toInt(16).toString(2)
            binary = binary.padStart(4, '0')

            for (j in binary.indices) {
                if (!usingAmd && skip > 0) {
                    skip--
                    continue
                }

                if (!usingAmd && i >= s[0].length - 2) continue
                val bit: Int = binary[j].toString().toInt()
                val y = Math.floorDiv(counter, 15)
                val x = (counter % 15)
                if (bit == exploredBit) setExplored(x, y) else setUnexplored(x, y)
                if (bit == exploredBit) exploredIndices.add(Pair(x, y))
                counter++
            }
        }

        if (s.size != 2) {
            return
        }

        counter = 0
        val bitLength: Int = s[1].length * 4
        var extraLength: Int = bitLength - exploredIndices.size

        for (i in s[1].indices) {
            var binary: String = s[1][i].toString().toInt(16).toString(2)
            binary = binary.padStart(4, '0')

            for (j in binary.indices) {
                val bit: Int = binary[j].toString().toInt()

                if (!testExplore && counter < extraLength) {
                    extraLength--
                    continue
                }

                val coordinates = if (testExplore) Pair(0, 0) else exploredIndices[counter]
                val x = if (testExplore) (counter % 15) else coordinates.first
                val y = if (testExplore) Math.floorDiv(counter, 15) else coordinates.second

                if (bit == 1) setObstacle(x, y)
                else removeObstacle(x, y)
                counter++
            }
        }
    }

    suspend fun turnRobot(facing: Int) = withContext(Dispatchers.Main) {
        updateRobotImage(facing)
        scan(robotPosition[0], robotPosition[1], facing)
    }

    suspend fun moveRobot(array: IntArray) = withContext(Dispatchers.Main) {
        val x = array[0]
        val y = array[1]
        moveRobot(x, y)
    }

    suspend fun moveRobot(direction: Int) = withContext(Dispatchers.Main) {
        val x = robotPosition[0]
        val y = robotPosition[1]

        when (direction) {
            0   -> moveRobot(x, y + 1)
            45  -> moveRobot(x + 1, y + 1)
            90  -> moveRobot(x + 1, y)
            135 -> moveRobot(x + 1, y - 1)
            180 -> moveRobot(x, y - 1)
            225 -> moveRobot(x - 1, y - 1)
            270 -> moveRobot(x - 1, y)
            315 -> moveRobot(x - 1, y + 1)
        }
    }

    suspend fun moveRobot(x1: Int, y1: Int) = withContext(Dispatchers.Main) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]

        if (!isRobotMovable(x, y)) {
            callback(Callback.UPDATE_STATUS, context.getString(R.string.blocked))
            return@withContext
        }

        val currentX: Int = robotPosition[0]
        val currentY: Int = robotPosition[1]
        val currentFacing: Int = robotPosition[2]

        val positionDifferenceX: Int = (x - currentX)
        val positionDifferenceY: Int = (y - currentY)
        val direction: Int = when {
            (positionDifferenceX == 0 && positionDifferenceY == 1)                      -> 0
            (positionDifferenceX == positionDifferenceY && positionDifferenceX == 1)    -> 45
            (positionDifferenceX == 1 && positionDifferenceY == 0)                      -> 90
            (positionDifferenceX == 1 && positionDifferenceY == -1)                     -> 135
            (positionDifferenceX == 0 && positionDifferenceY == -1)                     -> 180
            (positionDifferenceX == positionDifferenceY && positionDifferenceX == -1)   -> 225
            (positionDifferenceX == -1 && positionDifferenceY == 0)                     -> 270
            (positionDifferenceX == -1 && positionDifferenceY == 1)                     -> 315
            else -> currentFacing
        }

        var facing: Int = direction

        if (BluetoothController.isSocketConnected()) {
            val facingOffset: Int = currentFacing - facing

            if (facing == currentFacing) {
                callback(Callback.SEND_COMMAND, FORWARD_COMMAND)
            } else if (abs(facing - currentFacing) == 180) {
                callback(Callback.SEND_COMMAND, REVERSE_COMMAND)
            } else if (facingOffset == 90 || facingOffset == -270) {
                callback(Callback.SEND_COMMAND, TURN_LEFT_COMMAND)
            } else {
                callback(Callback.SEND_COMMAND, TURN_RIGHT_COMMAND)
            }

            return@withContext
        }

        val facingDifference = abs(facing - currentFacing)
        if (facingDifference == 180) facing = currentFacing

        if (facing != currentFacing) {
            callback(Callback.UPDATE_STATUS, context.getString(R.string.turning))
            updateRobotImage(facing)
            robotPosition[2] = facing
            delay(simulationDelay)
        }

        callback(Callback.UPDATE_STATUS, context.getString(R.string.moving))
        val anchorX = x - 1
        val anchorY = y + 1

        robotDisplay.layoutParams  = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3).apply {
            leftMargin = (anchorX * robotSize)
            topMargin = (19 - anchorY) * robotSize
        }

        robotDisplay.requestLayout()
        robotPosition[0] = x
        robotPosition[1] = y
        robotPosition[2] = facing
        callback(Callback.UPDATE_COORDINATES, "$x, $y")

        for (offsetY in -1..1) {
            for (offsetX in -1..1) {
                val newX = x + offsetX
                val newY = y + offsetY
                setExplored(newX, newY)
            }
        }

        if (testExplore) scan(x, y, facing)
        if (x == waypointPosition[0] && y == waypointPosition[1]) setWaypointTouched()
        else if (x == goalPosition[0] && y == goalPosition[1]) setGoalPointTouched()
    }

    fun updateRobotImage(facing: Int = robotPosition[2]) {
        val drawable: Int = when (facing) {
            45 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_45_connected else R.drawable.ic_45
            90 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_90_connected else R.drawable.ic_90
            135 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_135_connected else R.drawable.ic_135
            180 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_180_connected else R.drawable.ic_180
            225 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_225_connected else R.drawable.ic_225
            270 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_270_connected else R.drawable.ic_270
            315 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_315_connected else R.drawable.ic_315
            else -> if (BluetoothController.isSocketConnected()) R.drawable.ic_0_connected else R.drawable.ic_0
        }

        robotDisplay.setImageResource(drawable)
        robotPosition[2] = facing
    }

    fun setUnexploredForced(x: Int, y: Int) {
        if (isValidCoordinates(x, y)) {
            exploreArray[y][x] = unexploredBit
            plot(x, y, GridType.UNEXPLORED)
        }
    }

    fun setUnexplored(x: Int, y: Int) {
        if (isValidCoordinates(x, y)) {
            exploreArray[y][x] = unexploredBit
            if (!isOccupied(x, y)) plot(x, y, GridType.UNEXPLORED)
        }
    }

    fun setExplored(x: Int, y: Int) {
        if (isValidCoordinates(x, y)) {
            exploreArray[y][x] = exploredBit
            if (!isOccupied(x, y)) plot(x, y, GridType.EXPLORED)
        }
    }

    fun setObstacle(x: Int, y: Int) {
        if (isValidCoordinates(x, y)) plot(x, y, GridType.OBSTACLE)
        else callback(Callback.MESSAGE, context.getString(R.string.obstructed))
    }

    fun removeObstacle(x: Int, y: Int) {
        if (isValidCoordinates(x, y) && isObstacle(x, y)) plot(x, y, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
    }

    fun setSelectedPath(x: Int, y: Int) {
        if (isValidCoordinates(x, y) && !isOccupied(x, y)) plot(x, y, GridType.SEARCH_PICKED)
    }

    fun setSearchPath(x: Int, y: Int) {
        if (isValidCoordinates(x, y) && !isOccupied(x, y)) plot(x, y, GridType.SEARCH_ADJACENT)
    }

    fun setStartPoint(x1: Int, y1: Int) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]
        if (isStartPoint(x, y)) return

        if (isValidCoordinates(startPosition, true)) {
            plotThree(startPosition, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
        }

        plot(x, y, GridType.START_POINT)
        startPosition[0] = x
        startPosition[1] = y

        CoroutineScope(Dispatchers.Main).launch {
            moveRobot(x, y)
        }
    }

    fun setWaypoint(x1: Int, y1: Int) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]

        if (isWaypoint(x, y)) {
            plotThree(waypointPosition, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
            waypointPosition[0] = -1
            waypointPosition[1] = -1
            return
        }

        if (isValidCoordinates(waypointPosition, true)) {
            plotThree(waypointPosition, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
        }

        plot(x, y, GridType.WAYPOINT)
        waypointPosition[0] = x
        waypointPosition[1] = y
    }

    fun setGoalPoint(x1: Int, y1: Int) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]
        if (isGoalPoint(x, y)) return

        if (isValidCoordinates(goalPosition, true)) {
            plotThree(goalPosition, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
        }

        plot(x, y, GridType.GOAL_POINT)
        goalPosition[0] = x
        goalPosition[1] = y
    }

    fun setWaypointTouched() {
        if (isValidCoordinates(waypointPosition, true)) plot(waypointPosition, GridType.WAYPOINT_TOUCHED)
    }

    fun setGoalPointTouched() {
        if (isValidCoordinates(goalPosition, true)) plot(goalPosition, GridType.GOAL_POINT_TOUCHED)
    }

    fun resetWaypoint() {
        if (isValidCoordinates(waypointPosition, true)) plot(waypointPosition, GridType.WAYPOINT)
    }

    fun resetGoalPoint() {
        if (isValidCoordinates(goalPosition, true)) plot(goalPosition, GridType.GOAL_POINT)
    }

    fun resetPathing() {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                if (gridTypeArray[y][x] == GridType.SEARCH_ADJACENT || gridTypeArray[y][x] == GridType.SEARCH_PICKED) {
                    if (exploreArray[y][x] == exploredBit) setExplored(x, y) else setUnexploredForced(x, y)
                }
            }
        }
    }

    fun selectPoint(isStart: Boolean) {
        val coordinates: IntArray = getCoordinatesOfView(viewOnHold)
        if (isStart) setStartPoint(coordinates[0], coordinates[1])
        else setGoalPoint(coordinates[0], coordinates[1])
    }

    fun setImage(x1: Int, y1: Int, id: Int) {
        if (id < 1 || id > 15) return
        val coordinates: IntArray = getValidCoordinates(x1, y1)
        val x = coordinates[0]
        val y = coordinates[1]
        val grid: GestureImageView = gridArray[y][x]
        gridTypeArray[y][x] = GridType.IMAGE

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

    fun isRobotMovable(x: Int, y: Int): Boolean {
        for (yOffset in -1 .. 1) {
            for (xOffset in -1 .. 1) {
                if (!isValidCoordinates(x + xOffset, y + yOffset)) return false
                if (obstacleArray[y + yOffset][x + xOffset] == 1) return false
            }
        }

        return true
    }

    fun isExplored(x: Int, y: Int, facing: Int, checkSingle: Boolean = false): Boolean {
        if (facing == 0 || facing == 180) {
            for (offset in -1 .. 1) {
                if (!isValidCoordinates(x + offset, y)) return true

                if (checkSingle) {
                    if (exploreArray[y][x + offset] == unexploredBit) return false
                } else {
                    if (exploreArray[y][x + offset] == exploredBit) return true
                }
            }

            return checkSingle
        } else {
            for (offset in -1 .. 1) {
                if (!isValidCoordinates(x, y + offset)) return true

                if (checkSingle) {
                    if (exploreArray[y + offset][x] == unexploredBit) return false
                } else {
                    if (exploreArray[y + offset][x] == exploredBit) return true
                }
            }

            return checkSingle
        }
    }

    private fun scan(x: Int, y: Int, facing: Int) {
        if (Math.floorMod(facing, 90) != 0) return
        scanFront(x, y, facing)
        scanRight(x, y, facing)
        scanLeft(x, y, facing)
    }

    private fun scanFront(x: Int, y: Int, facing: Int) {
        for (offset in -1..1) {
            val newX: Int
            val newY: Int

            when (facing) {
                0 -> {
                    newX = x + offset
                    newY = y + 2
                }

                90 -> {
                    newX = x + 2
                    newY = y + offset
                }

                180 -> {
                    newX = x + offset
                    newY = y - 2
                }

                else -> {
                    newX = x - 2
                    newY = y + offset
                }
            }

            if (!isValidCoordinates(newX, newY)) continue

            if (obstacleArray[newY][newX] == 1) {
                setObstacle(newX, newY)
                setExplored(newX, newY)
            }
        }

        scanUnreachable(x, y, facing)
    }

    private fun scanRight(x: Int, y: Int, facing: Int) {
        for (offset in -1..1) {
            val newX: Int
            val newY: Int

            when (facing) {
                0 -> {
                    newX = x + 2
                    newY = y + offset
                }

                90 -> {
                    newX = x + offset
                    newY = y - 2
                }

                180 -> {
                    newX = x - 2
                    newY = y + offset
                }

                else -> {
                    newX = x + offset
                    newY = y + 2
                }
            }

            if (!isValidCoordinates(newX, newY)) continue

            if (obstacleArray[newY][newX] == 1) {
                setObstacle(newX, newY)
                setExplored(newX, newY)
            }
        }
    }

    private fun scanLeft(x: Int, y: Int, facing: Int) {
        for (offset in -1..1) {
            val newX: Int
            val newY: Int

            when (facing) {
                0 -> {
                    newX = x - 2
                    newY = y + offset
                }

                90 -> {
                    newX = x + offset
                    newY = y + 2
                }

                180 -> {
                    newX = x + 2
                    newY = y + offset
                }

                else -> {
                    newX = x + offset
                    newY = y - 2
                }
            }

            if (!isValidCoordinates(newX, newY)) continue

            if (obstacleArray[newY][newX] == 1) {
                setObstacle(newX, newY)
                setExplored(newX, newY)
            }
        }
    }

    private fun scanUnreachable(x: Int, y: Int, facing: Int) {
        if ((facing != 180) && y >= 15 && y < 18) {
            if (facing != 90 && x == 1 && !isRobotMovable(x, y + 2)) {
                var cutoffX = -1

                if (obstacleArray[y + 2][x + 1] == 1) cutoffX = x
                else if (obstacleArray[y + 2][x] == 1) cutoffX = x - 1

                if (cutoffX != -1) {
                    for (offsetY in 2..4) {
                        for (offsetX in (x - 1)..cutoffX) {
                            setExplored(offsetX, y + offsetY)
                        }
                    }
                }
            }

            if (facing != 270 && x == 13 && !isRobotMovable(x, y + 2)) {
                var cutoffX = -1

                if (obstacleArray[y + 2][x - 1] == 1) cutoffX = x
                else if (obstacleArray[y + 2][x] == 1) cutoffX = x + 1

                if (cutoffX != -1) {
                    for (offsetY in 2..4) {
                        for (offsetX in (x + 1) downTo cutoffX) {
                            setExplored(offsetX, y + offsetY)
                        }
                    }
                }
            }
        }

        if (facing != 0 && y <= 4 && y > 1) {
            if (facing != 90 && x == 1 && !isRobotMovable(x, y - 2)) {
                var cutoffX = -1

                if (obstacleArray[y - 2][x + 1] == 1) cutoffX = x
                else if (obstacleArray[y - 2][x] == 1) cutoffX = x - 1

                if (cutoffX != -1) {
                    for (offsetY in 2..4) {
                        for (offsetX in (x - 1)..cutoffX) {
                            setExplored(offsetX, y - offsetY)
                        }
                    }
                }
            }

            if (facing != 270 && x == 13 && !isRobotMovable(x, y - 2)) {
                var cutoffX = -1

                if (obstacleArray[y - 2][x - 1] == 1) cutoffX = x
                else if (obstacleArray[y - 2][x] == 1) cutoffX = x + 1

                if (cutoffX != -1) {
                    for (offsetY in 2..4) {
                        for (offsetX in (x + 1) downTo cutoffX) {
                            setExplored(offsetX, y - offsetY)
                        }
                    }
                }
            }
        }

        if (facing != 270 && x >= 10 && x < 13) {
            if (facing != 0 && y == 1 && !isRobotMovable(x + 2, y)) {
                var cutoffY = -1

                if (obstacleArray[y + 1][x + 2] == 1) cutoffY = y
                else if (obstacleArray[y][x + 2] == 1) cutoffY = y - 1

                if (cutoffY != -1) {
                    for (offsetY in (y - 1)..cutoffY) {
                        for (offsetX in 2..4) {
                            setExplored(x + offsetX, offsetY)
                        }
                    }
                }
            }

            if (facing != 180 && y == 18 && !isRobotMovable(x + 2, y)) {
                var cutoffY = -1

                if (obstacleArray[y - 1][x + 2] == 1) cutoffY = y
                else if (obstacleArray[y][x + 2] == 1) cutoffY = y + 1

                if (cutoffY != -1) {
                    for (offsetY in (y + 1) downTo cutoffY) {
                        for (offsetX in 2..4) {
                            setExplored(x + offsetX, offsetY)
                        }
                    }
                }
            }
        }

        if (facing != 90 && x <= 4 && x > 1) {
            if (facing != 0 && y == 1 && !isRobotMovable(x - 2, y)) {
                var cutoffY = -1

                if (obstacleArray[y + 1][x - 2] == 1) cutoffY = y
                else if (obstacleArray[y][x - 2] == 1) cutoffY = y - 1

                if (cutoffY != -1) {
                    for (offsetY in (y - 1)..cutoffY) {
                        for (offsetX in 2..4) {
                            setExplored(x - offsetX, offsetY)
                        }
                    }
                }
            }

            if (facing != 180 && y == 18 && !isRobotMovable(x - 2, y)) {
                var cutoffY = -1

                if (obstacleArray[y - 1][x - 2] == 1) cutoffY = y
                else if (obstacleArray[y][x - 2] == 1) cutoffY = y + 1

                if (cutoffY != -1) {
                    for (offsetY in (y + 1) downTo cutoffY) {
                        for (offsetX in 2..4) {
                            setExplored(x - offsetX, offsetY)
                        }
                    }
                }
            }
        }
    }

    private fun plot(array: IntArray, gridType: GridType): Boolean {
        if (array.size != 2) return false
        val x = array[0]
        val y = array[1]
        return plot(x, y, gridType)
    }

    private fun plotThree(array: IntArray, gridType: GridType): Boolean {
        if (array.size != 2) return false
        val x = array[0]
        val y = array[1]
        return plotThree(x, y, gridType)
    }

    private fun plot(x: Int, y: Int, gridType: GridType): Boolean {
        if (!isValidCoordinates(x, y)) return false
        val grid: GestureImageView = gridArray[y][x]
        gridTypeArray[y][x] = gridType
        obstacleArray[y][x] = if (gridType == GridType.OBSTACLE) 1 else 0
        grid.setImageResource(android.R.color.transparent)

        when(gridType) {
            GridType.UNEXPLORED         -> grid.setBackgroundColor(context.getColor(R.color.arena_unexplored))
            GridType.EXPLORED           -> grid.setBackgroundColor(context.getColor(R.color.arena_explored))
            GridType.OBSTACLE           -> grid.setBackgroundColor(context.getColor(R.color.arena_obstacle))
            GridType.SEARCH_PICKED      -> grid.setBackgroundColor(Color.GREEN)
            GridType.SEARCH_ADJACENT    -> grid.setBackgroundColor(Color.RED)

            GridType.START_POINT, GridType.GOAL_POINT, GridType.GOAL_POINT_TOUCHED, GridType.WAYPOINT, GridType.WAYPOINT_TOUCHED -> {
                if (!isValidCoordinates(x, y, true)) return false
                return plotThree(x, y, gridType)
            }

            else -> return false
        }

        return true
    }

    private fun plotThree(x: Int, y: Int, gridType: GridType): Boolean {
        for (offsetY in -1 .. 1) {
            for (offsetX in -1..1) {
                val newX = x + offsetX
                val newY = y + offsetY
                val grid = gridArray[newY][newX]
                gridTypeArray[newY][newX] = gridType
                obstacleArray[newY][newX] = 0
                grid.setImageResource(android.R.color.transparent)

                when(gridType) {
                    GridType.UNEXPLORED         -> grid.setBackgroundColor(context.getColor(R.color.arena_unexplored))
                    GridType.EXPLORED           -> grid.setBackgroundColor(context.getColor(R.color.arena_explored))
                    GridType.OBSTACLE           -> grid.setBackgroundColor(context.getColor(R.color.arena_obstacle))
                    GridType.SEARCH_PICKED      -> grid.setBackgroundColor(Color.GREEN)
                    GridType.SEARCH_ADJACENT    -> grid.setBackgroundColor(Color.RED)
                    GridType.START_POINT        -> grid.setBackgroundColor(context.getColor(R.color.arena_start_point))
                    GridType.GOAL_POINT         -> grid.setBackgroundColor(context.getColor(R.color.arena_goal_point))
                    GridType.GOAL_POINT_TOUCHED -> grid.setBackgroundColor(context.getColor(R.color.arena_goal_point_touched))
                    GridType.WAYPOINT           -> grid.setBackgroundColor(context.getColor(R.color.arena_way_point))
                    GridType.WAYPOINT_TOUCHED   -> grid.setBackgroundColor(context.getColor(R.color.arena_way_point_touched))
                    else -> return false
                }
            }
        }

        return true
    }

    fun saveObstacles() {
        for (y in 19 downTo 0) {
            for (x in 0 .. 14) {
                if (obstacleArray[y][x] == 1) {
                    setUnexploredForced(x, y)
                    obstacleArray[y][x] = 1
                } else {
                    obstacleArray[y][x] = 0
                }
            }
        }
    }

    fun clearObstacles() {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                if (obstacleArray[y][x] == 1) {
                    plot(x, y, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
                }
            }
        }
    }

    private fun gridSingleTap(view: GestureImageView) {
        if (isPlotting && (currentPlotFunction == PlotFunction.PLOT_OBSTACLE || currentPlotFunction == PlotFunction.REMOVE_OBSTACLE)) {
            val coordinates: IntArray = getCoordinatesOfView(view)
            val x: Int = coordinates[0]
            val y: Int = coordinates[1]

            if (currentPlotFunction == PlotFunction.PLOT_OBSTACLE) {
                setObstacle(x, y)
                undoActionList.add(Pair(PlotFunction.PLOT_OBSTACLE, intArrayOf(x, y)))
            } else {
                removeObstacle(x, y)
                undoActionList.add(Pair(PlotFunction.REMOVE_OBSTACLE, intArrayOf(x, y)))
            }
        }
    }

    private fun gridDoubleTap(view: GestureImageView) {
        if (isPlotting && (currentPlotFunction == PlotFunction.PLOT_OBSTACLE || currentPlotFunction == PlotFunction.REMOVE_OBSTACLE)) {
            gridSingleTap(view)
            return
        }

        val coordinates: IntArray = getCoordinatesOfView(view)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]
        setWaypoint(x, y)
    }

    private fun gridLongPress(view: GestureImageView) {
        callback(Callback.LONG_PRESS_CHOICE, "")
        viewOnHold = view
    }

    private fun gridFling(view: GestureImageView, gesture: GestureImageView.Gesture) {
        if (isPlotting && (currentPlotFunction == PlotFunction.PLOT_OBSTACLE || currentPlotFunction == PlotFunction.REMOVE_OBSTACLE)) {
            if (gesture == GestureImageView.Gesture.FLING_LEFT) undoAction()
            else redoAction()
            return
        }

        if (gesture == GestureImageView.Gesture.FLING_DOWN) {
            if (!autoUpdateArena && !isPlotting) {
                isWaitingUpdate = true
                callback(Callback.SEND_COMMAND, SEND_ARENA_COMMAND)
            }

            return
        }

        if (gesture == GestureImageView.Gesture.FLING_UP) {
            fastSimulation = !fastSimulation
            sharedPreferences.edit().putBoolean(context.getString(R.string.app_pref_fast_simulation), fastSimulation).apply()

            if (fastSimulation) {
                simulationDelay = FAST_SIM_DELAY
                callback(Callback.MESSAGE, context.getString(R.string.fast_simulation_on))
            } else {
                simulationDelay = SLOW_SIM_DELAY
                callback(Callback.MESSAGE, context.getString(R.string.fast_simulation_off))
            }

            return
        }

        callback(Callback.RESET_ARENA, "")
    }

    private fun undoAction() {
        if (undoActionList.isNotEmpty()) {
            val index: Int = undoActionList.size - 1
            val action: Pair<PlotFunction, IntArray> = undoActionList.removeAt(index)
            val x: Int = action.second[0]
            val y: Int = action.second[1]

            if (action.first == PlotFunction.PLOT_OBSTACLE) removeObstacle(x, y)
            else setObstacle(x, y)
            redoActionList.add(action)
        }
    }

    private fun redoAction() {
        if (redoActionList.isNotEmpty()) {
            val index = redoActionList.size - 1
            val action: Pair<PlotFunction, IntArray> = redoActionList.removeAt(index)
            val x: Int = action.second[0]
            val y: Int = action.second[1]

            if (action.first == PlotFunction.REMOVE_OBSTACLE) {
                removeObstacle(x, y)
                undoActionList.add(Pair(PlotFunction.REMOVE_OBSTACLE, intArrayOf(x, y)))
            } else {
                setObstacle(x, y)
                undoActionList.add(Pair(PlotFunction.PLOT_OBSTACLE, intArrayOf(x, y)))
            }
        }
    }

    fun resetActions() {
        undoActionList.clear()
        redoActionList.clear()
    }

    fun isWaypoint(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.WAYPOINT || gridType == GridType.WAYPOINT_TOUCHED)
    }

    fun isStartPoint(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.START_POINT)
    }

    fun isGoalPoint(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.GOAL_POINT || gridType == GridType.GOAL_POINT_TOUCHED)
    }

    fun isImage(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.IMAGE)
    }

    fun isObstacle(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.OBSTACLE)
    }

    fun isSearchMarkings(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.SEARCH_ADJACENT || gridType == GridType.SEARCH_PICKED)
    }

    fun isValidCoordinates(array: IntArray, forRobot: Boolean = false): Boolean {
        if (array.size != 2) return false
        val x = array[0]
        val y = array[1]
        return isValidCoordinates(x, y, forRobot)
    }

    fun getValidCoordinates(x: Int, y: Int, forRobot: Boolean = false): IntArray {
        val lowerBound: Int = if (forRobot) 1 else 0
        val upperBoundX: Int = if (forRobot) 13 else 14
        val upperBoundY: Int = if (forRobot) 18 else 19
        var retX: Int = x
        var retY: Int = y

        if (retX < lowerBound) retX = lowerBound
        else if (retX > upperBoundX) retX = upperBoundX

        if (retY < lowerBound) retY = lowerBound
        else if (retY > upperBoundY) retY = upperBoundY

        return intArrayOf(retX, retY)
    }

    fun getCoordinatesOfView(view: GestureImageView): IntArray {
        var x = -1
        var y = -1

        for (testY in 0 until 20) {
            x = gridArray[testY].indexOf(view)

            if (x != -1) {
                y = testY
                break
            }
        }

        return intArrayOf(x, y)
    }

    fun getMapDescriptor(): String {
        var tempString = "11"
        exploreArray.forEach { it.forEach { state -> tempString += state.toString() }}
        tempString += "11"

        if (tempString.length % 4 != 0) {
            callback(Callback.MESSAGE, context.getString(R.string.something_went_wrong))
            return ""
        }

        var explorationDescriptor = ""
        var hex = ""
        var counter = 0

        tempString.indices.forEach { i ->
            hex += tempString[i]
            counter++

            if (counter == 4) {
                hex = hex.toInt(2).toString(16)
                explorationDescriptor += hex
                hex = ""
                counter = 0
            }
        }

        tempString = ""
        obstacleArray.forEach { it.forEach { state -> tempString += state.toString() }}
        val pad = Math.floorMod(tempString.length, 4)
        tempString = "".padEnd(pad, '0') + tempString

        var obstacleDescriptor = ""
        hex = ""
        counter = 0

        tempString.indices.forEach { i ->
            hex += tempString[i]
            counter++

            if (counter == 4) {
                hex = hex.toInt(2).toString(16)
                obstacleDescriptor += hex
                hex = ""
                counter = 0
            }
        }

        explorationDescriptor = "f8007000e0000000000000000000000000000000000000000000000000000000000000000003"
        Log.e(this::class.simpleName, "\n")
        Log.e(this::class.simpleName, "Exploration Descriptor: $explorationDescriptor")
        Log.e(this::class.simpleName, "Obstacle Descriptor: $obstacleDescriptor")
        return "$explorationDescriptor//$obstacleDescriptor"
    }

    fun hasUnexploredGrid(): Boolean {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                if (exploreArray[y][x] == 0) return true
            }
        }

        return false
    }

    fun isWaypointSet(): Boolean = isValidCoordinates(waypointPosition, true)
    fun isOccupied(x: Int, y: Int): Boolean = (isWaypoint(x, y) || isStartPoint(x, y) || isGoalPoint(x, y) || isImage(x, y) || isObstacle(x, y))
    fun isValidCoordinates(x: Int, y: Int, forRobot: Boolean = false): Boolean = if (forRobot) (x in 1..13 && y in 1..18) else (x in 0..14 && y in 0..19)
    fun getRobotFacing(): Int = robotPosition[2]
    fun getRobotPosition(): IntArray = robotPosition
    fun getStartPosition(): IntArray = startPosition
    fun getWaypointPosition(): IntArray = waypointPosition
    fun getGoalPosition(): IntArray = goalPosition
    fun getGridType(x: Int, y: Int): GridType = gridTypeArray[y][x]
    fun isGridExplored(x: Int, y: Int): Boolean = (exploreArray[y][x] == 1)
}