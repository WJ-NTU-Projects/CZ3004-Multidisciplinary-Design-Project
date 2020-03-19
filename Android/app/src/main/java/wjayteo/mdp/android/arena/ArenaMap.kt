package wjayteo.mdp.android.arena

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.widget.*
import kotlinx.coroutines.*
import wjayteo.mdp.android.App.Companion.ARDUINO_PREFIX
import wjayteo.mdp.android.App.Companion.COMMAND_DIVIDER
import wjayteo.mdp.android.App.Companion.DESCRIPTOR_DIVIDER
import wjayteo.mdp.android.App.Companion.FORWARD_COMMAND
import wjayteo.mdp.android.App.Companion.IS_TABLET
import wjayteo.mdp.android.App.Companion.PC_PREFIX
import wjayteo.mdp.android.App.Companion.REVERSE_COMMAND
import wjayteo.mdp.android.App.Companion.SEND_ARENA_COMMAND
import wjayteo.mdp.android.App.Companion.TURN_LEFT_COMMAND
import wjayteo.mdp.android.App.Companion.TURN_RIGHT_COMMAND
import wjayteo.mdp.android.App.Companion.WAYPOINT_COMMAND
import wjayteo.mdp.android.App.Companion.AUTO_UPDATE_ARENA
import wjayteo.mdp.android.App.Companion.COVERAGE_LIMIT
import wjayteo.mdp.android.App.Companion.sharedPreferences
import wjayteo.mdp.android.App.Companion.SIM_DELAY
import wjayteo.mdp.android.App.Companion.SIM_MODE
import wjayteo.mdp.android.R
import wjayteo.mdp.android.bluetooth.BluetoothController
import wjayteo.mdp.android.simulation.AStarSearch
import wjayteo.mdp.android.utils.GestureImageView
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor


open class ArenaMap (private val context: Context, private val callback: (status: Callback, message: String) -> Unit) {

    companion object {
        const val unexploredBit = 0
        const val exploredBit = 1

        var isWaitingUpdate = false
    }

    enum class Direction {
        NONE,
        FORWARD,
        REVERSE,
        LEFT,
        RIGHT
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
        LONG_PRESS_CHOICE
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
        FASTEST_PATH
    }

    enum class Broadcast {
        MOVE_COMPLETE,
        OBSTRUCTED,
        TURN_COMPLETE
    }

    enum class Visibility {
        UNEXPLORED,
        EXPLORED
    }

    var currentVisibility       : Visibility = Visibility.UNEXPLORED
    private val scale           : Double = if (context.resources.getBoolean(R.bool.isTablet)) 0.745 else 0.68
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
    private val startDisplay    : ImageView = ImageView(context)
    private val goalDisplay     : ImageView = ImageView(context)
    private val waypointDisplay : ImageView = ImageView(context)
    private val tklDisplay      : ImageView = ImageView(context)

    private val gridTypeArray   : Array<Array<GridType>> = Array(20) { Array(15) { GridType.UNEXPLORED }}
    private val exploreArray    : Array<Array<Int>> = Array(20) { Array(15) { unexploredBit }}
    private val obstacleArray   : Array<Array<Int>> = Array(20) { Array(15) { unexploredBit }}
    private val gridArray       : Array<Array<GestureImageView>> = Array(20) { Array(15) { GestureImageView(context) }}

    private val debugImageList  : ArrayList<Int> = arrayListOf()
    private val imageList       : ArrayList<IntArray> = arrayListOf()
    private val undoActionList  : ArrayList<Pair<PlotFunction, IntArray>> = arrayListOf()
    private val redoActionList  : ArrayList<Pair<PlotFunction, IntArray>> = arrayListOf()
    private var viewOnHold      : GestureImageView = GestureImageView(context)
    private var currentFunction : PlotFunction = PlotFunction.NONE

    private var broadcastList   : ArrayList<(Broadcast, BooleanArray) -> Unit> = arrayListOf()
    private var lastMoveTime    : Long = 0L
    private var travelComplete  : Boolean = true
    private var cancel          : Boolean = false

    private val gestureCallback : (view: GestureImageView, gesture: GestureImageView.Gesture) -> Unit = { view, gesture ->
        when (gesture) {
            GestureImageView.Gesture.SINGLE_TAP -> gridSingleTap(view)
            GestureImageView.Gesture.DOUBLE_TAP -> gridDoubleTap(view)
            GestureImageView.Gesture.LONG_PRESS -> gridLongPress(view)
            GestureImageView.Gesture.FLING_LEFT, GestureImageView.Gesture.FLING_RIGHT, GestureImageView.Gesture.FLING_DOWN, GestureImageView.Gesture.FLING_UP -> gridFling(gesture)
        }
    }

