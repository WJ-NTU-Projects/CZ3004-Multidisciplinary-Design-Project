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
import ntu.mdp.android.mdptestkotlin.App.Companion.FORWARD_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.REVERSE_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.TURN_LEFT_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.TURN_RIGHT_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.coverageLimit
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationDelay
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationMode
import ntu.mdp.android.mdptestkotlin.App.Companion.usingAmd
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.utils.GestureImageView
import kotlin.math.abs

open class ArenaV2 (private val context: Context, private val callback: (status: Callback, message: String) -> Unit) {

    companion object {
        const val unexploredBit = 0
        const val exploredBit = 1

        var isWaitingUpdate = false
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
        OBSTACLE
    }

    enum class Broadcast {
        MOVE_COMPLETE,
        WAIT_TURN_COMPLETE,
        TURN_COMPLETE
    }

    private val scale           : Double = 0.74
    private val displayPixels   : Int = (context.resources.displayMetrics.widthPixels * scale).toInt()
    private val gridSize        : Int = ((displayPixels - 30) / 15)
    private val robotSize       : Int = (displayPixels / 15)

    private val robotPosition   : IntArray = intArrayOf(-1, -1, 0)
    private val startPosition   : IntArray = intArrayOf(-1, -1)
    private val goalPosition    : IntArray = intArrayOf(-1, -1)
    private val waypointPosition: IntArray = intArrayOf(-1, -1)

    private val gridParent      : RelativeLayout = (context as Activity).findViewById(R.id.gridParent)
    private val gridLayout      : GridLayout = (context as Activity).findViewById(R.id.main_grid_arena)
    private val robotDisplay    : ImageView = ImageView(context)

    private val gridTypeArray   : Array<Array<GridType>> = Array(20) { Array(15) { GridType.UNEXPLORED }}
    private val exploreArray    : Array<Array<Int>> = Array(20) { Array(15) { unexploredBit }}
    private val obstacleArray   : Array<Array<Int>> = Array(20) { Array(15) { unexploredBit }}
    private val gridArray       : Array<Array<GestureImageView>> = Array(20) { Array(15) { GestureImageView(context) }}

    private val undoActionList  : ArrayList<Pair<PlotFunction, IntArray>> = arrayListOf()
    private val redoActionList  : ArrayList<Pair<PlotFunction, IntArray>> = arrayListOf()
    private var viewOnHold      : GestureImageView = GestureImageView(context)
    private var currentFunction : PlotFunction = PlotFunction.NONE

    private var broadcastList   : ArrayList<(Broadcast) -> Unit> = arrayListOf()
    private var waitingForTurn  : Boolean = false
    private var waitingForTurnX : Int = -1
    private var waitingForTurnY : Int = -1
    private var lastMoveTime    : Long = 0L

    private val gestureCallback : (view: GestureImageView, gesture: GestureImageView.Gesture) -> Unit = { view, gesture ->
        when (gesture) {
            GestureImageView.Gesture.DOUBLE_TAP -> gridDoubleTap(view)
            GestureImageView.Gesture.LONG_PRESS -> gridLongPress(view)
            GestureImageView.Gesture.FLING_LEFT, GestureImageView.Gesture.FLING_RIGHT, GestureImageView.Gesture.FLING_DOWN, GestureImageView.Gesture.FLING_UP -> gridFling(gesture)
        }
    }

    private val broadcastCallback : (broadcastType: Broadcast) -> Unit = { broadcastType ->
        if (broadcastType == Broadcast.WAIT_TURN_COMPLETE) {
            if (isValidCoordinates(waitingForTurnX, waitingForTurnY)) {
                CoroutineScope(Dispatchers.Main).launch {
                    waitingForTurn = false
                    actuallyMoveRobot(waitingForTurnX, waitingForTurnY, robotPosition[2])
                }
            }
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
        updateRobotImage(0)
        setStartPoint(1, 1)
        setGoalPoint(13, 18)
        callback(Callback.UPDATE_STATUS, context.getString(R.string.idle))
        registerForBroadcast(broadcastCallback)
    }

    fun clearArena() {
        if (isValidCoordinates(waypointPosition, true)) setWaypoint(waypointPosition[0], waypointPosition[1])
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                setUnexploredForced(x, y)
            }
        }

