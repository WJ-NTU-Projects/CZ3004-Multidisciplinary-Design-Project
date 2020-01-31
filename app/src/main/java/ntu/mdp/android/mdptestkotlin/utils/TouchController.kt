package ntu.mdp.android.mdptestkotlin.utils

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.MainActivityController
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainBinding
import kotlin.math.abs
import kotlin.math.roundToInt

class TouchController(private val context: Context, private val mainActivityController: MainActivityController, private val binding: ActivityMainBinding, private val callback: (message: String) -> Unit) {
    companion object {
        var isSwipeMode = false
    }

    private enum class MovementFlag {
        NONE,
        FORWARD,
        REVERSE,
        LEFT,
        RIGHT
    }

    private var continuousMovement = false
    private var movementFlag: MovementFlag = MovementFlag.NONE
    private var viewOriginX: Float = 0.0f
    private var viewOriginY: Float = 0.0f
    private var forwardRect = Rect(80, 55, 150, 125)
    private var reverseRect = Rect(80, 190, 150, 260)
    private var leftRect = Rect(20, 125, 90, 195)
    private var rightRect = Rect(150, 125, 220, 195)
    var lastMoveTime = 0L

    val touchListener = View.OnTouchListener { view, event ->

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isSwipeMode) {
                    viewOriginX = (view.width / 2.0f)
                    viewOriginY = (view.height / 2.0f)
                } else {
                    if (!mainActivityController.isClickDelayOver()) return@OnTouchListener false
                    checkIntersect(event)
                }

                if (!continuousMovement) {
                    ContinuousMovementThread().start()
                    continuousMovement = true
                }

                return@OnTouchListener true
            }

            MotionEvent.ACTION_MOVE -> {
                if (continuousMovement && isSwipeMode) {
                    val x = (event.x - viewOriginX)
                    val y = (event.y - viewOriginY)
                    val threshold = 33

                    when {
                        (y < -threshold && abs(y) > abs(x)) -> movementFlag = MovementFlag.FORWARD
                        (y > threshold && abs(y) > abs(x)) -> movementFlag = MovementFlag.REVERSE
                        (x < -threshold && abs(y) < abs(x))  -> movementFlag = MovementFlag.LEFT
                        (x > threshold && abs(y) < abs(x)) -> movementFlag = MovementFlag.RIGHT
                    }
                } else {
                    checkIntersect(event)
                }

                return@OnTouchListener true
            }

            MotionEvent.ACTION_UP -> {
                continuousMovement = false
                callback(context.getString(R.string.idle))
                movementFlag = MovementFlag.NONE
                if (!isSwipeMode) releasePadButtons()
                return@OnTouchListener true
            }
        }

        false
    }

    private fun checkIntersect(event: MotionEvent) {
        when {
            forwardRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(MovementFlag.FORWARD)
            reverseRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(MovementFlag.REVERSE)
            leftRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(MovementFlag.LEFT)
            rightRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(MovementFlag.RIGHT)
            else -> releasePadButtons()
        }
    }

    private fun pressPadButton(movementFlag: MovementFlag) {
        if (this.movementFlag != movementFlag) {
            this.movementFlag = movementFlag
            val buttonList: List<View> = listOf(binding.padForwardButton, binding.padReverseButton, binding.padLeftButton, binding.padRightButton)
            val skipIndex: Int = when (movementFlag) {
                MovementFlag.FORWARD -> 0
                MovementFlag.REVERSE -> 1
                MovementFlag.LEFT -> 2
                MovementFlag.RIGHT -> 3
                MovementFlag.NONE -> -1
            }

            for ((i, button) in buttonList.withIndex()) {
                if (i == skipIndex) dispatchTouchEvent(button, MotionEvent.ACTION_DOWN)
                else dispatchTouchEvent(button, MotionEvent.ACTION_UP)
            }
        }
    }

    private fun releasePadButtons() {
        movementFlag = MovementFlag.NONE
        val buttonList: List<View> = listOf(binding.padForwardButton, binding.padReverseButton, binding.padLeftButton, binding.padRightButton)
        buttonList.forEach { dispatchTouchEvent(it, MotionEvent.ACTION_UP) }
    }

    private fun dispatchTouchEvent(view: View, action: Int) {
        view.dispatchTouchEvent(MotionEvent.obtain(1, 1, action, 0.0f, 0.0f, 0))
        view.isEnabled = action != MotionEvent.ACTION_DOWN
    }

    private inner class ContinuousMovementThread: Thread() {
        override fun run() {
            while (continuousMovement) {
                if (System.currentTimeMillis() - lastMoveTime >= 500L) {
                    lastMoveTime = System.currentTimeMillis()

                    CoroutineScope(Dispatchers.Main).launch {
                        val robotPosition: IntArray = mainActivityController.getArena().getRobotPosition()
                        val x: Int = robotPosition[0]
                        val y: Int = robotPosition[1]

                        when (movementFlag) {
                            MovementFlag.FORWARD -> mainActivityController.getArena().moveRobot(x, y + 1)
                            MovementFlag.REVERSE -> mainActivityController.getArena().moveRobot(x, y - 1)
                            MovementFlag.LEFT -> mainActivityController.getArena().moveRobot(x - 1, y)
                            MovementFlag.RIGHT -> mainActivityController.getArena().moveRobot(x + 1, y)
                        }
                    }
                }
            }
        }
    }

    /*
    private inner class ContinuousMovementThread: Thread() {
        override fun run() {

            while (continuousMovement) {
                if (System.currentTimeMillis() - lastMoveTime >= 500L) {
                    lastMoveTime = System.currentTimeMillis()

                    CoroutineScope(Dispatchers.Main).launch {
                        if (movementFlag == MovementFlag.NONE) {
                            return@launch
                        }

                        val requestedFacing: Int = when (movementFlag) {
                            MovementFlag.REVERSE -> 180
                            MovementFlag.LEFT -> 270
                            MovementFlag.RIGHT -> 90
                            else -> 0
                        }
                        val robotFacing = mainActivityController.arenaController.getRobotFacing()
                        val facingOffset: Int = robotFacing - requestedFacing

                        if (requestedFacing == robotFacing) {
                            mainActivityController.arenaController.moveRobot(1, BluetoothController.isSocketConnected())
                        } else if (abs(requestedFacing - robotFacing) == 180) {
                            mainActivityController.arenaController.moveRobot(-1, BluetoothController.isSocketConnected())
                        } else if (facingOffset == 90 || facingOffset == -270) {
                            mainActivityController.arenaController.turnRobot(-1, BluetoothController.isSocketConnected())
                        } else {
                            mainActivityController.arenaController.turnRobot(1, BluetoothController.isSocketConnected())
                        }
                    }
                }
            }
        }
    }
     */
}