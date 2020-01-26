package ntu.mdp.android.mdptestkotlin.main

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.ROBOT_FOOTPRINT
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.main.MainActivity.Companion.isPlotting
import ntu.mdp.android.mdptestkotlin.utils.GestureImageView


class ArenaController(private val context: Context, private val callback: (status: Status, message: String) -> Unit) {
    enum class Gesture {
        SINGLE_TAP,
        DOUBLE_TAP,
        LONG_PRESS,
        FLING_LEFT,
        FLING_RIGHT,
        FLING_DOWN
    }

    enum class PlotType {
        ROBOT,
        OBSTACLE,
        WAY_POINT,
        LIVE
    }

    enum class Status {
        WRITE,
        INFO,
        COORDINATES,
        ROBOT,
        STATUS,
        RESET
    }

    enum class PlotMode {
        NONE,
        PLOT_OBSTACLE,
        REMOVE_OBSTACLE
    }

    private var plotMode: PlotMode = PlotMode.NONE
    private var lastRobotPosition: IntArray = intArrayOf(-1, -1, 0)
    private var lastStartPosition: IntArray = intArrayOf(-1, -1)
    private var lastWayPointPosition: IntArray = intArrayOf(-1, -1)
    private var isUndo = false

    private val undoActionList: ArrayList<Pair<Int, IntArray>> = arrayListOf()
    private val redoActionList: ArrayList<Pair<Int, IntArray>> = arrayListOf()
    private val savedStateMap: HashMap<Int, Int> = hashMapOf()
    private val arenaGridLayout: GridLayout = (context as Activity).findViewById(R.id.main_grid_arena)
    private val robotGridLayout: GridLayout = (context as Activity).findViewById(R.id.main_grid_robot)
    private val imageGridLayout: GridLayout = (context as Activity).findViewById(R.id.main_grid_image)
    private val gridLayoutList: List<GridLayout> = listOf(arenaGridLayout, robotGridLayout, imageGridLayout)
    private val arenaArray: Array<Array<GestureImageView>> = Array(20) { Array(15) { GestureImageView(context) } }
    private val robotArray: Array<Array<ImageView>> = Array(20) { Array(15) { ImageView(context) } }
    private val imageArray: Array<Array<TextView>> = Array(20) { Array(15) { TextView(context) } }
    private val arenaStateArray: Array<Array<Int>> = Array(20) { Array(15) { -1 } }
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    private val dp: Int = (displayMetrics.widthPixels * 0.66).toInt()
    private val gridSize: Int = ((dp - 30) / 15)
    private val robotSize: Int = (dp / 15)

