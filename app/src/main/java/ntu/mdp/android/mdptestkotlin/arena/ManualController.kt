package ntu.mdp.android.mdptestkotlin.arena

import android.content.Context
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.App.Companion.PAD_MOVABLE
import ntu.mdp.android.mdptestkotlin.App.Companion.TILT_MOVABLE
import ntu.mdp.android.mdptestkotlin.App.Companion.accelerometer
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainBinding
import kotlin.math.abs
import kotlin.math.roundToInt

class ManualController(private val context: Context, binding: ActivityMainBinding, private val robotController: RobotController, private val callback: (callback: ArenaV2.Callback, message: String) -> Unit) {

    private val buttonList      : List<View> = listOf(binding.padForwardButton, binding.padReverseButton, binding.padLeftButton, binding.padRightButton)
    private var trackMovement   : Boolean = false
    private var currentDirection: ArenaV2.Direction = ArenaV2.Direction.NONE
    private lateinit var movementThread  : MovementThread

    val touchListener = View.OnTouchListener { _, event ->
        //Log.e("EVENT", "$event")
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return@OnTouchListener handleTouchDown(event)
            MotionEvent.ACTION_MOVE -> handleTouchMove(event)
            MotionEvent.ACTION_UP -> handleTouchUp()
        }

        return@OnTouchListener true
    }

    //create listener
    val gyroscopeSensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent?) {
            if (!TILT_MOVABLE) {
                return
            }

            if (event != null) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                when {
                    (z > 9 && y < 2.5) -> {
                        TILT_MOVABLE = false
                        pressPadButton(ArenaV2.Direction.FORWARD)
                        robotController.moveOrTurnRobot(0)
                    }

                    x > 4.5 -> {
                        TILT_MOVABLE = false
                        pressPadButton(ArenaV2.Direction.LEFT)
                        robotController.moveOrTurnRobot(270)
                    }

                    x < -4 -> {
                        TILT_MOVABLE = false
                        pressPadButton(ArenaV2.Direction.RIGHT)
                        robotController.moveOrTurnRobot(90)
                    }

                    (y > 9.5 && z < 3.5) -> {
                        TILT_MOVABLE = false
                        pressPadButton(ArenaV2.Direction.REVERSE)
                        robotController.moveOrTurnRobot(180)
                    }

                    else -> {
                        releasePadButtons()
                        robotController.cancelLast()
                    }
                }

                //Log.e("XYZ values", "${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
            }
        }
    }

    init {
        robotController.registerForBroadcast { _, _ ->
            if (accelerometer) {
                TILT_MOVABLE = true
            } else {
                PAD_MOVABLE = true

                if (!trackMovement && currentDirection != ArenaV2.Direction.NONE) {
                    trackMovement = true
                    movementThread = MovementThread()
                    movementThread.start()
                }
            }
        }
    }

    private fun handleTouchDown(event: MotionEvent): Boolean {
        checkTouchIntersect(event)

        if (!PAD_MOVABLE) return true
        if (!trackMovement) {
            trackMovement = true
            movementThread = MovementThread()
            movementThread.start()
        }

        return true
    }

    private fun handleTouchMove(event: MotionEvent) {
        checkTouchIntersect(event)

        if (!PAD_MOVABLE) return
        if (!trackMovement) {
            trackMovement = true
            movementThread = MovementThread()
            movementThread.start()
        }
    }

    private fun handleTouchUp() {
        robotController.cancelLast()
        callback(ArenaV2.Callback.UPDATE_STATUS, context.getString(R.string.idle))
        updateCurrentDirection(ArenaV2.Direction.NONE)
        if (::movementThread.isInitialized) movementThread.end()
        releasePadButtons()
    }

    private fun checkTouchIntersect(event: MotionEvent) {
        /*
        for ((i, b) in buttonList.withIndex()) {
            Log.e("CTI $i", "${b?.left?.minus(10)}, ${b?.top?.minus(10)}, ${b?.right?.plus(10)}, ${b?.bottom?.plus(10)}")
        }
        */
        val forwardRect = Rect(buttonList[0].left.minus(10), buttonList[0].top.minus(10), buttonList[0].right.plus(10), buttonList[0].bottom.plus(10))
        val reverseRect = Rect(buttonList[1].left.minus(10), buttonList[1].top.minus(10), buttonList[1].right.plus(10), buttonList[1].bottom.plus(10))
        val leftRect = Rect(buttonList[2].left.minus(10), buttonList[2].top.minus(10), buttonList[2].right.plus(10), buttonList[2].bottom.plus(10))
        val rightRect = Rect(buttonList[3].left.minus(10), buttonList[3].top.minus(10), buttonList[3].right.plus(10), buttonList[3].bottom.plus(10))

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
        if (currentDirection != direction) {
            robotController.cancelLast()
            this.currentDirection = direction
        }
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

    fun reset() {
        releasePadButtons()
    }

    private inner class MovementThread: Thread() {
        fun end() {
            trackMovement = false
        }

        override fun run() {
            CoroutineScope(Dispatchers.Default).launch {
                while (trackMovement) {
                    if (!PAD_MOVABLE) {
                        end()
                        return@launch
                    }

                    val facing = when (currentDirection) {
                        ArenaV2.Direction.FORWARD -> 0
                        ArenaV2.Direction.REVERSE -> 180
                        ArenaV2.Direction.LEFT -> 270
                        ArenaV2.Direction.RIGHT -> 90
                        ArenaV2.Direction.NONE -> -1
                    }

                    if (facing == -1) continue
                    PAD_MOVABLE = false
                    val currentFacing: Int = robotController.getRobotFacing()
                    val facingOffset: Int = currentFacing - facing
                    end()

                    if (facing == currentFacing || abs(facing - currentFacing) == 180) {
                        robotController.moveRobot(facing)
                    } else if (facingOffset == 90 || facingOffset == -270) {
                        robotController.turnRobot(Math.floorMod(currentFacing - 90, 360))
                    } else if (facingOffset == -90 || facingOffset == 270) {
                        robotController.turnRobot(Math.floorMod(currentFacing + 90, 360))
                    }

                    return@launch
                    //if (BluetoothController.isSocketConnected()) delay(App.simulationDelay)
                }
            }
        }
    }
}