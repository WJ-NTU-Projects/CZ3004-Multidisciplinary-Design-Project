package ntu.mdp.android.mdptestkotlin

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.isSimple
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.testExplore
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainBinding
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainSimpleBinding
import ntu.mdp.android.mdptestkotlin.settings.SettingsActivity
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothMessageParser
import ntu.mdp.android.mdptestkotlin.arena.ArenaV2
import ntu.mdp.android.mdptestkotlin.utils.ScratchPad
import java.util.*

class MainActivityController(private val context: Context, private val activityUtil: ActivityUtil, tempBinding: ViewBinding) {
    companion object {
        var robotAutonomous = false
        var currentMode: Mode = Mode.NONE
    }

    enum class Mode {
        NONE,
        EXPLORATION,
        FASTEST_PATH
    }

    enum class MessageType {
        INCOMING,
        OUTGOING,
        SYSTEM
    }

    private val bluetoothCallback: (status: BluetoothController.Status, message: String) -> Unit = { status, message ->
        when (status) {
            BluetoothController.Status.CONNECTED, BluetoothController.Status.DISCONNECTED -> {
                connectionChanged(status)
                activityUtil.sendSnack(message)
            }

            BluetoothController.Status.READ -> bluetoothMessageParser.parse(message)
            BluetoothController.Status.WRITE_SUCCESS -> Log.d(this::class.simpleName ?: "-", message)
            else -> activityUtil.sendSnack(message)
        }
    }

    private val arenaV2Callback: (status: ArenaV2.Callback, message: String) -> Unit = { status, message ->
        when (status) {
            ArenaV2.Callback.MESSAGE -> activityUtil.sendSnack(message)
            ArenaV2.Callback.SEND_COMMAND -> sendCommand(message)
            ArenaV2.Callback.UPDATE_COORDINATES -> {
                if (isSimple) binding2?.coordinatesLabel2?.text = message
                else binding?.coordinatesLabel?.text = message
            }

            ArenaV2.Callback.UPDATE_STATUS -> {
                if (isSimple) binding2?.statusLabel2?.text = message
                else binding?.statusLabel?.text = message
            }

            ArenaV2.Callback.LONG_PRESS_CHOICE -> {
                activityUtil.sendYesNoDialog("Plot which?", "START", "GOAL") {
                    arenaV2.selectPoint(it)
                }
            }

            ArenaV2.Callback.RESET_ARENA -> resetArena()
        }
    }

    private val messageParserCallback: (status: BluetoothMessageParser.MessageStatus, message: String) -> Unit = { status, message ->
        when (status) {
            BluetoothMessageParser.MessageStatus.GARBAGE -> displayInChat(MessageType.INCOMING, message)
            BluetoothMessageParser.MessageStatus.ARENA -> arenaV2.updateArena(message)
            BluetoothMessageParser.MessageStatus.IMAGE_POSITION -> updateImage(message)
            BluetoothMessageParser.MessageStatus.ROBOT_POSITION -> updateRobot(message)
            BluetoothMessageParser.MessageStatus.INFO -> activityUtil.sendSnack(message)
            BluetoothMessageParser.MessageStatus.ROBOT_STATUS -> {
                if (isSimple) binding2?.statusLabel2?.text = message
                else binding?.statusLabel?.text = message
            }
        }
    }

    private val arenaV2: ArenaV2 = ArenaV2(context, arenaV2Callback)
    private val bluetoothMessageParser: BluetoothMessageParser = BluetoothMessageParser(messageParserCallback)
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val binding: ActivityMainBinding? = if (!isSimple && tempBinding is ActivityMainBinding) tempBinding else null
    private val binding2: ActivityMainSimpleBinding? = if (isSimple && tempBinding is ActivityMainSimpleBinding) tempBinding else null
    private val statusLabel: MaterialTextView? = if (isSimple) binding2?.statusLabel2 else binding?.statusLabel
    private val timerLabel: MaterialTextView? = if (isSimple) binding2?.timerLabel2 else binding?.timerLabel
    private val messagesTextView: TextView? = if (isSimple) binding2?.messagesTextView2 else binding?.messagesTextView
    private val messagesScrollView: ScrollView? = if (isSimple) binding2?.messagesScrollView2 else binding?.messagesScrollView
    private var buttonListCache: List<View> = listOf()
    private val scratchPad = ScratchPad(this) {
        if (it) onStartClicked(buttonListCache)
        arenaV2.resetPathing()
    }

    private var lastClickTime = 0L
    private lateinit var timer: CountDownTimer