    private val gestureCallback: (view: GestureImageView, gesture: Gesture) -> Unit = { view, gesture ->
        when (gesture) {
            Gesture.SINGLE_TAP -> {
                if (isPlotting && (plotMode == PlotMode.PLOT_OBSTACLE || plotMode == PlotMode.REMOVE_OBSTACLE)) {
                    doObstaclePlot(view)
                    if (redoActionList.isNotEmpty()) {
                        redoActionList.clear()
                    }
                }
            }

            Gesture.LONG_PRESS -> {
                if (plotMode == PlotMode.NONE) {
                    val coordinates: Pair<Int, Int> = getCoordinates(view)
                    var x = coordinates.first
                    var y = coordinates.second

                    if (x >= 0 && y >= 0) {
                        if (x > 13) x = 13
                        if (x < 1) x = 1
                        if (y > 18) y = 18
                        if (y < 1) y = 1

                        if (isClear(x, y, PlotType.ROBOT)) {
                            val oldX = lastStartPosition[0]
                            val oldY = lastStartPosition[1]

                            if (oldX >= 0 && oldY >= 0) {
                                for (Y in -1 until 2) {
                                    for (X in -1 until 2) {
                                        val color: Int = when (arenaStateArray[oldY - Y][oldX + X]) {
                                            -1 -> context.getColor(R.color.arena_unexplored)
                                            0 -> context.getColor(R.color.arena_explored)
                                            else -> Color.BLACK
                                        }

                                        arenaArray[oldY - Y][oldX + X].setBackgroundColor(color)
                                    }
                                }
                            }

                            lastStartPosition[0] = x
                            lastStartPosition[1] = y
                            updateRobot("$x, $y, 0")

                            for (Y in -1 until 2) {
                                for (X in -1 until 2) {
                                    arenaArray[y - Y][x + X].setBackgroundColor(context.getColor(R.color.arena_start_point))
                                }
                            }

                            callback(Status.WRITE, "#startpoint::$x, $y, 0")
                        } else {
                            callback(Status.INFO, "Obstructed, cannot plot.")
                        }
                    }
                }
            }

            Gesture.DOUBLE_TAP -> {
                if (plotMode == PlotMode.NONE) {
                    val coordinates: Pair<Int, Int> = getCoordinates(view)
                    val x = coordinates.first
                    val y = coordinates.second

                    if (x >= 0 && y >= 0) {
                        if (isClear(x, y, PlotType.WAY_POINT)) {
                            val oldX = lastWayPointPosition[0]
                            val oldY = lastWayPointPosition[1]

                            if (oldX >= 0 && oldY >= 0) {
                                val color: Int = when (arenaStateArray[oldY][oldX]) {
                                    -1 -> context.getColor(R.color.arena_unexplored)
                                    0 -> context.getColor(R.color.arena_explored)
                                    else -> Color.BLACK
                                }

                                arenaArray[oldY][oldX].setBackgroundColor(color)
                            }

                            lastWayPointPosition[0] = x
                            lastWayPointPosition[1] = y
                            view.setBackgroundColor(context.getColor(R.color.arena_way_point))
                            callback(Status.WRITE, "#waypoint::$x, $y")
                        } else {
                            callback(Status.INFO, "Obstructed, cannot plot.")
                        }
                    }
                }
            }

            Gesture.FLING_LEFT, Gesture.FLING_RIGHT -> {
                if (isPlotting && (plotMode == PlotMode.PLOT_OBSTACLE || plotMode == PlotMode.REMOVE_OBSTACLE)) {
                    if (gesture == Gesture.FLING_LEFT) {
                        if (undoActionList.isNotEmpty()) {
                            val index = undoActionList.size - 1
                            val action: Pair<Int, IntArray> = undoActionList.removeAt(index)
                            val x = action.second[0]
                            val y = action.second[1]
                            val originalAction: PlotMode = plotMode

                            plotMode = if (action.first == 1) {
                                PlotMode.REMOVE_OBSTACLE
                            } else {
                                PlotMode.PLOT_OBSTACLE
                            }

                            redoActionList.add(action)
                            isUndo = true
                            doObstaclePlot(x, y)
                            isUndo = false
                            plotMode = originalAction
                        }
                    } else {
                        if (redoActionList.isNotEmpty()) {
                            val index = redoActionList.size - 1
                            val action: Pair<Int, IntArray> = redoActionList.removeAt(index)
                            val x = action.second[0]
                            val y = action.second[1]
                            val originalAction: PlotMode = plotMode

                            plotMode = if (action.first != 1) {
                                PlotMode.REMOVE_OBSTACLE
                            } else {
                                PlotMode.PLOT_OBSTACLE
                            }

                            doObstaclePlot(x, y)
                            plotMode = originalAction
                        }
                    }
                } else {
                    callback(Status.RESET, "")
                }
            }

            Gesture.FLING_DOWN -> {
                if (!autoUpdateArena && !isPlotting) {
                    MainActivity.isUpdating = true
                    callback(Status.WRITE, SEND_ARENA_COMMAND)
                }
            }
        }
    }

