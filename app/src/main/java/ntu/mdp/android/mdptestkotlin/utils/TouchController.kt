package ntu.mdp.android.mdptestkotlin.utils

import android.content.Context
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.App.Companion.MOVEMENT_PROCSES_INTERVAL
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.MainActivityController
import kotlin.math.abs

class TouchController(private val context: Context, private val mainActivityController: MainActivityController, private val callback: (message: String) -> Unit) {
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

    val touchListener = View.OnTouchListener { view, event ->
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isSwipeMode) {
                    viewOriginX = (view.width / 2.0f)
                    viewOriginY = (view.height / 2.0f)
                } else {
                    if (!mainActivityController.isClickDelayOver()) return@OnTouchListener false
                    when (view.id) {
                        R.id.padForwardButton -> movementFlag = MovementFlag.FORWARD
                        R.id.padReverseButton -> movementFlag = MovementFlag.REVERSE
                        R.id.padLeftButton -> movementFlag = MovementFlag.LEFT
                        R.id.padRightButton -> movementFlag = MovementFlag.RIGHT
                    }
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
                }
            }

            MotionEvent.ACTION_UP -> {
                continuousMovement = false
                callback(if (BluetoothController.isSocketConnected()) context.getString(R.string.connected) else context.getString(R.string.disconnected))
                movementFlag = MovementFlag.NONE
                view?.performClick()
            }
        }

        false
    }

    private inner class ContinuousMovementThread: Thread() {
        override fun run() {
            var lastMoveTime = 0L

            while (continuousMovement) {
                if (System.currentTimeMillis() - lastMoveTime >= MOVEMENT_PROCSES_INTERVAL) {
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
}