    init {
        for (y in 19 downTo 0) {
            for (x in 0..15) {
                if (x == 15) {
                    val textView = TextView(context)
                    textView.textSize = context.resources.getDimension(R.dimen.text_size_arena_label)
                    textView.text = "$y"
                    gridLayout.addView(textView)

                    val params = textView.layoutParams as GridLayout.LayoutParams
                    params.leftMargin = 3
                    params.setGravity(Gravity.CENTER)
                    textView.layoutParams = params
                    continue
                }

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

        for (x in 0..14) {
            val textView = TextView(context)
            textView.textSize = context.resources.getDimension(R.dimen.text_size_arena_label)
            textView.text = "$x"
            gridLayout.addView(textView)

            val params = textView.layoutParams as GridLayout.LayoutParams
            params.setGravity(Gravity.CENTER_HORIZONTAL)
            textView.layoutParams = params
        }

        startDisplay.layoutParams = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3)
        startDisplay.setBackgroundColor(Color.TRANSPARENT)
        startDisplay.setImageDrawable(context.getDrawable(R.drawable.img_startpoint))
        startDisplay.alpha = 1.0f
        gridParent.addView(startDisplay)

        try {
            val coordinatesStr: String = sharedPreferences.getString(context.getString(R.string.app_pref_start_position), "1, 1") ?: "1, 1"
            val coordinates: List<String> = coordinatesStr.split(", ")
            setStartPoint(coordinates[0].toInt(), coordinates[1].toInt())
        } catch (e: Exception) {
            Log.e("GG", "GG!", e)
            setStartPoint(1, 1)
        }

        goalDisplay.layoutParams = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3)
        goalDisplay.setBackgroundColor(Color.TRANSPARENT)
        goalDisplay.setImageDrawable(context.getDrawable(R.drawable.img_goalpoint))
        goalDisplay.alpha = 1.0f
        gridParent.addView(goalDisplay)

        try {
            val coordinatesStr: String = sharedPreferences.getString(context.getString(R.string.app_pref_goal_position), "13, 18") ?: "13, 18"
            val coordinates: List<String> = coordinatesStr.split(", ")
            setGoalPoint(coordinates[0].toInt(), coordinates[1].toInt())
        } catch (e: Exception) {
            Log.e("GG", "GG!", e)
            setGoalPoint(13, 18)
        }

        waypointDisplay.layoutParams = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3)
        waypointDisplay.setBackgroundColor(Color.TRANSPARENT)
        waypointDisplay.setImageDrawable(context.getDrawable(R.drawable.img_waypoint))
        waypointDisplay.alpha = 0.0f
        gridParent.addView(waypointDisplay)

        robotDisplay.layoutParams = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3)
        robotDisplay.setBackgroundColor(Color.TRANSPARENT)
        robotDisplay.alpha = 1.0f
        gridParent.addView(robotDisplay)

        tklDisplay.layoutParams  = RelativeLayout.LayoutParams(displayPixels, displayPixels)
        tklDisplay.scaleType = ImageView.ScaleType.CENTER
        tklDisplay.alpha = 0.2f
        tklDisplay.setImageResource(R.drawable.tkl)
        gridParent.addView(tklDisplay)

        CoroutineScope(Dispatchers.Main).launch {
            updateRobotImage(0)
        }.invokeOnCompletion {
            callback(Callback.UPDATE_STATUS, context.getString(R.string.idle))
        }
    }

    fun cancelLast() {
        cancel = true
    }

    fun setAllExplored(yes: Boolean) {
        currentVisibility = if (yes) Visibility.EXPLORED else Visibility.UNEXPLORED

        for (y in 19 downTo 0) {
            for (x in 0..14) {
                if (yes) setExplored(x, y)
                else setUnexplored(x, y)
            }
        }
    }

    fun clearArena() {
        if (isValidCoordinates(waypointPosition, true)) setWaypoint(waypointPosition[0], waypointPosition[1])

        for (y in 19 downTo 0) {
            for (x in 0..14) {
                setUnexploredForced(x, y)
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            moveRobotToStart()
            waypointDisplay.alpha = 0.0f
            resetStartPoint()
            resetGoalPoint()
            updateRobotImage(0)
            imageList.clear()
            debugImageList.clear()
        }.invokeOnCompletion {
            callback(Callback.UPDATE_STATUS, context.getString(R.string.idle))
        }
    }

    fun emptyArena() {
        if (isValidCoordinates(waypointPosition, true)) setWaypoint(waypointPosition[0], waypointPosition[1])

        for (y in 19 downTo 0) {
            for (x in 0..14) {
                setUnexploredForced(x, y)
            }
        }

        waypointDisplay.alpha = 0.0f
        startPosition[0] = -1
        startPosition[1] = -1
        goalPosition[0] = -1
        goalPosition[1] = -1
        imageList.clear()
        debugImageList.clear()
    }

    fun resetArena() {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                setUnexplored(x, y)
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            moveRobotToStart()
            resetStartPoint()
            resetGoalPoint()
            resetWaypoint()
        }.invokeOnCompletion {
            callback(Callback.UPDATE_STATUS, context.getString(R.string.idle))
        }
    }

    fun showTKL(show: Boolean) {
        if (show) tklDisplay.alpha = 0.26f
        else tklDisplay.alpha = 0.0f
    }

    fun updateArena(explorationData: String) {
        val arrays: ArrayList<Array<Array<Int>>> = MapDescriptor.fromString(explorationData, exploredBit)

        for (y in 19 downTo 0) {
            for (x in 0..14) {
                val explorationBit: Int = arrays[0][y][x]
                val obstacleBit: Int = arrays[1][y][x]

                exploreArray[y][x] = explorationBit
                obstacleArray[y][x] = obstacleBit

                if (explorationBit == exploredBit) setExplored(x, y)
                else setUnexplored(x, y)

                if (obstacleBit == 1) setObstacle(x, y)
                else removeObstacle(x, y)
            }
        }
    }

    protected suspend fun broadcast(broadcastType: Broadcast, sensorData: BooleanArray) = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.e("BROADCASTING", "${LocalDateTime.now()}: ${robotPosition[0]}, ${robotPosition[1]}, ${robotPosition[2]}, $broadcastType, [${sensorData[0]}, ${sensorData[1]}, ${sensorData[2]}]")
        }

        for (f in broadcastList) {
            f.invoke(broadcastType, sensorData)
        }
    }

    fun registerForBroadcast(f: (broadcastType: Broadcast, sensorData: BooleanArray) -> Unit) {
        broadcastList.add(f)
    }

    fun deregisterForBroadcast(f: (broadcastType: Broadcast, sensorData: BooleanArray) -> Unit) {
        broadcastList.remove(f)
    }

    suspend fun moveRobotToStart() = withContext(Dispatchers.Main) {
        updateRobot(startPosition[0], startPosition[1])
    }

    suspend fun updateRobot(x1: Int, y1: Int, facing: Int = robotPosition[2]) = withContext(Dispatchers.Main) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]
