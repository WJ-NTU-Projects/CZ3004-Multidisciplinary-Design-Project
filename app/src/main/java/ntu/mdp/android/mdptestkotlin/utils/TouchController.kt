package ntu.mdp.android.mdptestkotlin.utils

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.ROBOT_MOVABLE
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.arena.ArenaV2
import ntu.mdp.android.mdptestkotlin.arena.RobotController
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainBinding
import kotlin.math.abs
import kotlin.math.roundToInt

class TouchController(private val context: Context, binding: ActivityMainBinding, private val robotController: RobotController, private val callback: (callback: ArenaV2.Callback, message: String) -> Unit) {

    private val buttonList      : List<View?> = listOf(binding.padForwardButton, binding.padReverseButton, binding.padLeftButton, binding.padRightButton)
    private val forwardRect     : Rect = Rect(75, 10, 165, 90)
    private val reverseRect     : Rect = Rect(75, 150, 165, 230)
    private val leftRect        : Rect = Rect(15, 90, 95, 170)
    private val rightRect       : Rect = Rect(150, 90, 230, 170)
    private var swipeMode       : Boolean = false
    private var swipeOriginX    : Float = 0.0f
    private var swipeOriginY    : Float = 0.0f
    private var trackMovement   : Boolean = false
    private var currentDirection: ArenaV2.Direction = ArenaV2.Direction.NONE
    private lateinit var movementThread  : MovementThread

    private val touchListener = View.OnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return@OnTouchListener handleTouchDown(view, event)
            MotionEvent.ACTION_MOVE -> handleTouchMove(event)
            MotionEvent.ACTION_UP -> handleTouchUp()
        }

        return@OnTouchListener true
    }

    init {
        robotController.registerForBroadcast { _, _ -> ROBOT_MOVABLE = true }
    }

    private fun handleTouchDown(view: View, event: MotionEvent): Boolean {
        if (swipeMode) {
            swipeOriginX = (view.width / 2.0f)
            swipeOriginY = (view.height / 2.0f)

            val x = (event.x - swipeOriginX)
            val y = (event.y - swipeOriginY)
            val threshold = 33

            when {
                (y < -threshold && abs(y) > abs(x)) -> updateCurrentDirection(ArenaV2.Direction.FORWARD)
                (y > threshold && abs(y) > abs(x)) -> updateCurrentDirection(ArenaV2.Direction.REVERSE)
                (x < -threshold && abs(y) < abs(x))  -> updateCurrentDirection(ArenaV2.Direction.LEFT)
                (x > threshold && abs(y) < abs(x)) -> updateCurrentDirection(ArenaV2.Direction.RIGHT)
            }
        } else {
            checkTouchIntersect(event)
        }

        if (!trackMovement) {
            if (::movementThread.isInitialized && movementThread.isAlive) movementThread.end()
            movementThread = MovementThread()
            movementThread.start()
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
                (y < -threshold && abs(y) > abs(x)) -> updateCurrentDirection(ArenaV2.Direction.FORWARD)
                (y > threshold && abs(y) > abs(x)) -> updateCurrentDirection(ArenaV2.Direction.REVERSE)
                (x < -threshold && abs(y) < abs(x))  -> updateCurrentDirection(ArenaV2.Direction.LEFT)
                (x > threshold && abs(y) < abs(x)) -> updateCurrentDirection(ArenaV2.Direction.RIGHT)
            }

            return
        }

        checkTouchIntersect(event)
    }

    private fun handleTouchUp() {
        callback(ArenaV2.Callback.UPDATE_STATUS, context.getString(R.string.idle))
        updateCurrentDirection(ArenaV2.Direction.NONE)
        if (::movementThread.isInitialized) movementThread.end()
        if (!swipeMode) releasePadButtons()
    }

    private fun checkTouchIntersect(event: MotionEvent) {
        when {
            forwardRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(ArenaV2.Direction.FORWARD)
            reverseRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(ArenaV2.Direction.REVERSE)
            leftRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(ArenaV2.Direction.LEFT)
            rightRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(ArenaV2.Direction.RIGHT)
            else -> releasePadButtons()
        }
    }

    @Synchronized
    private fun updateCurrentDirection(direction: ArenaV2.Direction) {
        this.currentDirection = direction
    }

    private fun pressPadButton(direction: ArenaV2.Direction) {
        if (this.currentDirection != direction) {
            updateCurrentDirection(direction)

            val skipIndex: Int = when (direction) {
                ArenaV2.Direction.FORWARD -> 0
                ArenaV2.Direction.REVERSE -> 1
                ArenaV2.Direction.LEFT -> 2
                ArenaV2.Direction.RIGHT -> 3
                ArenaV2.Direction.NONE -> -1
            }

            for ((i, button) in buttonList.withIndex()) {
                if (i == skipIndex) dispatchTouchEvent(button, MotionEvent.ACTION_DOWN)
                else dispatchTouchEvent(button, MotionEvent.ACTION_UP)
            }
        }
    }

    private fun releasePadButtons() {
        updateCurrentDirection(ArenaV2.Direction.NONE)
        buttonList.forEach { dispatchTouchEvent(it, MotionEvent.ACTION_UP) }
    }

    private fun dispatchTouchEvent(view: View?, action: Int) {
        view?.dispatchTouchEvent(MotionEvent.obtain(1, 1, action, 0.0f, 0.0f, 0))
        view?.isEnabled = action != MotionEvent.ACTION_DOWN
    }

    fun toggleSwipeMode(state: Boolean = !swipeMode) {
        swipeMode = state
    }

    fun getTouchListener() = touchListener
    fun getSwipeMode() = swipeMode

    private inner class MovementThread: Thread() {
        fun end() {
            trackMovement = false
        }

        override fun run() {
            CoroutineScope(Dispatchers.Default).launch {
                while (trackMovement) {
                    if (!ROBOT_MOVABLE) {
                        delay(10)
                        continue
                    }

                    val facing = when (currentDirection) {
                        ArenaV2.Direction.FORWARD -> 0
                        ArenaV2.Direction.REVERSE -> 180
                        ArenaV2.Direction.LEFT -> 270
                        ArenaV2.Direction.RIGHT -> 90
                        ArenaV2.Direction.NONE -> -1
                    }

                    if (facing == -1) continue
                    val currentFacing: Int = robotController.getRobotFacing()
                    val facingOffset: Int = currentFacing - facing
                    ROBOT_MOVABLE = false

                    if (facing == currentFacing || abs(facing - currentFacing) == 180) {
                        robotController.moveRobot(facing)
                    } else if (facingOffset == 90 || facingOffset == -270) {
                        robotController.turnRobot(Math.floorMod(currentFacing - 90, 360))
                    } else if (facingOffset == -90 || facingOffset == 270) {
                        robotController.turnRobot(Math.floorMod(currentFacing + 90, 360))
                    }

                    if (BluetoothController.isSocketConnected()) delay(App.simulationDelay)
                }
            }
        }
    }
}