    fun onStart() {
        if (!bluetoothAdapter.isEnabled) (context as Activity).startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1000)
    }

    fun onResume() {
        statusLabel?.text = context.getString(R.string.idle)
        if (!bluetoothAdapter.isEnabled) activityUtil.sendSnack(context.getString(R.string.error_bluetooth_off))
        else startBluetoothListener()
    }

    fun onBackPressed() {
        activityUtil.sendYesNoDialog(context.getString(R.string.exit_the_app)) { positive -> if (positive) activityUtil.finishActivity() }
    }

    private fun startBluetoothListener() {
        if (!BluetoothController.isSocketConnected()) {
            BluetoothController.startServer(bluetoothCallback)
        }

        else {
            BluetoothController.callback = bluetoothCallback
            ArenaV2.isWaitingUpdate = true
            sendCommand(SEND_ARENA_COMMAND)
        }
    }

    fun clickUiButton(view: View) {
        when (view.id) {
            R.id.settingsButton, R.id.settingsButton2 -> activityUtil.startActivity(SettingsActivity::class.java)
            R.id.resetButton -> resetArena()
            R.id.clearObstacleButton, R.id.clearObstacleButton2 -> arenaV2.clearObstacles()

            R.id.messagesClearButton, R.id.messagesClearButton2 -> activityUtil.sendYesNoDialog(context.getString(R.string.clear_message_log)) {
                if (it) messagesTextView?.text = ""
            }

            R.id.f1Button, R.id.f2Button -> {
                val text = if (view.id == R.id.f1Button) sharedPreferences.getString(context.getString(R.string.app_pref_command_f1), context.getString(R.string.f1_default))
                else sharedPreferences.getString(context.getString(R.string.app_pref_command_f2), context.getString(R.string.f2_default))

                when (text) {
                    sharedPreferences.getString(context.getString(R.string.app_pref_forward), context.getString(R.string.forward_default)),
                    sharedPreferences.getString(context.getString(R.string.app_pref_reverse), context.getString(R.string.reverse_default)),
                    sharedPreferences.getString(context.getString(R.string.app_pref_turn_left), context.getString(R.string.turn_left_default)),
                    sharedPreferences.getString(context.getString(R.string.app_pref_turn_right), context.getString(R.string.turn_right_default)) -> {
                        activityUtil.sendSnack(context.getString(R.string.illegal_command))
                        return
                    }
                }

                sendCommand(text!!)
            }
        }
    }

    fun sendCommand(command: String) {
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        displayInChat(MessageType.OUTGOING, command)

        if (!bluetoothAdapter.isEnabled) {
            activityUtil.sendSnack(context.getString(R.string.error_bluetooth_off))
            return
        }

        if (!BluetoothController.isSocketConnected()) {
            activityUtil.sendSnack(context.getString(R.string.error_bluetooth_not_connected))
            return
        }

        if (command.isNotEmpty()) BluetoothController.write(command)
    }

    private fun displayInChat(messageType: MessageType, message: String) {
        val prefixType: String =
            when (messageType) {
                MessageType.INCOMING -> context.getString(R.string.prefix_robot)
                MessageType.OUTGOING -> context.getString(R.string.prefix_tablet)
                MessageType.SYSTEM -> ""
            }

        val calendar: Calendar = Calendar.getInstance()
        val timeStamp = "${(calendar[Calendar.HOUR_OF_DAY]).toString().padStart(2, '0')}:${(calendar[Calendar.MINUTE]).toString().padStart(2, '0')}"
        val prefix: String = context.getString(R.string.chat_prefix, timeStamp, prefixType).trim()
        val displayMessage = "$prefix $message"
        val previousMessages = messagesTextView?.text.toString().trim()
        val newMessage = if (previousMessages.isNotBlank()) "$previousMessages\n$displayMessage" else displayMessage
        messagesTextView?.text = newMessage

        CoroutineScope(Dispatchers.Default).launch {
            delay(250)

            withContext(Dispatchers.Main) {
                messagesScrollView?.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    fun isClickDelayOver(): Boolean {
        if (System.currentTimeMillis() - lastClickTime < 100L) return false
        lastClickTime = System.currentTimeMillis()
        return true
    }

    fun onStartClicked(buttonList: List<View>) {
        robotAutonomous = !robotAutonomous
        buttonListCache = buttonList

        if (testExplore) {
            if (robotAutonomous) {
                arenaV2.resetGoalPoint()

                if (currentMode == Mode.EXPLORATION) {
                    if (testExplore) arenaV2.saveObstacles()
                    CoroutineScope(Dispatchers.Main).launch {
                        scratchPad.exploration()
                    }
                }

                if (currentMode == Mode.FASTEST_PATH) {
                    CoroutineScope(Dispatchers.Main).launch {
                        arenaV2.moveRobot(arenaV2.getStartPosition())
                        scratchPad.fastestPath()
                    }
                }
            } else {
                scratchPad.stop()
            }
        }

        val startButton: MaterialButton? = binding?.startButton

        if (isSimple) {
            if (robotAutonomous) {
                startTimer()
                buttonList.forEach { it.isEnabled = false }
                if (currentMode == Mode.EXPLORATION) binding2?.startExplorationButton2?.isEnabled = true
                else binding2?.startFastestPathButton2?.isEnabled = true
            } else {
                buttonList.forEach { it.isEnabled = true }
                stopTimer()
            }
        } else {
            if (robotAutonomous) {
                startTimer()
                buttonList.forEach { it.isEnabled = false }
                startButton?.text = context.getString(R.string.pause)
                startButton?.icon = context.getDrawable(R.drawable.ic_pause)
            } else {
                buttonList.forEach { it.isEnabled = true }
                startButton?.text = context.getString(R.string.start)
                startButton?.icon = context.getDrawable(R.drawable.ic_start)
                stopTimer()
            }
        }

        val modeLabel: MaterialTextView? = if (isSimple) binding2?.modeLabel2 else binding?.modeLabel

        when (currentMode) {
            Mode.NONE -> modeLabel?.text = context.getString(R.string.none)

            Mode.EXPLORATION -> {
                modeLabel?.text = context.getString(R.string.exploration)
                displayInChat(MessageType.SYSTEM, context.getString(R.string.started_something, "exploration."))
            }

            Mode.FASTEST_PATH -> {
                modeLabel?.text = context.getString(R.string.fastest_path)
                displayInChat(MessageType.SYSTEM, context.getString(R.string.started_something, "fastest path."))
            }
        }
    }

    fun onMapSaveClicked() {
        activityUtil.sendYesNoDialog(context.getString(R.string.save_map_prompt), leftLabel = context.getString(R.string.yes), rightLabel = context.getString(R.string.no)) {
            if (it) {
                val save: String = arenaV2.getMapDescriptor()
                sharedPreferences.edit().putString(context.getString(R.string.app_pref_map_descriptor_1), save).apply()
                activityUtil.sendSnack(context.getString(R.string.map_saved))
            }
        }
    }

    fun onMapLoadClicked() {
        activityUtil.sendYesNoDialog(context.getString(R.string.load_map_prompt), leftLabel = context.getString(R.string.yes), rightLabel = context.getString(R.string.no)) {
            if (it) {
                val load: String = sharedPreferences.getString(context.getString(R.string.app_pref_map_descriptor_1), "") ?: ""
                if (load.isEmpty()) {
                    activityUtil.sendSnack(context.getString(R.string.no_save_data))
                } else {
                    arenaV2.resetArena()
                    arenaV2.updateArena(load)
                }
            }
        }
    }

    private fun startTimer() {
        timer = object: CountDownTimer(Long.MAX_VALUE, 1000) {
            var timerCounter: Int = -1
            override fun onTick(p0: Long) {
                timerCounter++
                val seconds: Int = timerCounter % 60
                val minutes: Int = Math.floorDiv(timerCounter, 60)
                timerLabel?.text = context.getString(R.string.timer_minute_second, minutes.toString().padStart(2, '0'), seconds.toString().padStart(2, '0'))
            }

            override fun onFinish() {}
        }

        timer.start()
    }

    private fun stopTimer() {
        val type: String = if (currentMode == Mode.EXPLORATION) context.getString(R.string.exploration) else context.getString(R.string.fastest_path)
        displayInChat(MessageType.SYSTEM, "$type - ${timerLabel?.text.toString().trim()}")
        timer.cancel()
        currentMode = Mode.NONE
    }

    private fun resetArena() {
        activityUtil.sendYesNoDialog(context.getString(R.string.reset_arena_timer)) {
            if (it) {
                arenaV2.resetArena()
                timerLabel?.text = context.getString(R.string.timer_default)
            }
        }
    }

    private fun connectionChanged(status: BluetoothController.Status) {
        if (status == BluetoothController.Status.CONNECTED) {
            ArenaV2.isWaitingUpdate = true
            sendCommand(SEND_ARENA_COMMAND)
        } else {
            startBluetoothListener()
        }

        arenaV2.updateRobotImage()
    }

    fun updateImage(data: String) {
        val s = data.split(", ")

        try {
            val x = s[0].toInt()
            val y = s[1].toInt()
            val id = s[2].toInt()
            arenaV2.setImage(x, y, id)
        } catch (e: NumberFormatException) {
            activityUtil.sendSnack(context.getString(R.string.something_went_wrong))
            return
        }
    }

    fun updateRobot(data: String) {
        val s = data.split(", ")

        try {
            val x = s[0].toInt()
            val y = s[1].toInt()
            val r = s[2].toInt()
            val robotPosition = arenaV2.getRobotPosition()
            val currentX = robotPosition[0]
            val currentY = robotPosition[1]

            if (x == currentX && y == currentY) {
                CoroutineScope(Dispatchers.Main).launch {
                    arenaV2.turnRobot(r)
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    arenaV2.moveRobot(x, y)
                }
            }
        } catch (e: NumberFormatException) {
            activityUtil.sendSnack(context.getString(R.string.something_went_wrong))
            return
        }
    }

    fun getArena(): ArenaV2 = arenaV2
}