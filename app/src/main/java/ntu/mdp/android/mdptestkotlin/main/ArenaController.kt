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
import ntu.mdp.android.mdptestkotlin.R
import java.lang.NumberFormatException

class ArenaController(private val context: Context, private val callback: (status: Status, message: String) -> Unit) {
    enum class Status {
        WRITE,
        INFO
    }

    enum class Op {
        NONE,
        SET_STARTPOINT,
        SET_WAYPOINT
    }

    var currentOp: Op = Op.NONE

    private var lastStartGrid: ImageView? = null
    private var lastStartGridColor: Int = Color.RED
    private var lastWayGrid: ImageView? = null
    private var lastWayGridColor: Int = Color.RED
    private var lastStartGridPosition: IntArray = intArrayOf(-1, -1)
    private var lastWayGridPosition: IntArray = intArrayOf(-1, -1)
    private var lastRobotPosition: IntArray = intArrayOf(-1, -1, 0)
    // private var imageHashMap: HashMap<Int, IntArray> = hashMapOf()

    private val gridLayout: GridLayout = ((context as Activity).findViewById(R.id.main_grid) as GridLayout)
    private val robotGridLayout: GridLayout = ((context as Activity).findViewById(R.id.main_grid_robot) as GridLayout)
    private val imageGridLayout: GridLayout = ((context as Activity).findViewById(R.id.main_grid_image) as GridLayout)
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    private val dpWidth: Int = displayMetrics.widthPixels
    private val gridSize: Int = ((dpWidth - 60) / 15)
    private val robotSize: Int = (dpWidth / 15)

    private val clickGrid = View.OnClickListener { view ->
        if (currentOp == Op.NONE && !MainActivity.autoUpdate) {
            MainActivity.isUpdating = true
            callback(Status.WRITE, "sendArena")
            return@OnClickListener
        }

        if (currentOp == Op.SET_STARTPOINT || currentOp == Op.SET_WAYPOINT) {
            val indices: Pair<Int, Int> = getIndices(view as ImageView)
            val x = indices.first
            val y = indices.second

            if (x == -1 || y == -1) {
                callback(Status.INFO, "Invalid grid indices.")
                return@OnClickListener
            }

            if (arenaArray[y][x] == 1) {
                callback(Status.INFO, "Selected point clashes with an obstacle.")
                return@OnClickListener
            }

            if (currentOp == Op.SET_STARTPOINT) {
                lastStartGrid?.setBackgroundColor(lastStartGridColor)
                lastStartGrid = view
                lastStartGridColor = (view.background as ColorDrawable).color
                view.setBackgroundColor(Color.MAGENTA)
                updateRobot("$x, $y, ${lastRobotPosition[2]}")
                lastStartGridPosition[0] = x
                lastStartGridPosition[1] = y
                callback(Status.WRITE, "#startpoint::[$x, $y]")
            }

            if (currentOp == Op.SET_WAYPOINT) {
                lastWayGrid?.setBackgroundColor(lastWayGridColor)
                lastWayGrid = view
                lastWayGridColor = (view.background as ColorDrawable).color
                view.setBackgroundColor(Color.GREEN)
                lastWayGridPosition[0] = x
                lastWayGridPosition[1] = y
                callback(Status.WRITE, "#waypoint::[$x, $y]")
            }

            currentOp = Op.NONE
        }
    }

    private var gridArray: Array<Array<ImageView>> = Array(20) {
        Array(15) {
            ImageView(context)
        }
    }

    private var robotArray: Array<Array<ImageView>> = Array(20) {
        Array(15) {
            ImageView(context)
        }
    }

    private var imageArray: Array<Array<TextView>> = Array(20) {
        Array(15) {
            TextView(context)
        }
    }

    private var arenaArray: Array<Array<Int>> = Array(20) {
        Array(15) {
            0
        }
    }

    init {
        for (y in 19 downTo 0) {
            for (x in 0 until 15) {
                var imageView = ImageView(context)
                imageView.layoutParams = LinearLayout.LayoutParams(gridSize, gridSize)
                val param = imageView.layoutParams as LinearLayout.LayoutParams
                param.setMargins(2, 2, 2, 2)
                imageView.layoutParams = param
                imageView.setBackgroundColor(Color.CYAN)
                imageView.isClickable = true
                imageView.setOnClickListener(clickGrid)
                gridArray[y][x] = imageView
                gridLayout.addView(imageView)

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
                }

                val param2 = textView.layoutParams as LinearLayout.LayoutParams
                param2.setMargins(2, 2, 2, 2)
                textView.layoutParams = param2
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
            for (x in 0 until 15) {
                arenaArray[y][x] = 0
                gridArray[y][x].setBackgroundColor(Color.CYAN)

                val textView = imageArray[y][x]
                textView.text = ""
                textView.alpha = 0.0f

                val imageView = robotArray[y][x]
                imageView.setImageResource(android.R.color.transparent)
                imageView.alpha = 0.0f
            }
        }
    }

