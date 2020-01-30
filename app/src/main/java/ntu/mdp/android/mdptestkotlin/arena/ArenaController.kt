package ntu.mdp.android.mdptestkotlin.arena

import android.content.Context
import android.util.Log
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.isSimple
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.testExplore
import ntu.mdp.android.mdptestkotlin.App.Companion.usingAmd
import ntu.mdp.android.mdptestkotlin.MainActivityController
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.arena.Arena.Companion.exploredBit
import ntu.mdp.android.mdptestkotlin.utils.GestureImageView


class ArenaController(private val context: Context, val activityCallback: (callback: Callback, message: String) -> Unit) {
    enum class PlotMode {
        NONE,
        PLOT_OBSTACLE,
        REMOVE_OBSTACLE
    }

    enum class Callback {
        INFO,
        WRITE,
        STATUS,
        COORDINATES,
        LONG_PRESS_CHOICE,
        ROBOT,
        MESSAGE,
        RESET
    }

    var plotMode: PlotMode = PlotMode.NONE

    private val gestureCallback: (view: GestureImageView, gesture: GestureImageView.Gesture) -> Unit = { view, gesture ->
        when (gesture) {
            GestureImageView.Gesture.SINGLE_TAP -> gridSingleTap(view)
            GestureImageView.Gesture.DOUBLE_TAP -> gridDoubleTap(view)
            GestureImageView.Gesture.LONG_PRESS -> gridLongPress(view)

            GestureImageView.Gesture.FLING_LEFT, GestureImageView.Gesture.FLING_RIGHT -> {
                if (MainActivityController.isPlotting && (plotMode == PlotMode.PLOT_OBSTACLE || plotMode == PlotMode.REMOVE_OBSTACLE)) {
                    if (gesture == GestureImageView.Gesture.FLING_LEFT) undoAction()
                    else redoAction()
                } else {
                    activityCallback(Callback.RESET, "")
                }
            }

            GestureImageView.Gesture.FLING_DOWN -> {
                if (!autoUpdateArena && !MainActivityController.isPlotting) {
                    MainActivityController.isUpdating = true
                    activityCallback(Callback.WRITE, SEND_ARENA_COMMAND)
                }
            }
        }
    }

    private val scale: Double = if (isSimple) 0.81 else 0.66
    private val dp: Int = (context.resources.displayMetrics.widthPixels * scale).toInt()
    private val gridSize: Int = ((dp - 30) / 15)
    private val robotSize: Int = (dp / 15)
    private val undoActionList: ArrayList<Pair<Int, Pair<Int, Int>>> = arrayListOf()
    private val redoActionList: ArrayList<Pair<Int, Pair<Int, Int>>> = arrayListOf()
    private var viewOnHold: GestureImageView = GestureImageView(context)

    val arena: Arena = Arena(context, gridSize, robotSize, gestureCallback) { status, message ->
        when (status) {
            Arena.Callback.INFO -> activityCallback(Callback.INFO, message)
            Arena.Callback.WRITE -> activityCallback(Callback.WRITE, message)
            Arena.Callback.COORDINATES -> activityCallback(Callback.COORDINATES, message)
            Arena.Callback.ROBOT -> activityCallback(Callback.ROBOT, message)
        }
    }

    init {
        arena.setRobotStartGoalPoint(1, 1, true)
        arena.setRobotStartGoalPoint(13, 18, false)
    }