    init {
        for (y in 19 downTo 0) {
            for (x in 0 .. 14) {
                val gestureImageView = GestureImageView(context)
                gestureImageView.setCallback(gestureCallback)
                gestureImageView.layoutParams = LinearLayout.LayoutParams(gridSize, gridSize).apply {
                    bottomMargin = 1
                    leftMargin = 1
                    rightMargin = 1
                    topMargin = 1
                }

                gestureImageView.setBackgroundColor(context.getColor(R.color.arena_unexplored))
                gestureImageView.isClickable = false
                arenaArray[y][x] = gestureImageView
                arenaGridLayout.addView(gestureImageView)

                val imageView = ImageView(context)
                imageView.layoutParams = LinearLayout.LayoutParams(robotSize, robotSize)
                imageView.setBackgroundColor(Color.TRANSPARENT)
                imageView.alpha = 0.0f
                imageView.isClickable = false
                imageView.isFocusable = false
                robotArray[y][x] = imageView
                robotGridLayout.addView(imageView)

                val textView = TextView(context)
                textView.layoutParams = LinearLayout.LayoutParams(gridSize, gridSize).apply {
                    bottomMargin = 1
                    leftMargin = 1
                    rightMargin = 1
                    topMargin = 1
                }

                textView.text = ""
                textView.gravity = Gravity.CENTER
                textView.setTextColor(Color.WHITE)
                textView.setTypeface(textView.typeface, Typeface.BOLD)
                textView.isClickable = false
                textView.isFocusable = false
                textView.alpha = 0.0f
                imageArray[y][x] = textView
                imageGridLayout.addView(textView)
            }
        }

        robotGridLayout.visibility = View.VISIBLE
        imageGridLayout.visibility = View.VISIBLE
        updateRobot("0, 0, 0")
    }

