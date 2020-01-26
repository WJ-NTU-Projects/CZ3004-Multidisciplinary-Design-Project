package ntu.mdp.android.mdptestkotlin.main

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.coordinatesLabel
import kotlinx.android.synthetic.main.activity_main.modeLabel
import kotlinx.android.synthetic.main.activity_main.statusLabel
import kotlinx.android.synthetic.main.activity_main.timerLabel
import kotlinx.android.synthetic.main.activity_main_simple.*
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.appTheme
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.isSimple
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainSimpleBinding
import ntu.mdp.android.mdptestkotlin.settings.SettingsActivity
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil
import java.util.*


class MainSimpleActivity : AppCompatActivity() {
    companion object {
        var isUpdating = false
        var isPlotting = false
    }

    // Message control.
    private enum class MessageType {
        INCOMING,
        OUTGOING,
        SYSTEM
    }

    private enum class Mode {
        NONE,
        EXPLORATION,
        FASTEST_PATH
    }

    private var bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var activityUtil: ActivityUtil
    private lateinit var binding: ActivityMainSimpleBinding

    private lateinit var timer: CountDownTimer
    private lateinit var arenaController: ArenaController
    private lateinit var messageParser: MessageParser
    private lateinit var buttonList: List<FloatingActionButton>

    private var currentMode: Mode = Mode.NONE
    private var robotAutonomous = false
    private var timerCounter = 0
    private var lastClickTime: Long = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMainSimpleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN), 1001)

        activityUtil = ActivityUtil(this)
        activityUtil.toggleProgressBar(View.VISIBLE, opaque = true, instant = true)
        autoUpdateArena = true
        isSimple = true
        isPlotting = true
        isUpdating = true
        buttonList = listOf(startExplorationButton2, startFastestPathButton2, settingsButton2, plotObstacleButton2, removeObstacleButton2, clearObstacleButton2, f1Button2, f2Button2)
        arenaController = ArenaController(this) { status, message ->
            when (status) {
                ArenaController.Status.INFO -> activityUtil.sendSnack(message)
                ArenaController.Status.WRITE -> sendCommand(message)
                ArenaController.Status.ROBOT -> displayInChat(MessageType.INCOMING, message)
                ArenaController.Status.COORDINATES -> coordinatesLabel.text = message
                ArenaController.Status.STATUS -> statusLabel.text = message
                ArenaController.Status.RESET -> resetArena()
            }
        }

        messageParser = MessageParser(this) {status, message ->
            when (status) {
                MessageParser.Status.GARBAGE -> displayInChat(MessageType.INCOMING, message)
                MessageParser.Status.ARENA -> arenaController.updateArena(message)
                MessageParser.Status.IMAGE_POSITION -> arenaController.updateImage(message)
                MessageParser.Status.ROBOT_POSITION -> arenaController.updateRobot(message)
                MessageParser.Status.ROBOT_STATUS -> statusLabel.text = message
                MessageParser.Status.INFO -> activityUtil.sendSnack(message)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!bluetoothAdapter.isEnabled) startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1000)
    }

    override fun onResume() {
        super.onResume()
        statusLabel.text = if (BluetoothController.isSocketConnected()) getString(R.string.connected) else getString(R.string.disconnected)
        if (bluetoothAdapter.isEnabled) startBluetoothListener()

        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                activityUtil.toggleProgressBar(View.GONE)
            }
        }
    }

    override fun onBackPressed() {
        activityUtil.sendYesNoDialog(getString(R.string.exit_the_app), { positive -> if (positive) finish() })
    }

    fun clickUiButton(view: View) {
        if (!isClickDelayOver()) return

        when (view.id) {
            R.id.settingsButton2 -> activityUtil.startActivity(SettingsActivity::class.java)
            R.id.messagesClearButton2 -> activityUtil.sendYesNoDialog(getString(R.string.clear_message_log), { positive -> if (positive) messagesTextView.text = "" })
            R.id.clearObstacleButton2 -> arenaController.resetObstacles()

            R.id.startExplorationButton2 -> {
                if (!robotAutonomous) {
                    sendCommand(sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(R.string.exploration_default))!!)
                    currentMode = Mode.EXPLORATION
                }
                onStartClicked()
                displayInChat(MessageType.SYSTEM, getString(R.string.started_something, "exploration."))
            }

            R.id.startFastestPathButton2 -> {
                if (!robotAutonomous) {
                    sendCommand(sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(R.string.fastest_path_default))!!)
                    currentMode = Mode.FASTEST_PATH
                }
                onStartClicked()
                displayInChat(MessageType.SYSTEM, getString(R.string.started_something, "fastest path."))
            }

            R.id.plotObstacleButton2 -> {
                if (arenaController.plotMode != ArenaController.PlotMode.PLOT_OBSTACLE) {
                    buttonList.forEach { it.isEnabled = false }
                    view.isEnabled = true
                    arenaController.plotMode = ArenaController.PlotMode.PLOT_OBSTACLE
                } else {
                    buttonList.forEach { it.isEnabled = true }
                    arenaController.plotMode = ArenaController.PlotMode.NONE
                }
            }

            R.id.removeObstacleButton2 -> {
                if (arenaController.plotMode != ArenaController.PlotMode.REMOVE_OBSTACLE) {
                    buttonList.forEach { it.isEnabled = false }
                    view.isEnabled = true
                    arenaController.plotMode = ArenaController.PlotMode.REMOVE_OBSTACLE
                } else {
                    buttonList.forEach { it.isEnabled = true }
                    arenaController.plotMode = ArenaController.PlotMode.NONE
                }
            }

            R.id.f1Button2, R.id.f2Button2 -> {
                val text = if (view.id == R.id.f1Button2) sharedPreferences.getString(getString(R.string.app_pref_command_f1), getString(R.string.f1_default))
                else sharedPreferences.getString(getString(R.string.app_pref_command_f2), getString(R.string.f2_default))

                when (text) {
                    sharedPreferences.getString(getString(R.string.app_pref_forward), getString(
                        R.string.forward_default
                    )) -> {
                        arenaController.moveRobot(1, BluetoothController.isSocketConnected())
                        return
                    }

                    sharedPreferences.getString(getString(R.string.app_pref_reverse), getString(
                        R.string.reverse_default
                    )) -> {
                        arenaController.moveRobot(-1, BluetoothController.isSocketConnected())
                        return
                    }
                }

                sendCommand(text!!)
            }
        }
    }

    private fun startBluetoothListener() {
        if (!BluetoothController.isSocketConnected()) {
            BluetoothController.startServer(callback)
        }
        else {
            BluetoothController.callback = callback
            isUpdating = true
            sendCommand(SEND_ARENA_COMMAND)
        }
    }

    private fun sendCommand(command: String): Boolean {
        displayInChat(MessageType.OUTGOING, command)

        if (!bluetoothAdapter.isEnabled) {
            activityUtil.sendSnack(getString(R.string.error_bluetooth_off))
            return false
        }

        if (!BluetoothController.isSocketConnected()) {
            activityUtil.sendSnack(getString(R.string.error_bluetooth_not_connected))
            return false
        }

        if (command.isNotEmpty()) {
            BluetoothController.write(command)
            return true
        }

        return false
    }

    private fun onStartClicked() {
        robotAutonomous = !robotAutonomous

        if (robotAutonomous) {
            buttonList.forEach { it.isEnabled = false }

            if (currentMode == Mode.EXPLORATION) {
                startExplorationButton2.isEnabled = true
            }
            else startFastestPathButton2.isEnabled = true

            timer = object: CountDownTimer(Long.MAX_VALUE, 1000) {
                override fun onTick(p0: Long) {
                    timerCounter++
                    val seconds: Int = timerCounter % 60
                    val minutes: Int = Math.floorDiv(timerCounter, 60)
                    timerLabel.text = getString(R.string.timer_minute_second, minutes.toString().padStart(2, '0'), seconds.toString().padStart(2, '0'))
                }

                override fun onFinish() {}
            }

            timer.start()
        } else {
            buttonList.forEach { it.isEnabled = true }
            val type: String = if (currentMode == Mode.EXPLORATION) getString(R.string.exploration) else getString(R.string.fastest_path)
            displayInChat(MessageType.SYSTEM, "$type - ${timerLabel.text.toString().trim()}")
            timer.cancel()
            timerCounter = 0
            currentMode = Mode.NONE
        }

        setMode()
    }

    private fun displayInChat(messageType: MessageType, message: String) {
        val prefixType: String =
            when (messageType) {
                MessageType.INCOMING -> getString(R.string.prefix_robot)
                MessageType.OUTGOING -> getString(R.string.prefix_tablet)
                MessageType.SYSTEM -> ""
            }

        val calendar: Calendar = Calendar.getInstance()
        val timeStamp = "${(calendar[Calendar.HOUR_OF_DAY]).toString().padStart(2, '0')}:${(calendar[Calendar.MINUTE]).toString().padStart(2, '0')}"

        val prefix: String = getString(R.string.chat_prefix, timeStamp, prefixType).trim()
        val displayMessage = "$prefix $message"
        val previousMessages = messagesTextView2.text.toString().trim()
        val newMessage = if (previousMessages.isNotBlank()) "$previousMessages\n$displayMessage" else displayMessage
        messagesTextView2.text = newMessage
        CoroutineScope(Dispatchers.Default).launch {
            delay(250)

            withContext(Dispatchers.Main) {
                messagesScrollView2.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun connectionChanged(status: BluetoothController.Status) {
        if (status == BluetoothController.Status.CONNECTED) {
            statusLabel.text = getString(R.string.connected)
            isUpdating = true
            sendCommand(SEND_ARENA_COMMAND)
        } else {
            statusLabel.text = getString(R.string.disconnected)
            startBluetoothListener()
        }
    }

    private fun resetArena() {
        activityUtil.sendYesNoDialog(getString(R.string.reset_arena_timer), { positive ->
            if (positive) {
                arenaController.resetArena()
                timerLabel.text = getString(R.string.timer_default)
            }
        })
    }

    private fun isClickDelayOver(): Boolean {
        if (System.currentTimeMillis() - lastClickTime < App.BUTTON_CLICK_DELAY_INTERVAL) return false
        lastClickTime = System.currentTimeMillis()
        return true
    }

    private fun setMode() {
        when (currentMode) {
            Mode.NONE -> modeLabel.text = getString(R.string.none)
            Mode.EXPLORATION -> modeLabel.text = getString(R.string.exploration)
            Mode.FASTEST_PATH -> modeLabel.text = getString(R.string.fastest_path)
        }
    }

    private val callback: (status: BluetoothController.Status, message: String) -> Unit = { status, message ->
        when (status) {
            BluetoothController.Status.CONNECTED, BluetoothController.Status.DISCONNECTED -> {
                connectionChanged(status)
                activityUtil.sendSnack(message)
            }

            BluetoothController.Status.READ -> messageParser.parse(message)
            BluetoothController.Status.WRITE_SUCCESS -> Log.d(this::class.simpleName ?: "-", message)
            else -> activityUtil.sendSnack(message)
        }
    }
}
