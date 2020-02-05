package ntu.mdp.android.mdptestkotlin.arena

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.*
import kotlinx.coroutines.future.future
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationDelay
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainBinding
import java.util.concurrent.CompletableFuture
import kotlin.math.abs
import kotlin.math.roundToInt

class RobotController(private val context: Context, binding: ActivityMainBinding, private val callback: (callback: Callback, message: String) -> Unit)
    : ArenaV2(context, callback) {

    enum class Direction {
        NONE,
        FORWARD,
        REVERSE,
        LEFT,
        RIGHT
    }

    private val buttonList      : List<View?> = listOf(binding.padForwardButton, binding.padReverseButton, binding.padLeftButton, binding.padRightButton)
    private val forwardRect     : Rect = Rect(75, 10, 165, 90)
    private val reverseRect     : Rect = Rect(75, 150, 165, 230)
    private val leftRect        : Rect = Rect(15, 90, 95, 170)
    private val rightRect       : Rect = Rect(150, 90, 230, 170)
    private var swipeMode       : Boolean = false
    private var swipeOriginX    : Float = 0.0f
    private var swipeOriginY    : Float = 0.0f
    private var trackMovement   : Boolean = false
    private var currentDirection: Direction = Direction.NONE
    private var lastMoveTime    : Long = 0L

    private val touchListener = View.OnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return@OnTouchListener handleTouchDown(view, event)
            MotionEvent.ACTION_MOVE -> handleTouchMove(event)
            MotionEvent.ACTION_UP -> handleTouchUp()
        }

        return@OnTouchListener true
    }

    private suspend fun canMove(direction: Direction): Boolean = withContext(Dispatchers.Main) {
        val moveCoordinates: IntArray = getNextMovementCoordinates(direction)
        return@withContext isRobotMovable(moveCoordinates[0], moveCoordinates[1])
    }

    private suspend fun isGridExplored(direction: Direction): Boolean = withContext(Dispatchers.Main) {
        val moveCoordinates: IntArray = getNextExplorationCoordinates(direction)
        if (!isValidCoordinates(moveCoordinates)) return@withContext true
        return@withContext isGridExplored(moveCoordinates[0], moveCoordinates[1])
    }

    private suspend fun turnRobot(direction: Direction) = withContext(Dispatchers.Main) {
        when (direction) {
            Direction.RIGHT -> turnRobot(Math.floorMod(getRobotFacing() + 90, 360))
            Direction.LEFT -> turnRobot(Math.floorMod(getRobotFacing() - 90, 360))
            else -> return@withContext
        }
    }

    private suspend fun moveRobot(direction: Direction) = withContext(Dispatchers.Main) {
        val moveCoordinates: IntArray = getNextMovementCoordinates(direction)
        if (!canMove(direction)) return@withContext
        Log.e("TEST", "CAN_MOVE")
        moveRobot(moveCoordinates)
    }

    private suspend fun getNextMovementCoordinates(direction: Direction): IntArray = withContext(Dispatchers.Main) {
        val robotPosition = getRobotPosition()
        var x = robotPosition[0]
        var y = robotPosition[1]

        when (robotPosition[2]) {
            0 -> {
                when (direction) {
                    Direction.FORWARD -> y += 1
                    Direction.LEFT -> x -= 1
                    Direction.RIGHT -> x += 1
                    else -> y -= 1
                }
            }

            90 -> {
                when (direction) {
                    Direction.FORWARD -> x += 1
                    Direction.LEFT -> y += 1
                    Direction.RIGHT -> y -= 1
                    else -> x -= 1
                }
            }

            180 -> {
                when (direction) {
                    Direction.FORWARD -> y -= 1
                    Direction.LEFT -> x += 1
                    Direction.RIGHT -> x -= 1
                    else -> y += 1
                }
            }

            270 -> {
                when (direction) {
                    Direction.FORWARD -> x -= 1
                    Direction.LEFT -> y -= 1
                    Direction.RIGHT -> y += 1
                    else -> x += 1
                }
            }

            else -> return@withContext intArrayOf(-1, -1)
        }

        return@withContext intArrayOf(x, y)
    }

    private suspend fun getNextExplorationCoordinates(direction: Direction): IntArray = withContext(Dispatchers.Main) {
        val robotPosition = getRobotPosition()
        var x = robotPosition[0]
        var y = robotPosition[1]

        when (robotPosition[2]) {
            0 -> {
                when (direction) {
                    Direction.FORWARD -> y += 2
                    Direction.LEFT -> x -= 2
                    Direction.RIGHT -> x += 2
                    else -> y -= 2
                }
            }

            90 -> {
                when (direction) {
                    Direction.FORWARD -> x += 2
                    Direction.LEFT -> y += 2
                    Direction.RIGHT -> y -= 2
                    else -> x -= 2
                }
            }

            180 -> {
                when (direction) {
                    Direction.FORWARD -> y -= 2
                    Direction.LEFT -> x += 2
                    Direction.RIGHT -> x -= 2
                    else -> y += 2
                }
            }

            270 -> {
                when (direction) {
                    Direction.FORWARD -> x -= 2
                    Direction.LEFT -> y -= 2
                    Direction.RIGHT -> y += 2
                    else -> x += 2
                }
            }

            else -> return@withContext intArrayOf(-1, -1)
        }

        return@withContext intArrayOf(x, y)
    }

    fun isGridExploredJava(direction: Direction): CompletableFuture<Boolean> = CoroutineScope(Dispatchers.Main).future { isGridExplored(direction) }
    fun canMoveJava(direction: Direction): CompletableFuture<Boolean> = CoroutineScope(Dispatchers.Main).future { canMove(direction) }
    fun moveRobotJava(direction: Direction): CompletableFuture<*> = CoroutineScope(Dispatchers.Main).future { moveRobot(direction) }
    fun moveRobotJava(array: IntArray): CompletableFuture<*> = CoroutineScope(Dispatchers.Main).future { moveRobot(array) }
    fun turnRobotJava(direction: Int): CompletableFuture<*> = CoroutineScope(Dispatchers.Main).future { turnRobot(direction) }
    fun turnRobotJava(direction: Direction): CompletableFuture<*> = CoroutineScope(Dispatchers.Main).future { turnRobot(direction) }
    fun getTouchListener() = touchListener
    fun getSwipeMode() = swipeMode
    fun getResponded() = hasResponse
    fun setResponse(r: Boolean) {
        hasResponse = r
    }

    fun toggleSwipeMode(state: Boolean = !swipeMode) {
        swipeMode = state
    }

    private fun handleTouchDown(view: View, event: MotionEvent): Boolean {
        if (swipeMode) {
            swipeOriginX = (view.width / 2.0f)
            swipeOriginY = (view.height / 2.0f)
        } else {
            checkTouchIntersect(event)
        }

        if (!trackMovement) {
            MovementThread().start()
            trackMovement = true
        }

        return true
    }

    private fun handleTouchMove(event: MotionEvent) {
        if (swipeMode) {
            val x = (event.x - swipeOriginX)
            val y = (event.y - swipeOriginY)
            val threshold = 33

            when {
                (y < -threshold && abs(y) > abs(x)) -> updateCurrentDirection(Direction.FORWARD)
                (y > threshold && abs(y) > abs(x)) -> updateCurrentDirection(Direction.REVERSE)
                (x < -threshold && abs(y) < abs(x))  -> updateCurrentDirection(Direction.LEFT)
                (x > threshold && abs(y) < abs(x)) -> updateCurrentDirection(Direction.RIGHT)
            }

            return
        }

        checkTouchIntersect(event)
    }

    private fun handleTouchUp() {
        callback(Callback.UPDATE_STATUS, context.getString(R.string.idle))
        updateCurrentDirection(Direction.NONE)
        trackMovement = false
        if (!swipeMode) releasePadButtons()
    }

    private fun checkTouchIntersect(event: MotionEvent) {
        Log.e("EVENT", "${event.x}, ${event.y}")
        when {
            forwardRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(Direction.FORWARD)
            reverseRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(Direction.REVERSE)
            leftRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(Direction.LEFT)
            rightRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(Direction.RIGHT)
            else -> releasePadButtons()
        }
    }

    @Synchronized
    private fun updateCurrentDirection(direction: Direction) {
        this.currentDirection = direction
    }

    private fun pressPadButton(direction: Direction) {
        if (this.currentDirection != direction) {
            updateCurrentDirection(direction)

            val skipIndex: Int = when (direction) {
                Direction.FORWARD -> 0
                Direction.REVERSE -> 1
                Direction.LEFT -> 2
                Direction.RIGHT -> 3
                Direction.NONE -> -1
            }

            for ((i, button) in buttonList.withIndex()) {
                if (i == skipIndex) dispatchTouchEvent(button, MotionEvent.ACTION_DOWN)
                else dispatchTouchEvent(button, MotionEvent.ACTION_UP)
            }
        }
    }

    private fun releasePadButtons() {
        updateCurrentDirection(Direction.NONE)
        buttonList.forEach { dispatchTouchEvent(it, MotionEvent.ACTION_UP) }
    }

    private fun dispatchTouchEvent(view: View?, action: Int) {
        view?.dispatchTouchEvent(MotionEvent.obtain(1, 1, action, 0.0f, 0.0f, 0))
        view?.isEnabled = action != MotionEvent.ACTION_DOWN
    }

    private inner class MovementThread: Thread() {
        override fun run() {
            while (trackMovement) {
                if (!hasResponse) continue
                if (System.currentTimeMillis() - lastMoveTime < simulationDelay) continue
                lastMoveTime = System.currentTimeMillis()

                val currentFacing: Int = getRobotFacing()
                val facing = when (currentDirection) {
                    Direction.FORWARD    -> 0
                    Direction.REVERSE    -> 180
                    Direction.LEFT       -> 270
                    Direction.RIGHT      -> 90
                    Direction.NONE       -> -1
                }

                if (facing == -1) continue
                val facingOffset: Int = currentFacing - facing

                if (facing == currentFacing || abs(facing - currentFacing) == 180) {
                    CoroutineScope(Dispatchers.Main).launch {
                        moveRobot(facing)
                    }
                } else if (facingOffset == 90 || facingOffset == -270) {
                    CoroutineScope(Dispatchers.Main).launch {
                        turnRobot(Math.floorMod(currentFacing - 90, 360))
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        turnRobot(Math.floorMod(currentFacing + 90, 360))
                    }
                }
            }
        }
    }
}