//        val broadcastType: Broadcast =
//            if (x == robotPosition[0] && y == robotPosition[1]) {
//                Broadcast.TURN_COMPLETE
//            } else {
//                Broadcast.MOVE_COMPLETE
//            }

//        val elapsed: Long = System.currentTimeMillis() - lastMoveTime
//
//        if (elapsed < simulationDelay) {
//            delay(simulationDelay - elapsed)
//        }

//        val currentX = robotPosition[0]
//        val currentY = robotPosition[1]
        robotPosition[0] = x
        robotPosition[1] = y

        /*
        if (facing == robotPosition[2]) {
            if ((currentX == x && abs(currentY - y) == 1) || (currentY == y && abs(currentX - x) == 1)) {
                travelComplete = false
                setRobotPosition2(currentX, currentY, x, y)
            } else {
                setRobotPosition(x, y)
            }
        }
        */

        setRobotPosition(x, y)
//        travelComplete = false
//        updateRobotImage(facing)
        updateRobotImage2(facing)
        if (isWaypointExact(x, y)) setWaypointTouched()
        else if (isGoalPointExact(x, y)) setGoalPointTouched()
//        val sensorData: BooleanArray = if (simulationMode) scan(x, y, facing) else booleanArrayOf(false, false, false)
//        lastMoveTime = System.currentTimeMillis()
//        broadcast(broadcastType, sensorData)
    }

    suspend fun turnRobot(facing: Int) = withContext(Dispatchers.Main) {
        callback(Callback.UPDATE_STATUS, context.getString(R.string.turning))

        if (BluetoothController.isSocketConnected()) {
            val currentFacing: Int = robotPosition[2]
            val facingOffset: Int = currentFacing - facing

            if (facingOffset == 90 || facingOffset == -270) {
                callback(Callback.SEND_COMMAND, "$ARDUINO_PREFIX$TURN_LEFT_COMMAND")
            } else if (facingOffset == -90 || facingOffset == 270) {
                callback(Callback.SEND_COMMAND, "$ARDUINO_PREFIX$TURN_RIGHT_COMMAND")
            }

            delay(250)
            broadcast(Broadcast.TURN_COMPLETE, booleanArrayOf(false, false, false))
            return@withContext
        }

        val elapsed: Long = System.currentTimeMillis() - lastMoveTime

        if (elapsed < SIM_DELAY) {
            delay(SIM_DELAY - elapsed)
        }

        travelComplete = false
        updateRobotImage(facing)
        val sensorData: BooleanArray = if (SIM_MODE) scan(robotPosition[0], robotPosition[1], facing) else booleanArrayOf(false, false, false)
        lastMoveTime = System.currentTimeMillis()
        broadcast(Broadcast.TURN_COMPLETE, sensorData)
    }

    suspend fun moveRobot(x: Int, y: Int, noReverse: Boolean = false) = withContext(Dispatchers.Main) {
        if (!isRobotMovable(x, y) && !BluetoothController.isSocketConnected()) {
            callback(Callback.UPDATE_STATUS, context.getString(R.string.obstructed))
            val sensorData: BooleanArray = if (SIM_MODE) scan(robotPosition[0], robotPosition[1], robotPosition[2]) else booleanArrayOf(false, false, false)
            broadcast(Broadcast.OBSTRUCTED, sensorData)
            return@withContext
        }

        val currentX: Int = robotPosition[0]
        val currentY: Int = robotPosition[1]
        val currentFacing: Int = robotPosition[2]

        val positionDifferenceX: Int = (x - currentX)
        val positionDifferenceY: Int = (y - currentY)
        val direction: Int = when {
            (positionDifferenceX == 0 && positionDifferenceY == 1)  -> 0
            (positionDifferenceX == 1 && positionDifferenceY == 0)  -> 90
            (positionDifferenceX == 0 && positionDifferenceY == -1) -> 180
            (positionDifferenceX == -1 && positionDifferenceY == 0) -> 270
            else -> currentFacing
        }

        var facing: Int = direction
        val facingDifference = abs(facing - currentFacing)

        if (facingDifference == 180) {
            facing = currentFacing

            if (noReverse) {
                var newFacing: Int = facing

                for (i in 0..1) {
                    val elapsed: Long = System.currentTimeMillis() - lastMoveTime

                    if (elapsed < SIM_DELAY) {
                        delay(SIM_DELAY - elapsed)
                    }

                    travelComplete = false
                    newFacing = Math.floorMod(newFacing + 90, 360)
                    updateRobotImage(newFacing)

                    while (!travelComplete) {
                        delay(1)
                    }

                    scan(robotPosition[0], robotPosition[1], facing)
                    lastMoveTime = System.currentTimeMillis()
                }

                facing = newFacing
            }
        }

        if (facing != currentFacing) {
            val elapsed: Long = System.currentTimeMillis() - lastMoveTime

            if (elapsed < SIM_DELAY) {
                delay(SIM_DELAY - elapsed)
            }

            travelComplete = false
            updateRobotImage(facing)

            while (!travelComplete) {
                delay(1)
            }

            scan(robotPosition[0], robotPosition[1], facing)
            lastMoveTime = System.currentTimeMillis()
        }

        callback(Callback.UPDATE_STATUS, context.getString(R.string.moving))
        val elapsed: Long = System.currentTimeMillis() - lastMoveTime

        if (elapsed < SIM_DELAY) {
            delay(SIM_DELAY - elapsed)
        }

        while (!travelComplete) {
            delay(1)
        }

        if (cancel) {
            cancel = false
            val sensorData: BooleanArray = if (SIM_MODE) scan(robotPosition[0], robotPosition[1], robotPosition[2]) else booleanArrayOf(false, false, false)
            broadcast(Broadcast.MOVE_COMPLETE, sensorData)
            return@withContext
        }

        if (BluetoothController.isSocketConnected()) {
            if (facingDifference == 180 && !noReverse) callback(Callback.SEND_COMMAND, "$ARDUINO_PREFIX$REVERSE_COMMAND")
            else callback(Callback.SEND_COMMAND, "$ARDUINO_PREFIX$FORWARD_COMMAND")
            delay(250)
            broadcast(Broadcast.MOVE_COMPLETE, booleanArrayOf(false, false, false))
            return@withContext
        }

        travelComplete = false
        lastMoveTime = System.currentTimeMillis()
        robotPosition[0] = x
        robotPosition[1] = y
        robotPosition[2] = facing
        setRobotPosition2(currentX, currentY, x, y)
        if (isWaypointExact(x, y)) setWaypointTouched()
        else if (isGoalPointExact(x, y)) setGoalPointTouched()
        val sensorData: BooleanArray = if (SIM_MODE) scan(x, y, facing) else booleanArrayOf(false, false, false)
        broadcast(Broadcast.MOVE_COMPLETE, sensorData)
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

    private suspend fun setRobotPosition2(currentX: Int, currentY: Int, x: Int, y: Int) = withContext(Dispatchers.Main) {
        val currentAnchorX = (currentX - 1) * robotSize
        val currentAnchorY = (19 - (currentY + 1)) * robotSize
        val anchorX = (x - 1) * robotSize
        val anchorY = (19 - (y + 1)) * robotSize
        val params = robotDisplay.layoutParams as RelativeLayout.LayoutParams

        CoroutineScope(Dispatchers.Main).launch {
            for (offsetY in -1..1) {
                for (offsetX in -1..1) {
                    val newX = x + offsetX
                    val newY = y + offsetY
                    setExplored(newX, newY)
                }
            }
        }

        if (anchorX != currentAnchorX) {
            val distance: Double = 1.0 * abs(anchorX - currentAnchorX) / 3.0
            val modifier = if ((anchorX - currentAnchorX) < 0) -3 else 3
            val delay = ceil(1.0 * SIM_DELAY / distance).toLong()

            for (i in 0 until (floor(distance).toInt())) {
                params.topMargin = anchorY
                params.leftMargin += modifier
                robotDisplay.layoutParams = params
                delay(delay)
            }
        } else if (anchorY != currentAnchorY) {
            val distance: Double = 1.0 * abs(anchorY - currentAnchorY) / 3.0
            val modifier = if ((anchorY - currentAnchorY) < 0) -3 else 3
            val delay = ceil(1.0 * SIM_DELAY / distance).toLong()

            for (i in 0 until (floor(distance).toInt())) {
                params.topMargin += modifier
                params.leftMargin = anchorX
                robotDisplay.layoutParams = params
                delay(delay)
            }
        }

        if (params.leftMargin != anchorX) params.leftMargin = anchorX
        if (params.topMargin != anchorY) params.topMargin = anchorY
        robotDisplay.layoutParams = params
        updateRobotImage()
        travelComplete = true
        callback(Callback.UPDATE_COORDINATES, "$x, $y")
    }

    private suspend fun updateRobotImage2(facing: Int = robotPosition[2]) = withContext(Dispatchers.Main) {
        val drawable: Int =
            if (IS_TABLET) {
                when (facing) {
                    0 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_c_up else R.drawable.img_robot_up
                    90 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_c_right else R.drawable.img_robot_right
                    180 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_c_down else R.drawable.img_robot_down
                    270 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_c_left else R.drawable.img_robot_left
                    else -> return@withContext
                }
            } else {
                when (facing) {
                    0 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_tkl_0 else R.drawable.img_robot_up
                    90 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_tkl_90 else R.drawable.img_robot_right
                    180 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_tkl_180 else R.drawable.img_robot_down
                    270 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_tkl_270 else R.drawable.img_robot_left
                    else -> return@withContext
                }
            }

        robotDisplay.setImageResource(drawable)
        robotDisplay.rotation = 0.0f
        robotPosition[2] = facing
        travelComplete = true
    }

    suspend fun updateRobotImage(facing1: Int = robotPosition[2]) = withContext(Dispatchers.Main) {
        val facing = Math.floorMod(facing1, 360)
        var currentFacing = robotPosition[2]
        val offset = facing - currentFacing
        val modifier = if (offset == 90 || offset == -270) 10 else -10
        val delay = ceil(1.0 * SIM_DELAY / 9.0).toLong()

        if (facing != currentFacing) {
            while (currentFacing != facing) {
                currentFacing = Math.floorMod((currentFacing + modifier), 360)
                robotDisplay.rotation += modifier
                delay(delay)
            }
        }

        val drawable: Int =
            if (IS_TABLET) {
                when (facing) {
                    0 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_c_up else R.drawable.img_robot_up
                    90 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_c_right else R.drawable.img_robot_right
                    180 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_c_down else R.drawable.img_robot_down
                    270 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_c_left else R.drawable.img_robot_left
                    else -> {
                        travelComplete = true
                        return@withContext
                    }
                }
            } else {
                when (facing) {
                    0 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_tkl_0 else R.drawable.img_robot_up
                    90 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_tkl_90 else R.drawable.img_robot_right
                    180 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_tkl_180 else R.drawable.img_robot_down
                    270 -> if (BluetoothController.isSocketConnected()) R.drawable.img_robot_tkl_270 else R.drawable.img_robot_left
                    else -> {
                        travelComplete = true
                        return@withContext
                    }
                }
            }

        robotDisplay.setImageResource(drawable)
        robotDisplay.rotation = 0.0f
        robotPosition[2] = facing
        travelComplete = true
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

            if (!isOccupied(x, y, false)) {
                plot(x, y, GridType.UNEXPLORED)
            }
        }
    }

    private fun setExplored(x: Int, y: Int) {
        if (isValidCoordinates(x, y)) {
            exploreArray[y][x] = exploredBit

            if (!isOccupied(x, y, false)) {
                plot(x, y, GridType.EXPLORED)
            }
        }
    }

    private fun setObstacle(x: Int, y: Int) {
        if (isValidCoordinates(x, y) && !isOccupied(x, y, false)) {
            plot(x, y, GridType.OBSTACLE)
        }
    }

    private fun removeObstacle(x: Int, y: Int) {
        if (isValidCoordinates(x, y) && isObstacle(x, y)) {
            plot(x, y, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
        }
    }

    fun setStartPoint(x1: Int, y1: Int) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]

        if ((isOccupied(x, y, true) && !isStartPoint(x, y)) || isStartPointExact(x, y)) {
            if (isStartPointExact(x, y)) {
                CoroutineScope(Dispatchers.Main).launch {
                    moveRobotToStart()
                }
            } else {
                callback(Callback.MESSAGE, context.getString(R.string.cannot_plot))
            }

            return
        }

        if (isValidCoordinates(startPosition, true)) {
            for (offsetY in -1 .. 1) {
                for (offsetX in -1..1) {
                    val newX = startPosition[0] + offsetX
                    val newY = startPosition[1] + offsetY
                    plot(newX, newY, if (exploreArray[newY][newX] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
                }
            }
        }

        val anchorX = x - 1
        val anchorY = y + 1

        startDisplay.layoutParams  = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3).apply {
            leftMargin = (anchorX * robotSize)
            topMargin = (19 - anchorY) * robotSize
        }

        startDisplay.requestLayout()
        plot(x, y, GridType.START_POINT)
        startPosition[0] = x
        startPosition[1] = y
        sharedPreferences.edit().putString(context.getString(R.string.app_pref_start_position), "$x, $y").apply()

        for (offsetY in -1..1) {
            for (offsetX in -1..1) {
                val newX = x + offsetX
                val newY = y + offsetY
                setExplored(newX, newY)
            }
        }

        //callback(Callback.SEND_COMMAND, "${START_POINT_COMMAND}${COMMAND_DIVIDER}$x, $y")

        CoroutineScope(Dispatchers.Main).launch {
            moveRobotToStart()
        }
    }

    fun setWaypoint(x1: Int, y1: Int) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]

        if (isOccupied(x, y, true) && !isWaypoint(x, y)) {
            if (!isWaypointExact(x, y)) {
                callback(Callback.MESSAGE, context.getString(R.string.cannot_plot))
            }

            return
        }

        if (isWaypointExact(x, y) || isValidCoordinates(waypointPosition, true)) {
            for (offsetY in -1 .. 1) {
                for (offsetX in -1..1) {
                    val newX = waypointPosition[0] + offsetX
                    val newY = waypointPosition[1] + offsetY
                    plot(newX, newY, if (exploreArray[newY][newX] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
                }
            }

            if (isWaypointExact(x, y)) {
                waypointDisplay.alpha = 0.0f
                waypointPosition[0] = -1
                waypointPosition[1] = -1
                return
            }
        }

        val anchorX = x - 1
        val anchorY = y + 1

        waypointDisplay.layoutParams  = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3).apply {
            leftMargin = (anchorX * robotSize)
            topMargin = (19 - anchorY) * robotSize
        }

        waypointDisplay.requestLayout()
        waypointDisplay.alpha = 1.0f
        plot(x, y, GridType.WAYPOINT)
        waypointPosition[0] = x
        waypointPosition[1] = y
        callback(Callback.SEND_COMMAND, "$PC_PREFIX$WAYPOINT_COMMAND$COMMAND_DIVIDER$x, $y")
    }

    fun setGoalPoint(x1: Int, y1: Int) {
        val coordinates: IntArray = getValidCoordinates(x1, y1, true)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]

        if ((isOccupied(x, y, true) && !isGoalPoint(x, y)) || isGoalPointExact(x, y)) {
            if (!isGoalPointExact(x, y)) {
                callback(Callback.MESSAGE, context.getString(R.string.cannot_plot))
            }

            return
        }

        if (isValidCoordinates(goalPosition, true)) {
            for (offsetY in -1 .. 1) {
                for (offsetX in -1..1) {
                    val newX = goalPosition[0] + offsetX
                    val newY = goalPosition[1] + offsetY
                    plot(newX, newY, if (exploreArray[newY][newX] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
                }
            }
        }

        val anchorX = x - 1
        val anchorY = y + 1

        goalDisplay.layoutParams  = RelativeLayout.LayoutParams(robotSize * 3, robotSize * 3).apply {
            leftMargin = (anchorX * robotSize)
            topMargin = (19 - anchorY) * robotSize
        }

        goalDisplay.requestLayout()
        plot(x, y, GridType.GOAL_POINT)
        goalPosition[0] = x
        goalPosition[1] = y
        sharedPreferences.edit().putString(context.getString(R.string.app_pref_goal_position), "$x, $y").apply()
        //callback(Callback.SEND_COMMAND, "${GOAL_POINT_COMMAND}${COMMAND_DIVIDER}$x, $y")
    }

    private fun setWaypointTouched() {
        if (isValidCoordinates(waypointPosition, true)) plot(waypointPosition, GridType.WAYPOINT_TOUCHED)
    }

    private fun setGoalPointTouched() {
        if (isValidCoordinates(goalPosition, true)) plot(goalPosition, GridType.GOAL_POINT_TOUCHED)
    }

    private fun resetStartPoint() {
        if (isValidCoordinates(startPosition, true)) plot(startPosition, GridType.START_POINT)
    }

    fun resetWaypoint() {
        if (isValidCoordinates(waypointPosition, true)) plot(waypointPosition, GridType.WAYPOINT)
    }

    fun resetGoalPoint() {
        if (isValidCoordinates(goalPosition, true)) plot(goalPosition, GridType.GOAL_POINT)
    }

    fun selectPoint(isStart: Boolean) {
        val coordinates: IntArray = getCoordinatesOfView(viewOnHold)

        if (isStart) {
            setStartPoint(coordinates[0], coordinates[1])
        } else {
            setGoalPoint(coordinates[0], coordinates[1])
        }
    }

    fun plotFastestPath() {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                if (gridTypeArray[y][x] == GridType.FASTEST_PATH) plot(x, y, if (exploreArray[y][x] == exploredBit) GridType.EXPLORED else GridType.UNEXPLORED)
            }
        }

        val pathList: List<IntArray> = AStarSearch(this).fastestPathChallenge()

        for (p in pathList) {
            val x = p[0]
            val y = p[1]

            if (isValidCoordinates(x, y)) {
                if (!isOccupied(x, y, false)) plot(x, y, GridType.FASTEST_PATH)
            }
        }
    }

    fun clearImages() {
        imageList.clear()

        for (y in 19 downTo 0) {
            for (x in 14 downTo 0) {
                if (isImage(x, y)) {
                    removeImage(intArrayOf(x, y))
                }
            }
        }
    }

    fun setImage(x1: Int, y1: Int, id: Int) {
        var x: Int = x1
        var y: Int = y1
        if (id < 1 || id > 15) return

        if (!isValidCoordinates(x, y)) {
            val valid: IntArray = getValidCoordinates(x, y)
            x = valid[0]
            y = valid[1]
        }

//        if (isOccupied(x, y, false) && !isObstacle(x, y)) {
//            callback(Callback.MESSAGE, context.getString(R.string.cannot_plot))
//            return
//        }

        //#im:(5,0,11)(1,5,21)(4,8,9)

        if (!isObstacle2(x, y)) {
            var shortestDistance: Int = Integer.MAX_VALUE
            var finalX: Int = x
            var finalY: Int = y
            val parentX: Int = x
            val parentY: Int = y

            for (y2 in 19 downTo 0) {
                for (x2 in 14 downTo 0) {
                    if (!isObstacle2(x2, y2) || isImage(x2, y2)) continue
                    val distance: Int = abs(x2 - parentX) + abs(y2 - parentY)
                    if (distance >= shortestDistance) continue
                    shortestDistance = distance
                    finalX = x2
                    finalY = y2
                }
            }

            x = finalX
            y = finalY
        }

        val grid: GestureImageView = gridArray[y][x]
        gridTypeArray[y][x] = GridType.IMAGE
        imageList.add(intArrayOf(x, y, id))

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

    private fun removeImage(array: IntArray) {
        val x = array[0]
        val y = array[1]
        if (!isValidCoordinates(x, y)) return
        gridTypeArray[y][x] = GridType.EXPLORED
        setObstacle(x, y)
    }

    fun isRobotMovable(array: IntArray): Boolean {
        if (array.size < 2) return false
        return isRobotMovable(array[0], array[1])
    }

    fun isRobotMovable(x: Int, y: Int): Boolean {
        for (yOffset in -1 .. 1) {
            for (xOffset in -1 .. 1) {
                if (!isValidCoordinates(x + xOffset, y + yOffset)) {
                    return false
                }

                if (obstacleArray[y + yOffset][x + xOffset] == 1) {
                    return false
                }
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
                    if (exploreArray[y + offset][x] == unexploredBit) {
                        return false
                    }
                } else {
                    if (exploreArray[y + offset][x] == exploredBit) {
                        return true
                    }
                }
            }

            return checkSingle
        }
    }

    protected fun scan(x: Int, y: Int, facing: Int, gridsAhead: Int = 1): BooleanArray {
        if (Math.floorMod(facing, 90) != 0) return booleanArrayOf(true, true, true)
        val ret1: Boolean = scanFront(x, y, facing, gridsAhead)
        val ret2: Boolean = scanRight(x, y, facing, gridsAhead)
        val ret3: Boolean = scanLeft(x, y, facing, gridsAhead)
        return booleanArrayOf(ret1, ret2, ret3)
    }

    private fun scanFront(x: Int, y: Int, facing: Int, gridsAhead: Int): Boolean {
        val gridsAheadNoObstacleList: ArrayList<IntArray> = arrayListOf()
        var ret = false

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

            if (!isValidCoordinates(newX, newY)) {
                ret = true
                continue
            }

            if (obstacleArray[newY][newX] == 1) {
                setObstacle(newX, newY)
                setExplored(newX, newY)
                ret = true
            } else {
                gridsAheadNoObstacleList.add(intArrayOf(newX, newY))
            }
        }

        scanUnreachable2(facing, gridsAheadNoObstacleList)
        return ret
    }

    private fun scanRight(x: Int, y: Int, facing: Int, gridsAhead: Int): Boolean {
        var ret = false

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

            if (!isValidCoordinates(newX, newY)) {
                ret = true
                continue
            }

            if (obstacleArray[newY][newX] == 1) {
                setObstacle(newX, newY)
                setExplored(newX, newY)
                ret = true
            }
        }

        return ret
    }

    private fun scanLeft(x: Int, y: Int, facing: Int, gridsAhead: Int): Boolean {
        var ret = false

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

            if (!isValidCoordinates(newX, newY)) {
                ret = true
                continue
            }

            if (obstacleArray[newY][newX] == 1) {
                setObstacle(newX, newY)
                setExplored(newX, newY)
                ret = true
            }
        }

        return ret
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
            GridType.FASTEST_PATH       -> grid.setBackgroundColor(Color.GREEN)

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
                    GridType.GOAL_POINT         -> grid.setBackgroundColor(context.getColor(R.color.arena_goal_point))
                    GridType.WAYPOINT           -> grid.setBackgroundColor(context.getColor(R.color.arena_way_point))
