package ntu.mdp.android.mdptestkotlin.main

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.RotateAnimation
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.get
import ntu.mdp.android.mdptestkotlin.App.Companion.ROBOT_FOOTPRINT
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.MainActivity
import ntu.mdp.android.mdptestkotlin.R
import kotlin.time.seconds

class ArenaController(private val context: Context, private val callback: (status: Status, message: String) -> Unit) {
    enum class Op {
        NONE,
        SET_START_POINT,
        SET_WAY_POINT
    }

    enum class Status {
        WRITE,
        INFO,
        COORDINATES,
        ROBOT
    }

    var currentOp: Op = Op.NONE
    var lastRobotPosition: IntArray = intArrayOf(-1, -1, 0)
    private var lastStartPosition: IntArray = intArrayOf(-1, -1)
    private var lastWayPointPosition: IntArray = intArrayOf(-1, -1)

    private val arenaGridLayout: GridLayout = (context as Activity).findViewById(R.id.main_grid_arena)
    private val robotGridLayout: GridLayout = (context as Activity).findViewById(R.id.main_grid_robot)
    private val imageGridLayout: GridLayout = (context as Activity).findViewById(R.id.main_grid_image)
    private val arenaArray: Array<Array<ImageView>> = Array(20) { Array(15) { ImageView(context) } }
    private val robotArray: Array<Array<ImageView>> = Array(20) { Array(15) { ImageView(context) } }
    private val imageArray: Array<Array<TextView>> = Array(20) { Array(15) { TextView(context) } }
    private val arenaStateArray: Array<Array<Int>> = Array(20) { Array(15) { -1 } }
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    private val dp: Int = (displayMetrics.widthPixels * 0.66).toInt()
    private val gridSize: Int = ((dp - 30) / 15)
    private val robotSize: Int = (dp / 15)
    private val clickGrid = View.OnClickListener { view ->
        if (currentOp == Op.NONE && !autoUpdateArena) {
            MainActivity.isUpdating = true
            callback(Status.WRITE, SEND_ARENA_COMMAND)
            return@OnClickListener
        }

        if (currentOp == Op.SET_START_POINT || currentOp == Op.SET_WAY_POINT) {
            var x = -1
            var y = -1

            for (testY in 0 until 20) {
                x = arenaArray[testY].indexOf(view as ImageView)

                if (x != -1) {
                    y = testY
                    break
                }
            }

            if (x == -1 || y == -1) return@OnClickListener

            if (currentOp == Op.SET_START_POINT) {
                val oldX = lastStartPosition[0]
                val oldY = lastStartPosition[1]

                if (oldX >= 0 && oldY >= 0) {
                    for (Y in 0 until ROBOT_FOOTPRINT) {
                        for (X in 0 until ROBOT_FOOTPRINT) {
                            val color: Int = when (arenaStateArray[oldY - Y][oldX + X]) {
                                -1 -> context.getColor(R.color.arena_unexplored)
                                0 -> context.getColor(R.color.arena_explored)
                                else -> Color.BLACK
                            }

                            arenaArray[oldY - Y][oldX + X].setBackgroundColor(color)
                        }
                    }
                }

                if (x > 12) x = 12
                if (y < 2) y = 2

                lastStartPosition[0] = x
                lastStartPosition[1] = y

                for (Y in 0 until ROBOT_FOOTPRINT) {
                    for (X in 0 until ROBOT_FOOTPRINT) {
                        arenaArray[y - Y][x + X].setBackgroundColor(context.getColor(R.color.arena_start_point))
                    }
                }

                updateRobot("$x, $y, 0")
            } else {
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
            }

            currentOp = Op.NONE
        }
    }