    fun resetArena() {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                arenaStateArray[y][x] = -1
                arenaArray[y][x].setBackgroundColor(context.getColor(R.color.arena_unexplored))

                imageArray[y][x].let { i ->
                    i.text = ""
                    i.alpha = 0.0f
                }

                robotArray[y][x].let { i ->
                    i.requestLayout()
                    i.layoutParams.height = robotSize
                    i.layoutParams.width = robotSize
                    i.setImageResource(android.R.color.transparent)
                    i.alpha = 0.0f
                }
            }
        }

        lastRobotPosition = intArrayOf(-1, -1, 0)
        lastStartPosition = intArrayOf(-1, -1)
        lastWayPointPosition = intArrayOf(-1, -1)
        updateRobot("0, 0, 0")
    }

    fun updateArena(data: String) {
        var counter = 0

        for (i in data.indices) {
            var binary: String = data[i].toString().toInt(16).toString(2)
            binary = binary.padStart(4, '0')

            for (j in binary.indices) {
                val bit: Int = binary[j].toString().toInt()
                val y = Math.floorDiv(counter, 15)
                val x = (counter % 15)

                if (bit == 1) {
                    arenaArray[y][x].setBackgroundColor(Color.BLACK)
                } else {
                    if (isClear(x, y, PlotType.LIVE)) {
                        val color: Int = if (bit == 0) R.color.arena_explored else R.color.arena_unexplored
                        arenaArray[y][x].setBackgroundColor(context.getColor(color))
                    }
                }

                arenaStateArray[y][x] = bit
                counter++
            }
        }
    }

    fun resetGridColors() {
        var oldX = lastStartPosition[0]
        var oldY = lastStartPosition[1]

        if (oldX >= 0 && oldY >= 0) {
            for (Y in -1 until 2) {
                for (X in -1 until 2) {
                    val color: Int = when (arenaStateArray[oldY - Y][oldX + X]) {
                        -1 -> context.getColor(R.color.arena_unexplored)
                        0 -> context.getColor(R.color.arena_explored)
                        else -> Color.BLACK
                    }

                    arenaArray[oldY - Y][oldX + X].setBackgroundColor(color)
                }
            }
        }

        oldX = lastWayPointPosition[0]
        oldY = lastWayPointPosition[1]

        if (oldX >= 0 && oldY >= 0) {
            val color: Int = when (arenaStateArray[oldY][oldX]) {
                -1 -> context.getColor(R.color.arena_unexplored)
                0 -> context.getColor(R.color.arena_explored)
                else -> Color.BLACK
            }

            arenaArray[oldY][oldX].setBackgroundColor(color)
        }

        lastStartPosition = intArrayOf(-1, -1)
        lastWayPointPosition = intArrayOf(-1, -1)
        callback(Status.WRITE, "#startpoint::[-1, -1]")
        callback(Status.WRITE, "#waypoint::[-1, -1]")
    }

    fun updateImage(data: String) {
        val s = data.split(", ")

        if (s.size != 3) {
            callback(Status.INFO, "Something went wrong.")
            return
        }

        try {
            val x = s[0].toInt()
            val y = s[1].toInt()
            val textView: TextView = imageArray[y][x]
            val bit: Int = arenaStateArray[y][x]
            textView.text = s[2]
            textView.alpha = 1.0f

            if (bit != 1) {
                callback(Status.INFO, "Image not on an obstacle???")
            }
        } catch (e: NumberFormatException) {
            Log.e(this::class.simpleName ?: "-", "Something went wrong.", e)
            callback(Status.INFO, "Something went wrong.")
            return
        }
    }

    fun updateRobot(data: String) {
        val s = data.split(", ")

        if (s.size != 3) {
            callback(Status.INFO, "Something went wrong.")
            return
        }

        try {
            var newX = s[0].toInt()
            var newY = s[1].toInt()
            val newR = s[2].toInt()
            if (newX < 1) newX = 1
            if (newX > 13) newX = 13
            if (newY < 1) newY = 1
            if (newY > 18) newY = 18

            val oldX: Int = lastRobotPosition[0] - 1
            val oldY: Int = lastRobotPosition[1] + 1
            var imageView: ImageView

            if (oldX >= 0 && oldY >= 0) {
                imageView = robotArray[oldY][oldX]
                imageView.requestLayout()
                imageView.layoutParams.height = robotSize
                imageView.layoutParams.width = robotSize
                imageView.setImageResource(android.R.color.transparent)
                imageView.alpha = 0.0f
            }

            val drawable: Int = when (newR) {
                0 -> R.drawable.ic_0
                90 -> R.drawable.ic_90
                180 -> R.drawable.ic_180
                else -> R.drawable.ic_270
            }

            lastRobotPosition[0] = newX
            lastRobotPosition[1] = newY
            lastRobotPosition[2] = newR
            callback(Status.COORDINATES, "$newX, $newY")

            newX -= 1
            newY += 1
            imageView = robotArray[newY][newX]
            imageView.requestLayout()
            imageView.layoutParams.height = robotSize * ROBOT_FOOTPRINT
            imageView.layoutParams.width = robotSize * ROBOT_FOOTPRINT
            imageView.setImageResource(drawable)
            imageView.alpha = 1.0f

            for (y in 0 until ROBOT_FOOTPRINT) {
                for (x in 0 until ROBOT_FOOTPRINT) {
                    val bit = arenaStateArray[newY - y][newX + x]

                    if (bit == -1) {
                        arenaStateArray[newY - y][newX + x] = 0
                        arenaArray[newY - y][newX + x].setBackgroundColor(context.getColor(R.color.arena_explored))
                    }
                }
            }
        } catch (e: NumberFormatException) {
            Log.e(this::class.simpleName ?: "-", "Something went wrong.", e)
            callback(Status.INFO, "Something went wrong.")
            return
        }
    }

    private fun checkClear(dir: Int): Boolean {
        var x = lastRobotPosition[0]
        var y = lastRobotPosition[1]
        val r = lastRobotPosition[2]
        Log.d(this::class.simpleName ?: "-", "Robot's current position: [$x, $y] facing $r.")
        val obstacleMessage = "Obstacle ahead. Cannot move."
        val edgeMessage = "At edge of arena. Cannot move."

        if (x == -1 || y == -1) {
            callback(Status.INFO, "Robot position unknown. Cannot move.")
            return false
        }

        if ((r % 180) == 0) {
            if (dir == 1) {
                if ((r == 0 && y + 2 > 19) || (r == 180 && y - 2 < 0)) {
                    callback(Status.ROBOT, edgeMessage)
                    callback(Status.STATUS, "Blocked")
                    return false
                }

                if (r == 0) y += 2 else y -= 2
            } else {
                if ((r == 180 && y + 2 > 19) || (r == 0 && y - 2 < 0)) {
                    callback(Status.ROBOT, edgeMessage)
                    callback(Status.STATUS, "Blocked")
                    return false
                }

                if (r == 180) y += 2 else y -= 2
            }

            if (arenaStateArray[y][x] == 1 || arenaStateArray[y][x + 1] == 1 || arenaStateArray[y][x - 1] == 1) {
                callback(Status.ROBOT, obstacleMessage)
                callback(Status.STATUS, "Blocked")
                return false
            }

            return true
        }

        if (dir == 1) {
            if ((r == 90 && x + 2 > 14) || (r == 270 && x - 2 < 0)) {
                callback(Status.ROBOT, edgeMessage)
                callback(Status.STATUS, "Blocked")
                return false
            }

            if (r == 90) x += 2 else x -= 2
        } else {
            if ((r == 270 && x + 2 > 14) || (r == 90 && x - 2 < 0)) {
                callback(Status.ROBOT, edgeMessage)
                callback(Status.STATUS, "Blocked")
                return false
            }

            if (r == 270) x += 2 else x -= 2
        }

        if (arenaStateArray[y][x] == 1 || arenaStateArray[y - 1][x] == 1 || arenaStateArray[y + 1][x] == 1) {
            callback(Status.ROBOT, obstacleMessage)
            callback(Status.STATUS, "Blocked")
            return false
        }

        return true
    }

    fun moveRobot(dir: Int, connected: Boolean = false) {
        if (!checkClear(dir)) return
        if (dir == 1) callback(Status.STATUS, "Moving") else callback(Status.STATUS, "Reversing")

        if (dir == 1 && connected) {
            callback(Status.WRITE, App.sharedPreferences.getString(context.getString(R.string.app_pref_forward), context.getString(R.string.forward_default))!!)
            return
        }

        if (dir == -1 && connected) {
            callback(Status.WRITE, App.sharedPreferences.getString(context.getString(R.string.app_pref_reverse), context.getString(R.string.reverse_default))!!)
            return
        }

        val x = lastRobotPosition[0]
        val y = lastRobotPosition[1]
        val move: Int = (1 * dir)

        when (val r = lastRobotPosition[2]) {
            0 -> updateRobot("$x, ${y + move}, $r")
            90 -> updateRobot("${x + move}, $y, $r")
            180 -> updateRobot("$x, ${y - move}, $r")
            else -> updateRobot("${x - move}, $y, $r")
        }
    }

    fun getRobotFacing(): Int {
        return lastRobotPosition[2]
    }

    fun turnRobot(dir: Int, connected: Boolean = false) {
        if (dir == 1) callback(Status.STATUS, "Turning Right") else callback(Status.STATUS, "Turning Left")

        if (dir == -1 && connected) {
            callback(Status.WRITE, App.sharedPreferences.getString(context.getString(R.string.app_pref_turn_left), context.getString(R.string.turn_left_default))!!)
            return
        }

        if (dir == 1 && connected) {
            callback(Status.WRITE, App.sharedPreferences.getString(context.getString(R.string.app_pref_turn_right), context.getString(R.string.turn_right_default))!!)
            return
        }


        val x = lastRobotPosition[0]
        val y = lastRobotPosition[1]
        var r = lastRobotPosition[2]
        val move: Int = (90 * dir)
        r += move
        r = Math.floorMod(r, 360)
        updateRobot("$x, $y, $r")
        Log.d(this::class.simpleName ?: "-", "Robot's current position: [$x, $y] facing $r.")
    }

    private fun getCoordinates(view: GestureImageView): Pair<Int, Int> {
        var x = -1
        var y = -1

        for (testY in 0 until 20) {
            x = arenaArray[testY].indexOf(view)

            if (x != -1) {
                y = testY
                break
            }
        }

        return Pair(x, y)
    }

    private fun isClear(x: Int, y: Int, type: PlotType): Boolean {
        if (type == PlotType.ROBOT) {
            for (Y in -1 until 2) {
                for (X in -1 until 2) {
                    if (arenaStateArray[y - Y][x + X] == 1) {
                        return false
                    }
                }
            }
        } else {
            if (type == PlotType.OBSTACLE) {
                val startX = lastRobotPosition[0]
                val clashArrayX = intArrayOf(startX, startX + 1, startX - 1)
                val startY = lastRobotPosition[1]
                val clashArrayY = intArrayOf(startY, startY - 1, startY + 1)

                if (clashArrayX.contains(x) && clashArrayY.contains(y)) {
                    return false
                }
            }

            val startX = lastStartPosition[0]
            val clashArrayX = intArrayOf(startX, startX + 1, startX - 1)
            val startY = lastStartPosition[1]
            val clashArrayY = intArrayOf(startY, startY - 1, startY + 1)

            if (clashArrayX.contains(x) && clashArrayY.contains(y)) {
                return false
            }

            if (x == lastWayPointPosition[0] && y == lastWayPointPosition[1]) {
                return false
            }

            if (type != PlotType.LIVE) {
                if (arenaStateArray[y][x] == 1) {
                    return false
                }
            }
        }

        return true
    }

    private fun doObstaclePlot(view: GestureImageView) {
        val coordinates: Pair<Int, Int> = getCoordinates(view)
        val x = coordinates.first
        val y = coordinates.second
        doObstaclePlot(x, y)
    }

    private fun doObstaclePlot(x: Int, y: Int) {
        val index = getIndex(x, y)

        if (plotMode == PlotMode.REMOVE_OBSTACLE) {
            if (x >= 0 && y >= 0) {
                if (arenaStateArray[y][x] == 1) {
                    var bit = 0

                    if (savedStateMap.containsKey(index)) {
                        bit = savedStateMap[index] ?: 0
                    }

                    if (bit == -1) {
                        arenaArray[y][x].setBackgroundColor(context.getColor(R.color.arena_unexplored))
                    } else {
                        bit = 0
                        arenaArray[y][x].setBackgroundColor(context.getColor(R.color.arena_explored))
                    }

                    arenaStateArray[y][x] = bit

                    if (!isUndo) {
                        undoActionList.add(Pair(-1, intArrayOf(x, y)))
                    }
                }
            }
        } else if (plotMode == PlotMode.PLOT_OBSTACLE) {
            if (x >= 0 && y >= 0) {
                if (isClear(x, y, PlotType.OBSTACLE)) {
                    if (!savedStateMap.containsKey(index)) {
                        val bit: Int = arenaStateArray[y][x]
                        savedStateMap[index] = bit
                    }

                    arenaArray[y][x].setBackgroundColor(Color.BLACK)
                    arenaStateArray[y][x] = 1

                    if (!isUndo) {
                        undoActionList.add(Pair(1, intArrayOf(x, y)))
                    }
                } else {
                    callback(Status.INFO, "Obstructed, cannot plot.")
                }
            }
        }
    }

    private fun getIndex(x: Int, y: Int): Int {
        return (15 * y + x)
    }

    fun resetActions() {
        undoActionList.clear()
        redoActionList.clear()
    }

    fun getGridLayouts(): List<GridLayout> = gridLayoutList

    fun togglePlotMode() {


        /*
        val scaleAfter = if (isPlotting) 1.16f else 0.8333333333334f
        val anim: Animation = ScaleAnimation(
            1.0f, scaleAfter,  // Start and end values for the X axis scaling
            1.0f, scaleAfter,  // Start and end values for the Y axis scaling
            Animation.RELATIVE_TO_SELF, 0f,  // Pivot point of X scaling
            Animation.RELATIVE_TO_SELF, 0f) // Pivot point of Y scaling
        anim.duration = 350
        anim.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                gridLayoutList.forEach {
                    it.pivotX = 0.0f
                    it.pivotY = 0.0f
                    it.scaleX = if (isPlotting) 1.2f else 1.0f
                    it.scaleY = if (isPlotting) 1.2f else 1.0f
                }
            }
        })

        gridLayoutList.forEach { it.startAnimation(anim) }
         */
    }
}