    fun resetArena() {
        arena.reset()
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
                Log.e("PLOT", "$x, $y, $bit")
                arena.plot(x, y, if (bit == exploredBit) Arena.GridType.EXPLORED else Arena.GridType.UNEXPLORED)
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

                if (bit == 1) {
                    arena.plot(x, y, Arena.GridType.OBSTACLE)
                } else {
                    arena.removeObstacle(x, y)
                }

                counter++
            }
        }
    }

    fun resetObstacles() {
        arena.resetObstacles()
        resetActions()
    }

    fun updateImage(data: String) {
        val s = data.split(", ")

        try {
            val x = s[0].toInt()
            val y = s[1].toInt()
            val id = s[2].toInt()
            arena.setImage(x, y, id)
        } catch (e: NumberFormatException) {
            Log.e(this::class.simpleName ?: "-", "Something went wrong.", e)
            activityCallback(Callback.INFO, "Something went wrong.")
            return
        }
    }

    fun updateRobot(data: String) {
        val s = data.split(", ")

        try {
            val x = s[0].toInt()
            val y = s[1].toInt()
            val r = s[2].toInt()
            arena.moveRobot(x, y, r)
        } catch (e: NumberFormatException) {
            Log.e(this::class.simpleName ?: "-", "Something went wrong.", e)
            activityCallback(Callback.INFO, "Something went wrong.")
            return
        }
    }

    fun resetActions() {
        undoActionList.clear()
        redoActionList.clear()
    }

    fun moveRobot(direction: Int, connected: Boolean) {
        val robotCoordinates: IntArray = arena.robotCoordinates
        var x = robotCoordinates[0]
        var y = robotCoordinates[1]
        val r = robotCoordinates[2]
        val check = direction * 2
        //Log.d(this::class.simpleName ?: "-", "Robot's current position: [$x, $y] facing $r.")

        when (r) {
            0 -> for (xOffset in -1..1) if (!arena.isGridMovable(x + xOffset, y + check)) return
            90 -> for (yOffset in -1..1) if (!arena.isGridMovable(x + check, y + yOffset)) return
            180 -> for (xOffset in -1..1) if (!arena.isGridMovable(x + xOffset, y - check)) return
            else -> for (yOffset in -1..1) if (!arena.isGridMovable(x - check, y + yOffset)) return
        }

        if (direction == 1) {
            activityCallback(Callback.STATUS, context.getString(R.string.moving))
        } else {
            activityCallback(Callback.STATUS, context.getString(R.string.reversing))
        }

        if (direction == 1 && connected) {
            activityCallback(Callback.WRITE, sharedPreferences.getString(context.getString(R.string.app_pref_forward), context.getString(R.string.forward_default))!!)
            return
        }

        if (direction == -1 && connected) {
            activityCallback(Callback.WRITE, sharedPreferences.getString(context.getString(R.string.app_pref_reverse), context.getString(R.string.reverse_default))!!)
            return
        }

        val move: Int = (1 * direction)

        when (r) {
            0 -> y += move
            90 -> x += move
            180 -> y -= move
            else -> x -= move
        }

        updateRobot("$x, $y, $r")
    }

    fun turnRobot(direction: Int, connected: Boolean) {
        if (direction == 1) {
            activityCallback(Callback.STATUS, context.getString(R.string.turning_right))
        } else {
            activityCallback(Callback.STATUS, context.getString(R.string.turning_left))
        }

        if (direction == 1 && connected) {
            activityCallback(Callback.WRITE, sharedPreferences.getString(context.getString(R.string.turn_right), context.getString(R.string.turn_right_default))!!)
            return
        }

        if (direction == -1 && connected) {
            activityCallback(Callback.WRITE, sharedPreferences.getString(context.getString(R.string.app_pref_turn_left), context.getString(R.string.turn_left_default))!!)
            return
        }

        val robotCoordinates: IntArray = arena.robotCoordinates
        val x = robotCoordinates[0]
        val y = robotCoordinates[1]
        var r = robotCoordinates[2]
        val move: Int = (90 * direction)
        r += move
        r = Math.floorMod(r, 360)
        updateRobot("$x, $y, $r")
    }

    fun getRobotFacing(): Int = arena.robotCoordinates[2]

    private fun gridSingleTap(view: GestureImageView) {
        if (MainActivityController.isPlotting && (plotMode == PlotMode.PLOT_OBSTACLE || plotMode == PlotMode.REMOVE_OBSTACLE)) {
            val coordinates: Pair<Int, Int> = arena.getCoordinatesOfView(view)
            val x: Int = coordinates.first
            val y: Int = coordinates.second

            if (plotMode == PlotMode.PLOT_OBSTACLE) {
                arena.plot(x, y, Arena.GridType.OBSTACLE)
                undoActionList.add(Pair(1, Pair(x, y)))
            } else {
                arena.removeObstacle(x, y)
                undoActionList.add(Pair(-1, Pair(x, y)))
            }
        }
    }

    private fun gridDoubleTap(view: GestureImageView) {
        if (MainActivityController.isPlotting && (plotMode == PlotMode.PLOT_OBSTACLE || plotMode == PlotMode.REMOVE_OBSTACLE)) {
            gridSingleTap(view)
        } else {
            val coordinates: Pair<Int, Int> = arena.getCoordinatesOfView(view)
            val x: Int = coordinates.first
            val y: Int = coordinates.second
            arena.setWaypoint(x, y)
        }
    }

    private fun gridLongPress(view: GestureImageView) {
        activityCallback(Callback.LONG_PRESS_CHOICE, "")
        viewOnHold = view
    }

    fun selectPoint(isStart: Boolean) {
        val coordinates: Pair<Int, Int> = arena.getCoordinatesOfView(viewOnHold)
        val x: Int = coordinates.first
        val y: Int = coordinates.second
        arena.setRobotStartGoalPoint(x, y, isStart)
    }

    private fun undoAction() {
        if (undoActionList.isNotEmpty()) {
            val index = undoActionList.size - 1
            val action: Pair<Int, Pair<Int, Int>> = undoActionList.removeAt(index)
            val x = action.second.first
            val y = action.second.second

            if (action.first == 1) {
                arena.removeObstacle(x, y)
            } else {
                arena.plot(x, y, Arena.GridType.OBSTACLE)
            }

            redoActionList.add(action)
        }
    }

    private fun redoAction() {
        if (redoActionList.isNotEmpty()) {
            val index = redoActionList.size - 1
            val action: Pair<Int, Pair<Int, Int>> = redoActionList.removeAt(index)
            val x = action.second.first
            val y = action.second.second

            if (action.first != 1) {
                arena.removeObstacle(x, y)
                undoActionList.add(Pair(-1, Pair(x, y)))
            } else {
                arena.plot(x, y, Arena.GridType.OBSTACLE)
                undoActionList.add(Pair(1, Pair(x, y)))
            }
        }
    }

    fun updateRobotImage() {
        arena.setRobotImage(getRobotFacing())
    }

    fun saveObstacles() {
        arena.saveObstacles()
    }

    fun isWaypointSet(): Boolean = arena.isWaypointSet()
    fun resetGoalPoint() = arena.resetGoalPoint()

    fun getMapDescriptor(): String {
        var tempString = "11"
        arena.gridStateArray.forEach { it.forEach { state -> tempString += state.toString() }}
        tempString += "11"

        if (tempString.length % 4 != 0) {
            activityCallback(Callback.INFO, context.getString(R.string.something_went_wrong))
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
        arena.obstacleArray.forEach { it.forEach { state -> tempString += state.toString() }}
        Log.e("tempString", "$tempString, ${tempString.length}")
        val pad = Math.floorMod(tempString.length, 4)
        tempString = "".padEnd(pad, '0') + tempString
        Log.e("tempString", "$tempString, ${tempString.length}")

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
}