//                    GridType.START_POINT        -> grid.setBackgroundColor(context.getColor(R.color.arena_start_point))
//                    GridType.GOAL_POINT_TOUCHED -> grid.setBackgroundColor(context.getColor(R.color.arena_goal_point_touched))
//                    GridType.WAYPOINT_TOUCHED   -> grid.setBackgroundColor(context.getColor(R.color.arena_way_point_touched))
                    GridType.START_POINT        -> grid.setBackgroundColor(context.getColor(R.color.arena_explored))
                    GridType.GOAL_POINT_TOUCHED -> grid.setBackgroundColor(context.getColor(R.color.arena_explored))
                    GridType.WAYPOINT_TOUCHED   -> grid.setBackgroundColor(context.getColor(R.color.arena_explored))
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
                    setUnexplored(x, y)
                    obstacleArray[y][x] = 0
                }
            }
        }
    }

    private fun gridSingleTap(view: GestureImageView) {
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
    }

    private fun gridDoubleTap(view: GestureImageView) {
        if (currentFunction == PlotFunction.PLOT_OBSTACLE) {
            return
        }

        val coordinates: IntArray = getCoordinatesOfView(view)
        val x: Int = coordinates[0]
        val y: Int = coordinates[1]
        setWaypoint(x, y)
    }

    private fun gridLongPress(view: GestureImageView) {
        if (currentFunction == PlotFunction.PLOT_OBSTACLE) {
            val coordinates: IntArray = getCoordinatesOfView(view)
            val x: Int = coordinates[0]
            val y: Int = coordinates[1]

            if (isImage(x, y)) {
                var remove: IntArray? = null

                for (image in imageList) {
                    if (image[0] == x && image[1] == y) {
                        remove = image
                        removeImage(image)
                        break
                    }
                }

                if (remove != null) {
                    debugImageList.remove(remove[2])
                    Log.e("IM", "${imageList.size}")
                    imageList.remove(remove)
                    Log.e("IM", "${imageList.size}")
                }
            } else {
                if (debugImageList.size >= 5) {
                    callback(Callback.MESSAGE, "Cannot plot any more images.")
                    return
                }

                var id = (1..15).random()

                while (debugImageList.contains(id)) {
                    id = (1..15).random()
                }

                debugImageList.add(id)
                setImage(x, y, id)
            }

            return
        }

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
            if (!AUTO_UPDATE_ARENA && currentFunction == PlotFunction.NONE) {
                isWaitingUpdate = true
                callback(Callback.SEND_COMMAND, "$PC_PREFIX$SEND_ARENA_COMMAND")
                return
            }

            if (!SIM_MODE) return
            var currentSpeed: Int = sharedPreferences.getInt(context.getString(R.string.app_pref_simulation_speed), 2)
            currentSpeed--
            if (currentSpeed < 0) return
            SIM_DELAY = (1000 / (currentSpeed + 1)).toLong()
            sharedPreferences.edit().putInt(context.getString(R.string.app_pref_simulation_speed), currentSpeed).apply()
            callback(Callback.MESSAGE, context.getString(R.string.simulation_speed_notification, currentSpeed + 1))
            return
        }

        if (gesture == GestureImageView.Gesture.FLING_UP) {
            if (!SIM_MODE) return
            var currentSpeed: Int = sharedPreferences.getInt(context.getString(R.string.app_pref_simulation_speed), 2)
            currentSpeed++
            if (currentSpeed > 9) return
            SIM_DELAY = (1000 / (currentSpeed + 1)).toLong()
            sharedPreferences.edit().putInt(context.getString(R.string.app_pref_simulation_speed), currentSpeed).apply()
            callback(Callback.MESSAGE, context.getString(R.string.simulation_speed_notification, currentSpeed + 1))
            return
        }
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

    fun isStartPointExact(x: Int, y: Int): Boolean {
        return (startPosition[0] == x && startPosition[1] == y)
    }

    private fun isGoalPoint(x: Int, y: Int): Boolean {
        val gridType = gridTypeArray[y][x]
        return (gridType == GridType.GOAL_POINT || gridType == GridType.GOAL_POINT_TOUCHED)
    }

    fun isGoalPointExact(x: Int, y: Int): Boolean {
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

    private fun isObstacle2(x: Int, y: Int): Boolean {
        return (obstacleArray[y][x] == 1)
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
        val strings: ArrayList<String> = getMapDescriptorList()

        if (strings.size != 2) {
            callback(Callback.MESSAGE, context.getString(R.string.something_went_wrong))
            return ""
        }

        return "${strings[0]}${DESCRIPTOR_DIVIDER}${strings[1]}"
    }

    fun getMapDescriptorList(): ArrayList<String> {
        return MapDescriptor.fromArray(exploreArray, obstacleArray, exploredBit)
    }

    fun getImageList(): ArrayList<String> {
        val ret: ArrayList<String> = arrayListOf()

        for (image in imageList) {
            ret.add("(${image[2]}, ${image[0]}, ${image[1]})")
        }

        return ret
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
        return ((1.0 * coveredCount / 300) * 100 >= COVERAGE_LIMIT)
    }

    fun setPlotFunction(function: PlotFunction) {
        currentFunction = function
    }

    private fun isOccupied(x: Int, y: Int, robotSize: Boolean): Boolean {
        if (robotSize) {
            for (offsetY in -1 .. 1) {
                for (offsetX in -1..1) {
                    val newX = x + offsetX
                    val newY = y + offsetY
                    if (isWaypoint(newX, newY) || isStartPoint(newX, newY) || isGoalPoint(newX, newY) || isImage(newX, newY) || isObstacle(newX, newY)) return true
                }
            }

            return false
        } else {
            return (isWaypoint(x, y) || isStartPoint(x, y) || isGoalPoint(x, y) || isImage(x, y) || isObstacle(x, y))
        }
    }
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