    private fun rotate(imageView: ImageView, degree: Float) {
        val rotateAnim = RotateAnimation(
            0.0f, degree,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f
        )
        rotateAnim.duration = 0
        rotateAnim.fillAfter = true
        imageView.startAnimation(rotateAnim)
        lastRobotPosition[2] = degree.toInt()
    }

    fun resetGridColors() {
        lastStartGrid?.setBackgroundColor(lastStartGridColor)
        lastWayGrid?.setBackgroundColor(lastWayGridColor)
        lastStartGrid = null
        lastWayGrid = null
        callback(Status.WRITE, "#startpoint::[-1, -1]")
        callback(Status.WRITE, "#waypoint::[-1, -1]")
    }

    private fun getIndices(imageView: ImageView): Pair<Int, Int> {
        for (y in 0 until 20) {
            val x: Int = gridArray[y].indexOf(imageView)
            if (x != -1) return Pair(x, y)
        }

        return Pair(-1, -1)
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
                    gridArray[y][x].setBackgroundColor(Color.BLACK)
                } else {
                    val x1: Int = lastStartGridPosition[0]
                    val x2: Int = lastWayGridPosition[0]
                    val y1: Int = lastStartGridPosition[1]
                    val y2: Int = lastWayGridPosition[1]
                    if (!((x == x1 && y == y1) || (x == x2 && y == y2))) gridArray[y][x].setBackgroundColor(Color.CYAN)
                }

                arenaArray[y][x] = bit
                counter++
            }
        }
    }

    fun updateRobot(data: String) {
        val s = data.split(", ")

        if (s.size != 3) {
            callback(Status.INFO, "Something went wrong.")
            return
        }

        try {
            val newX = s[0].toInt()
            var newY = s[1].toInt()
            if (newX < 0 || newY < 0) return

            //newY = 19 - newY
            val newR = s[2].toInt()
            val oldX: Int = lastRobotPosition[0]
            val oldY: Int = lastRobotPosition[1]
            val oldR: Int = lastRobotPosition[2]
            var imageView: ImageView

            if (oldX >= 0 && oldY >= 0) {
                imageView = robotArray[oldY][oldX]

                if (newX == oldX && newY == oldY && newR != oldR) {
                    rotate(imageView, newR.toFloat())
                    return
                }

                imageView.requestLayout()
                imageView.layoutParams.height = robotSize
                imageView.layoutParams.width = robotSize
                imageView.alpha = 0.0f
            }

            imageView = robotArray[newY][newX]
            imageView.requestLayout()
            imageView.layoutParams.height = robotSize * 2
            imageView.layoutParams.width = robotSize * 2
            imageView.setImageResource(R.drawable.ic_robot_arrow)
            imageView.alpha = 1.0f
            rotate(imageView, newR.toFloat())
            lastRobotPosition[0] = newX
            lastRobotPosition[1] = newY
            lastRobotPosition[2] = newR
        } catch (e: NumberFormatException) {
            Log.e(this::class.simpleName ?: "-", "Something went wrong.", e)
            callback(Status.INFO, "Something went wrong.")
            return
        }
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
            val bit: Int = arenaArray[y][x]
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

    fun checkClear(dir: Int = 1): Boolean {
        var x = lastRobotPosition[0]
        var y = lastRobotPosition[1]
        val r = lastRobotPosition[2]
        val obstacleMessage = "Obstacle ahead. Cannot move."
        val edgeMessage = "At edge of arena. Cannot move."

        if (x == -1 || y == -1) {
            callback(Status.INFO, "Robot position unknown. Cannot move.")
            return false
        }

        if ((r % 180) == 0) {
            if (dir == 1) {
                if ((r == 0 && y + 1 > 19) || (r == 180 && y - 2 < 0)) {
                    callback(Status.INFO, edgeMessage)
                    return false
                }

                if (r == 0) y += 1 else y -= 2
            } else {
                if ((r == 180 && y + 1 > 19) || (r == 0 && y - 2 < 0)) {
                    callback(Status.INFO, edgeMessage)
                    return false
                }

                if (r == 180) y += 1 else y -= 2
            }

            if (arenaArray[y][x] == 1 || arenaArray[y][x + 1] == 1) {
                callback(Status.INFO, obstacleMessage)
                return false
            }

            return true
        }

        if (dir == 1) {
            if ((r == 90 && x + 2 > 14) || (r == 270 && x - 1 < 0)) {
                callback(Status.INFO, edgeMessage)
                return false
            }

            if (r == 90) x += 2 else x -= 1
        } else {
            if ((r == 270 && x + 2 > 14) || (r == 90 && x - 1 < 0)) {
                callback(Status.INFO, edgeMessage)
                return false
            }

            if (r == 270) x += 2 else x -= 1
        }

        if (arenaArray[y][x] == 1 || arenaArray[y - 1][x] == 1) {
            callback(Status.INFO, obstacleMessage)
            return false
        }

        return true
    }
}