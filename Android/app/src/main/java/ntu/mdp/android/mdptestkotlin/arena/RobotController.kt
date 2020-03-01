package ntu.mdp.android.mdptestkotlin.arena

import android.content.Context
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.App.Companion.PAD_MOVABLE
import ntu.mdp.android.mdptestkotlin.App.Companion.TILT_MOVABLE
import ntu.mdp.android.mdptestkotlin.App.Companion.accelerometer
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainBinding
import kotlin.math.abs
import kotlin.math.roundToInt

class RobotController(private val context: Context, binding: ActivityMainBinding, private val arenaMapController: ArenaMapController, private val callback: (callback: ArenaMap.Callback, message: String) -> Unit) {

    private val buttonList      : List<View> = listOf(binding.padForwardButton, binding.padReverseButton, binding.padLeftButton, binding.padRightButton)
    private var trackMovement   : Boolean = false
    private var currentDirection: ArenaMap.Direction = ArenaMap.Direction.NONE
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
                        pressPadButton(ArenaMap.Direction.FORWARD)
                        arenaMapController.moveOrTurnRobot(0)
                    }

                    x > 4.5 -> {
                        TILT_MOVABLE = false
                        pressPadButton(ArenaMap.Direction.LEFT)
                        arenaMapController.moveOrTurnRobot(270)
                    }

                    x < -4 -> {
                        TILT_MOVABLE = false
                        pressPadButton(ArenaMap.Direction.RIGHT)
                        arenaMapController.moveOrTurnRobot(90)
                    }

                    (y > 9.5 && z < 3.5) -> {
                        TILT_MOVABLE = false
                        pressPadButton(ArenaMap.Direction.REVERSE)
                        arenaMapController.moveOrTurnRobot(180)
                    }

                    else -> {
                        releasePadButtons()
                        arenaMapController.cancelLast()
                    }
                }

                //Log.e("XYZ values", "${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
            }
        }
    }

    init {
        arenaMapController.registerForBroadcast { _, _ ->
            if (accelerometer) {
                TILT_MOVABLE = true
            } else {
                PAD_MOVABLE = true

                if (!trackMovement && currentDirection != ArenaMap.Direction.NONE) {
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
        arenaMapController.cancelLast()
        callback(ArenaMap.Callback.UPDATE_STATUS, context.getString(R.string.idle))
        updateCurrentDirection(ArenaMap.Direction.NONE)
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
            forwardRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(ArenaMap.Direction.FORWARD)
            reverseRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(ArenaMap.Direction.REVERSE)
            leftRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(ArenaMap.Direction.LEFT)
            rightRect.contains(event.x.roundToInt(), event.y.roundToInt()) -> pressPadButton(ArenaMap.Direction.RIGHT)
            else -> releasePadButtons()
        }
    }

    @Synchronized
    private fun updateCurrentDirection(direction: ArenaMap.Direction) {
        if (currentDirection != direction) {
            arenaMapController.cancelLast()
            this.currentDirection = direction
        }
    }

    private fun pressPadButton(direction: ArenaMap.Direction) {
        if (this.currentDirection != direction) {
            updateCurrentDirection(direction)

            val skipIndex: Int = when (direction) {
                ArenaMap.Direction.FORWARD -> 0
                ArenaMap.Direction.REVERSE -> 1
                ArenaMap.Direction.LEFT -> 2
                ArenaMap.Direction.RIGHT -> 3
                ArenaMap.Direction.NONE -> -1
            }

            for ((i, button) in buttonList.withIndex()) {
                if (i == skipIndex) dispatchTouchEvent(button, MotionEvent.ACTION_DOWN)
                else dispatchTouchEvent(button, MotionEvent.ACTION_UP)
            }
        }
    }

    private fun releasePadButtons() {
        updateCurrentDirection(ArenaMap.Direction.NONE)
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

                    PAD_MOVABLE = false
                    end()

                    when (currentDirection) {
                        ArenaMap.Direction.FORWARD, ArenaMap.Direction.REVERSE -> arenaMapController.moveRobot(currentDirection)
                        ArenaMap.Direction.LEFT, ArenaMap.Direction.RIGHT -> arenaMapController.turnRobot(currentDirection)
                    }

                    return@launch

//                    val facing = when (currentDirection) {
//                        ArenaMap.Direction.FORWARD -> 0
//                        ArenaMap.Direction.REVERSE -> 180
//                        ArenaMap.Direction.LEFT -> 270
//                        ArenaMap.Direction.RIGHT -> 90
//                        ArenaMap.Direction.NONE -> -1
//                    }
//
//                    if (facing == -1) continue
//                    PAD_MOVABLE = false
//                    val currentFacing: Int = arenaMapController.getRobotFacing()
//                    val facingOffset: Int = currentFacing - facing
//                    end()
//
//                    if (facing == currentFacing || abs(facing - currentFacing) == 180) {
//                        arenaMapController.moveRobot(facing)
//                    } else if (facingOffset == 90 || facingOffset == -270) {
//                        arenaMapController.turnRobot(Math.floorMod(currentFacing - 90, 360))
//                    } else if (facingOffset == -90 || facingOffset == 270) {
//                        arenaMapController.turnRobot(Math.floorMod(currentFacing + 90, 360))
//                    }
                    //if (BluetoothController.isSocketConnected()) delay(App.simulationDelay)
                }
            }
        }
    }
}