    init {
        for (y in 19 downTo 0) {
            for (x in 0 .. 14) {
                var imageView = ImageView(context)
                imageView.layoutParams = LinearLayout.LayoutParams(gridSize, gridSize).apply {
                    bottomMargin = 1
                    leftMargin = 1
                    rightMargin = 1
                    topMargin = 1
                }

                imageView.setBackgroundColor(context.getColor(R.color.arena_unexplored))
                imageView.isClickable = true
                imageView.setOnClickListener(clickGrid)
                arenaArray[y][x] = imageView
                arenaGridLayout.addView(imageView)

                imageView = ImageView(context)
                imageView.layoutParams = LinearLayout.LayoutParams(robotSize, robotSize)
                imageView.setBackgroundColor(Color.WHITE)
                imageView.alpha = 0.0f
                robotArray[y][x] = imageView
                robotGridLayout.addView(imageView)

                val textView = TextView(context)
                textView.layoutParams = LinearLayout.LayoutParams(gridSize, gridSize).apply {
                    weight = 1.0f
                    gravity = Gravity.CENTER
                    bottomMargin = 1
                    leftMargin = 1
                    rightMargin = 1
                    topMargin = 1
                }

                textView.text = ""
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
                    val startX = lastStartPosition[0]
                    val clashArrayX = intArrayOf(startX, startX + 1, startX + 2)
                    val startY = lastStartPosition[1]
                    val clashArrayY = intArrayOf(startY, startY - 1, startY - 2)

                    if (!(clashArrayX.contains(x) && clashArrayY.contains(y))) {
                        if (x != lastWayPointPosition[0] || y != lastWayPointPosition[1]) {
                            val color: Int = if (bit == 0) R.color.arena_explored else R.color.arena_unexplored
                            arenaArray[y][x].setBackgroundColor(context.getColor(color))
                        }
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
            for (Y in 0 until ROBOT_FOOTPRINT) {
                for (X in 0 until ROBOT_FOOTPRINT) {
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
            if (newX < 0) newX = 0
            if (newX > 12) newX = 12
            if (newY < 2) newY = 2
            if (newY > 19) newY = 19

            val oldX: Int = lastRobotPosition[0]
            val oldY: Int = lastRobotPosition[1]
            val oldR: Int = lastRobotPosition[2]
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

            imageView = robotArray[newY][newX]
            imageView.requestLayout()
            imageView.layoutParams.height = robotSize * ROBOT_FOOTPRINT
            imageView.layoutParams.width = robotSize * ROBOT_FOOTPRINT
            imageView.setImageResource(drawable)
            imageView.alpha = 1.0f
            lastRobotPosition[0] = newX
            lastRobotPosition[1] = newY
            lastRobotPosition[2] = newR

            for (y in 0 until ROBOT_FOOTPRINT) {
                for (x in 0 until ROBOT_FOOTPRINT) {
                    val bit = arenaStateArray[newY - y][newX + x]

                    if (bit == -1) {
                        arenaStateArray[newY - y][newX + x] = 0
                        arenaArray[newY - y][newX + x].setBackgroundColor(context.getColor(R.color.arena_explored))
                    }
                }
            }

            callback(Status.COORDINATES, "$newX, $newY")
        } catch (e: NumberFormatException) {
            Log.e(this::class.simpleName ?: "-", "Something went wrong.", e)
            callback(Status.INFO, "Something went wrong.")
            return
        }
    }

    fun checkClear(dir: Int): Boolean {
        var x = lastRobotPosition[0]
        var y = lastRobotPosition[1]
        val r = lastRobotPosition[2]
        val obstacleMessage = "Obstacle ahead. Cannot move."
        val edgeMessage = "At edge of arena. Cannot move."

        if (x == -1 || y == -1) {
            callback(Status.ROBOT, "Robot position unknown. Cannot move.")
            return false
        }

        if ((r % 180) == 0) {
            if (dir == 1) {
                if ((r == 0 && y + 1 > 19) || (r == 180 && y - ROBOT_FOOTPRINT < 0)) {
                    callback(Status.ROBOT, edgeMessage)
                    return false
                }

                if (r == 0) y += 1 else y -= ROBOT_FOOTPRINT
            } else {
                if ((r == 180 && y + 1 > 19) || (r == 0 && y - ROBOT_FOOTPRINT < 0)) {
                    callback(Status.ROBOT, edgeMessage)
                    return false
                }

                if (r == 180) y += 1 else y -= ROBOT_FOOTPRINT
            }

            if (arenaStateArray[y][x] == 1 || arenaStateArray[y][x + 1] == 1) {
                callback(Status.ROBOT, obstacleMessage)
                return false
            }

            if (ROBOT_FOOTPRINT == 3 && arenaStateArray[y][x + 2] == 1) {
                callback(Status.ROBOT, obstacleMessage)
                return false
            }

            return true
        }

        if (dir == 1) {
            if ((r == 90 && x + ROBOT_FOOTPRINT > 14) || (r == 270 && x - 1 < 0)) {
                callback(Status.ROBOT, edgeMessage)
                return false
            }

            if (r == 90) x += ROBOT_FOOTPRINT else x -= 1
        } else {
            if ((r == 270 && x + ROBOT_FOOTPRINT > 14) || (r == 90 && x - 1 < 0)) {
                callback(Status.ROBOT, edgeMessage)
                return false
            }

            if (r == 270) x += ROBOT_FOOTPRINT else x -= 1
        }

        if (arenaStateArray[y][x] == 1 || arenaStateArray[y - 1][x] == 1) {
            callback(Status.ROBOT, obstacleMessage)
            return false
        }

        if (ROBOT_FOOTPRINT == 3 && arenaStateArray[y - 2][x] == 1) {
            callback(Status.ROBOT, obstacleMessage)
            return false
        }

        return true
    }
}