        updateRobotImage(0)
        setStartPoint(1, 1)
        setGoalPoint(13, 18)
        callback(Callback.UPDATE_STATUS, context.getString(R.string.idle))
    }

    fun resetArena() {
        if (isValidCoordinates(waypointPosition, true)) setWaypoint(waypointPosition[0], waypointPosition[1])
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                setUnexplored(x, y)
            }
        }

        updateRobotImage(0)
        setStartPoint(1, 1)
        setGoalPoint(13, 18)
        callback(Callback.UPDATE_STATUS, context.getString(R.string.idle))
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

                if (!usingAmd && i == s[0].length - 1 && j >= 2) continue
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
        //val bitLength: Int = s[1].length * 4
        //var extraLength: Int = bitLength - exploredIndices.size

        for (i in s[1].indices) {
            var binary: String = s[1][i].toString().toInt(16).toString(2)
            binary = binary.padStart(4, '0')

            for (j in binary.indices) {
                val bit: Int = binary[j].toString().toInt()

                //if (!simulationMode && counter < extraLength) {
                //    extraLength--
                //    continue
                //}

                //val coordinates = if (simulationMode) Pair(0, 0) else exploredIndices[counter]
                //val x = if (simulationMode) (counter % 15) else coordinates.first
                //val y = if (simulationMode) Math.floorDiv(counter, 15) else coordinates.second

                val x = Math.floorMod(counter, 15)
                val y = Math.floorDiv(counter, 15)
                if (bit == 1) setObstacle(x, y)
                else removeObstacle(x, y)
                counter++
            }
        }
    }

    private fun broadcast(broadcastType: Broadcast) {
        for (f in broadcastList) {
            f.invoke(broadcastType)
        }
    }

    fun registerForBroadcast(f: (broadcastType: Broadcast) -> Unit) {
        broadcastList.add(f)
    }

    suspend fun moveRobotToStart() = withContext(Dispatchers.Main) {
        updateRobot(startPosition[0], startPosition[1])
    }

    suspend fun updateRobot(x1: Int, y1: Int, facing: Int = 0) = withContext(Dispatchers.Main) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]
        val broadcastType: Broadcast =
            if (x == robotPosition[0] && y == robotPosition[1]) {
                if (waitingForTurn) Broadcast.WAIT_TURN_COMPLETE
                else Broadcast.TURN_COMPLETE
            } else {
                Broadcast.MOVE_COMPLETE
            }

        robotPosition[0] = x
        robotPosition[1] = y
        updateRobotImage(facing)
        setRobotPosition(x, y)
        lastMoveTime = System.currentTimeMillis()
        if (simulationMode) scan(x, y, facing)
        if (isWaypointExact(x, y)) setWaypointTouched()
        else if (isGoalPointExact(x, y)) setGoalPointTouched()

        withContext(Dispatchers.Default) {
            val elapsed: Long = System.currentTimeMillis() - lastMoveTime

            if (elapsed < simulationDelay) {
                delay(simulationDelay - elapsed)
            }

            broadcast(broadcastType)
        }
    }

    suspend fun turnRobot(facing: Int) = withContext(Dispatchers.Main) {
        callback(Callback.UPDATE_STATUS, context.getString(R.string.turning))

        if (BluetoothController.isSocketConnected()) {
            val currentFacing: Int = robotPosition[2]
            val facingOffset: Int = currentFacing - facing

            if (facingOffset == 90 || facingOffset == -270) {
                callback(Callback.SEND_COMMAND, TURN_LEFT_COMMAND)
            } else {
                callback(Callback.SEND_COMMAND, TURN_RIGHT_COMMAND)
            }
        } else {
            updateRobotImage(facing)
            lastMoveTime = System.currentTimeMillis()
            if (simulationMode) scan(robotPosition[0], robotPosition[1], facing)

            withContext(Dispatchers.Default) {
                val elapsed: Long = System.currentTimeMillis() - lastMoveTime

                if (elapsed < simulationDelay) {
                    delay(simulationDelay - elapsed)
                }

                if (waitingForTurn) broadcast(Broadcast.WAIT_TURN_COMPLETE)
                else broadcast(Broadcast.TURN_COMPLETE)
            }
        }
    }

    protected suspend fun moveRobot(array: IntArray) = withContext(Dispatchers.Main) {
        moveRobot(array[0], array[1])
    }

    protected suspend fun moveRobot(facing: Int) = withContext(Dispatchers.Main) {
        val x = robotPosition[0]
        val y = robotPosition[1]

        when (facing) {
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

    private suspend fun moveRobot(x: Int, y: Int) = withContext(Dispatchers.Main) {
        if (!isRobotMovable(x, y)) {
            callback(Callback.UPDATE_STATUS, context.getString(R.string.blocked))
            lastMoveTime = System.currentTimeMillis()
            val elapsed: Long = System.currentTimeMillis() - lastMoveTime

            if (elapsed < simulationDelay) {
                delay(simulationDelay - elapsed)
            }

            lastMoveTime = System.currentTimeMillis()
            broadcast(Broadcast.MOVE_COMPLETE)
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
        val facingDifference = abs(facing - currentFacing)

        if (facingDifference == 180) {
            if (BluetoothController.isSocketConnected()) {
                callback(Callback.SEND_COMMAND, REVERSE_COMMAND)
                return@withContext
            }

            facing = currentFacing
            //if (direction != previousFacing) delay(simulationDelay)
        }

        if (facing != currentFacing) {
            waitingForTurn = true
            waitingForTurnX = x
            waitingForTurnY = y
            turnRobot(facing)
            return@withContext
        }

        if (BluetoothController.isSocketConnected()) {
            callback(Callback.SEND_COMMAND, FORWARD_COMMAND)
            return@withContext
        }

        actuallyMoveRobot(x, y, facing)
    }

    private suspend fun actuallyMoveRobot(x: Int, y: Int, facing: Int) = withContext(Dispatchers.Main) {
        callback(Callback.UPDATE_STATUS, context.getString(R.string.moving))
        setRobotPosition(x, y)
        robotPosition[2] = facing
        lastMoveTime = System.currentTimeMillis()
        if (simulationMode) scan(x, y, facing)
        if (isWaypointExact(x, y)) setWaypointTouched()
        else if (isGoalPointExact(x, y)) setGoalPointTouched()

        withContext(Dispatchers.Default) {
            val elapsed: Long = System.currentTimeMillis() - lastMoveTime

            if (elapsed < simulationDelay) {
                delay(simulationDelay - elapsed)
            }

            broadcast(Broadcast.MOVE_COMPLETE)
        }
    }

    private suspend fun setRobotPosition(x: Int, y: Int) = withContext(Dispatchers.Main) {
        val anchorX = x - 1
        val anchorY = y + 1

        robotDisplay.layoutParams  = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3).apply {
            leftMargin = (anchorX * robotSize)
            topMargin = (19 - anchorY) * robotSize
        }

        robotDisplay.requestLayout()
        robotPosition[0] = x
        robotPosition[1] = y
        callback(Callback.UPDATE_COORDINATES, "$x, $y")

        for (offsetY in -1..1) {
            for (offsetX in -1..1) {
                val newX = x + offsetX
                val newY = y + offsetY
                setExplored(newX, newY)
            }
        }
    }

    fun updateRobotImage(facing: Int = robotPosition[2]) {
        val drawable: Int = when (facing) {
            //45 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_45_connected else R.drawable.ic_45
            90 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_90_connected else R.drawable.ic_90
            //135 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_135_connected else R.drawable.ic_135
            180 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_180_connected else R.drawable.ic_180
            //225 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_225_connected else R.drawable.ic_225
            270 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_270_connected else R.drawable.ic_270
            //315 -> if (BluetoothController.isSocketConnected()) R.drawable.ic_315_connected else R.drawable.ic_315
            else -> if (BluetoothController.isSocketConnected()) R.drawable.ic_0_connected else R.drawable.ic_0
        }

        robotDisplay.setImageResource(drawable)
        robotPosition[2] = facing
    }

    private fun setUnexploredForced(x: Int, y: Int) {
        if (isValidCoordinates(x, y)) {
            exploreArray[y][x] = unexploredBit
            plot(x, y, GridType.UNEXPLORED)
        }
    }

    private fun setUnexplored(x: Int, y: Int) {
        if (isValidCoordinates(x, y)) {
            exploreArray[y][x] = unexploredBit
            if (!isOccupied(x, y)) plot(x, y, GridType.UNEXPLORED)
        }
    }

    private fun setExplored(x: Int, y: Int) {
        if (isValidCoordinates(x, y)) {
            exploreArray[y][x] = exploredBit
            if (!isOccupied(x, y)) plot(x, y, GridType.EXPLORED)
        }
    }

    private fun setObstacle(x: Int, y: Int) {
        if (isValidCoordinates(x, y)) plot(x, y, GridType.OBSTACLE)
        else callback(Callback.MESSAGE, context.getString(R.string.obstructed))
    }

    private fun removeObstacle(x: Int, y: Int) {
        if (isValidCoordinates(x, y) && isObstacle(x, y)) plot(x, y, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
    }

    private fun setStartPoint(x1: Int, y1: Int) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]

        if (isOccupied(x, y)) {
            if (isStartPointExact(x, y)) {
                CoroutineScope(Dispatchers.Main).launch {
                    moveRobotToStart()
                }
            }

            return
        }

        if (isValidCoordinates(startPosition, true)) {
            plotThree(startPosition, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
        }

        plot(x, y, GridType.START_POINT)
        startPosition[0] = x
        startPosition[1] = y

        CoroutineScope(Dispatchers.Main).launch {
            moveRobotToStart()
        }
    }

    private fun setWaypoint(x1: Int, y1: Int) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]

        if (isWaypointExact(x, y)) {
            plotThree(waypointPosition, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
            waypointPosition[0] = -1
            waypointPosition[1] = -1
            return
        }

        if (isOccupied(x, y)) return

        if (isValidCoordinates(waypointPosition, true)) {
            plotThree(waypointPosition, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
        }

        plot(x, y, GridType.WAYPOINT)
        waypointPosition[0] = x
        waypointPosition[1] = y
    }

    private fun setGoalPoint(x1: Int, y1: Int) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]

        if (isOccupied(x, y)) return

        if (isValidCoordinates(goalPosition, true)) {
            plotThree(goalPosition, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
        }

        plot(x, y, GridType.GOAL_POINT)
        goalPosition[0] = x
        goalPosition[1] = y
    }

    private fun setWaypointTouched() {
        if (isValidCoordinates(waypointPosition, true)) plot(waypointPosition, GridType.WAYPOINT_TOUCHED)
    }

    private fun setGoalPointTouched() {
        if (isValidCoordinates(goalPosition, true)) plot(goalPosition, GridType.GOAL_POINT_TOUCHED)
    }

    fun resetWaypoint() {
        if (isValidCoordinates(waypointPosition, true)) plot(waypointPosition, GridType.WAYPOINT)
    }

    fun resetGoalPoint() {
        if (isValidCoordinates(goalPosition, true)) plot(goalPosition, GridType.GOAL_POINT)
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

    fun isRobotMovable(array: IntArray): Boolean {
        if (array.size < 2) return false
        return isRobotMovable(array[0], array[1])
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

    @Suppress("unused")
    private fun isExplored(x: Int, y: Int, facing: Int, checkSingle: Boolean = false): Boolean {
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

    private fun scan(x: Int, y: Int, facing: Int, gridsAhead: Int = 1) {
        if (Math.floorMod(facing, 90) != 0) return
        scanFront(x, y, facing, gridsAhead)
        scanRight(x, y, facing, gridsAhead)
        scanLeft(x, y, facing, gridsAhead)
    }

    private fun scanFront(x: Int, y: Int, facing: Int, gridsAhead: Int) {
        val gridsAheadNoObstacleList: ArrayList<IntArray> = arrayListOf()

        for (offset in -1..1) {
            val newX: Int
            val newY: Int

            when (facing) {
                0 -> {
                    newX = x + offset
                    newY = y + (2 * gridsAhead)
                }

                90 -> {
                    newX = x + (2 * gridsAhead)
                    newY = y + offset
                }

                180 -> {
                    newX = x + offset
                    newY = y - (2 * gridsAhead)
                }

                else -> {
                    newX = x - (2 * gridsAhead)
                    newY = y + offset
                }
            }

            if (!isValidCoordinates(newX, newY)) continue

            if (obstacleArray[newY][newX] == 1) {
                setObstacle(newX, newY)
                setExplored(newX, newY)
            } else {
                gridsAheadNoObstacleList.add(intArrayOf(newX, newY))
            }
        }

        scanUnreachable2(facing, gridsAheadNoObstacleList)
    }

    private fun scanRight(x: Int, y: Int, facing: Int, gridsAhead: Int) {
        for (offset in -1..1) {
            val newX: Int
            val newY: Int

            when (facing) {
                0 -> {
                    newX = x + (2 * gridsAhead)
                    newY = y + offset
                }

                90 -> {
                    newX = x + offset
                    newY = y - (2 * gridsAhead)
                }

                180 -> {
                    newX = x - (2 * gridsAhead)
                    newY = y + offset
                }

                else -> {
                    newX = x + offset
                    newY = y + (2 * gridsAhead)
                }
            }

            if (!isValidCoordinates(newX, newY)) continue

            if (obstacleArray[newY][newX] == 1) {
                setObstacle(newX, newY)
                setExplored(newX, newY)
            }
        }
    }

    private fun scanLeft(x: Int, y: Int, facing: Int, gridsAhead: Int) {
        for (offset in -1..1) {
            val newX: Int
            val newY: Int

            when (facing) {
                0 -> {
                    newX = x - (2 * gridsAhead)
                    newY = y + offset
                }

                90 -> {
                    newX = x + offset
                    newY = y + (2 * gridsAhead)
                }

                180 -> {
                    newX = x + (2 * gridsAhead)
                    newY = y + offset
                }

                else -> {
                    newX = x + offset
                    newY = y - (2 * gridsAhead)
                }
            }

            if (!isValidCoordinates(newX, newY)) continue

            if (obstacleArray[newY][newX] == 1) {
                setObstacle(newX, newY)
                setExplored(newX, newY)
            }
        }
    }

    private fun scanUnreachable2(facing: Int, gridsAheadNoObstacleList: ArrayList<IntArray>) {
        for (grid in gridsAheadNoObstacleList) {
            var x: Int = grid[0]
            var y: Int = grid[1]
            var skip = false

            for (count in 0..2) {
                if (skip) break
                if (!isValidCoordinates(x, y)) continue

                if (count == 0) {
                    for (offset in -1..1 step 2) {
                        if (facing == 0 || facing == 180) {
                            if (isRobotMovable(x + offset, y)) {
                                skip = true
                                break
                            }
                        } else {
                            if (isRobotMovable(x, y + offset)) {
                                skip = true
                                break
                            }
                        }
                    }

                    if (skip) break
                }

                for (offsetY in -1..1) {
                    for (offsetX in -1..1) {
                        if (isValidCoordinates(x + offsetX, y + offsetY) && isRobotMovable(x + offsetX, y + offsetY)) {
                            skip = true
                            break
                        }
                    }
                }

                if (skip) break
                setExplored(x, y)

                when (facing) {
                    0 -> y += 1
                    90 -> x += 1
                    180 -> y -= 1
                    270 -> x -= 1
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

    private fun gridDoubleTap(view: GestureImageView) {
        if (currentFunction == PlotFunction.PLOT_OBSTACLE) {
            val coordinates: IntArray = getCoordinatesOfView(view)
            val x: Int = coordinates[0]
            val y: Int = coordinates[1]

            if (isObstacle(x, y)) {
                removeObstacle(x, y)
                undoActionList.add(Pair(PlotFunction.REMOVE_OBSTACLE, intArrayOf(x, y)))
            } else {
                setObstacle(x, y)
                undoActionList.add(Pair(PlotFunction.PLOT_OBSTACLE, intArrayOf(x, y)))
            }

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

    private fun gridFling(gesture: GestureImageView.Gesture) {
        if (currentFunction == PlotFunction.PLOT_OBSTACLE) {
            if (gesture == GestureImageView.Gesture.FLING_LEFT) undoAction()
            else redoAction()
            return
        }

        if (gesture == GestureImageView.Gesture.FLING_DOWN) {
            if (!autoUpdateArena && currentFunction == PlotFunction.NONE) {
                isWaitingUpdate = true
                callback(Callback.SEND_COMMAND, SEND_ARENA_COMMAND)
                return
            }

            if (!simulationMode) return
            var currentSpeed: Int = sharedPreferences.getInt(context.getString(R.string.app_pref_simulation_speed), 2)
            currentSpeed--
            if (currentSpeed < 0) return
            simulationDelay = (1000 / (currentSpeed + 1)).toLong()
            sharedPreferences.edit().putInt(context.getString(R.string.app_pref_simulation_speed), currentSpeed).apply()
            callback(Callback.MESSAGE, context.getString(R.string.simulation_speed_notification, currentSpeed + 1))
            return
        }

        if (gesture == GestureImageView.Gesture.FLING_UP) {
            if (!simulationMode) return
            var currentSpeed: Int = sharedPreferences.getInt(context.getString(R.string.app_pref_simulation_speed), 2)
            currentSpeed++
            if (currentSpeed > 9) return
            simulationDelay = (1000 / (currentSpeed + 1)).toLong()
            sharedPreferences.edit().putInt(context.getString(R.string.app_pref_simulation_speed), currentSpeed).apply()
            callback(Callback.MESSAGE, context.getString(R.string.simulation_speed_notification, currentSpeed + 1))
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

    private fun isWaypoint(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.WAYPOINT || gridType == GridType.WAYPOINT_TOUCHED)
    }

    private fun isWaypointExact(x: Int, y: Int): Boolean {
        return (waypointPosition[0] == x && waypointPosition[1] == y)
    }

    private fun isStartPoint(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.START_POINT)
    }

    fun isStartPointExact(array: IntArray): Boolean {
        if (array.size < 2) return false
        return isStartPointExact(array[0], array[1])
    }

    fun isStartPointExact(x: Int, y: Int): Boolean {
        return (startPosition[0] == x && startPosition[1] == y)
    }

    private fun isGoalPoint(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.GOAL_POINT || gridType == GridType.GOAL_POINT_TOUCHED)
    }

    private fun isGoalPointExact(x: Int, y: Int): Boolean {
        return (goalPosition[0] == x && goalPosition[1] == y)
    }

    private fun isImage(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.IMAGE)
    }

    private fun isObstacle(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.OBSTACLE)
    }

    fun isValidCoordinates(array: IntArray, forRobot: Boolean = false): Boolean {
        if (array.size != 2) return false
        val x = array[0]
        val y = array[1]
        return isValidCoordinates(x, y, forRobot)
    }

    private fun getValidCoordinates(x: Int, y: Int, forRobot: Boolean = false): IntArray {
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

    private fun getCoordinatesOfView(view: GestureImageView): IntArray {
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

        //explorationDescriptor = "f8007000e0000000000000000000000000000000000000000000000000000000000000000003"
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

    fun coverageReached(): Boolean {
        var coveredCount = 0
        exploreArray.forEach { it.forEach { grid -> if (grid == 1) coveredCount++ } }
        return ((1.0 * coveredCount / 300) * 100 >= coverageLimit)
    }

    fun setPlotFunction(function: PlotFunction) {
        currentFunction = function
    }

    private fun isOccupied(x: Int, y: Int): Boolean = (isWaypoint(x, y) || isStartPoint(x, y) || isGoalPoint(x, y) || isImage(x, y) || isObstacle(x, y))
    fun isWaypointSet(): Boolean = isValidCoordinates(waypointPosition, true)
    fun isValidCoordinates(x: Int, y: Int, forRobot: Boolean = false): Boolean = if (forRobot) (x in 1..13 && y in 1..18) else (x in 0..14 && y in 0..19)
    fun getRobotPosition(): IntArray = robotPosition
    fun getStartPosition(): IntArray = startPosition
    fun getWaypointPosition(): IntArray = waypointPosition
    fun getGoalPosition(): IntArray = goalPosition
    fun isGridExplored(x: Int, y: Int): Boolean = (exploreArray[y][x] == 1)
    fun getRobotFacing(): Int = robotPosition[2]
    fun getCurrentFunction(): PlotFunction = currentFunction
}