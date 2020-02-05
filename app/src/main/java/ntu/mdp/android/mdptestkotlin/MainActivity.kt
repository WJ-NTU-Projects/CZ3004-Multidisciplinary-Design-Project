package ntu.mdp.android.mdptestkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.App.Companion.ANIMATOR_DURATION
import ntu.mdp.android.mdptestkotlin.App.Companion.CLICK_DELAY
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.appTheme
import ntu.mdp.android.mdptestkotlin.App.Companion.darkMode
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationMode
import ntu.mdp.android.mdptestkotlin.arena.ArenaV2
import ntu.mdp.android.mdptestkotlin.arena.RobotController
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothMessageParser
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainBinding
import ntu.mdp.android.mdptestkotlin.settings.SettingsBluetoothActivity
import ntu.mdp.android.mdptestkotlin.settings.SettingsCommunicationActivity
import ntu.mdp.android.mdptestkotlin.settings.SettingsSimulationActivity
import ntu.mdp.android.mdptestkotlin.simulation.Callback
import ntu.mdp.android.mdptestkotlin.simulation.Exploration
import ntu.mdp.android.mdptestkotlin.simulation.FastestPath
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
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

    private val onEnter = View.OnKeyListener { view, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
            val message: String = (view as EditText).text.toString().trim()

            if (message.isNotBlank()) {
                sendCommand(message)
                messagesOutputEditText.setText("")
            }

            return@OnKeyListener true
        }

        false
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

    private val robotControllerCallback: (status: ArenaV2.Callback, message: String) -> Unit = { status, message ->
        when (status) {
            ArenaV2.Callback.MESSAGE -> activityUtil.sendSnack(message)
            ArenaV2.Callback.SEND_COMMAND -> sendCommand(message)
            ArenaV2.Callback.UPDATE_COORDINATES -> coordinatesCardLabel.text = message
            ArenaV2.Callback.UPDATE_STATUS -> statusCardLabel.text = message

            ArenaV2.Callback.LONG_PRESS_CHOICE -> {
                activityUtil.sendYesNoDialog("Plot which?", "START", "GOAL") {
                    robotController.selectPoint(it)
                }
            }

            ArenaV2.Callback.PLOT_FASTEST_PATH -> {
                activityUtil.sendYesNoDialog("Plot fastest path?") {
                    if (it) robotController.plotFastestPath()
                }
            }

            ArenaV2.Callback.RESET_ARENA -> resetArena(false)
        }
    }

    private val messageParserCallback: (status: BluetoothMessageParser.MessageStatus, message: String) -> Unit = { status, message ->
        when (status) {
            BluetoothMessageParser.MessageStatus.GARBAGE -> displayInChat(MessageType.INCOMING, message)
            BluetoothMessageParser.MessageStatus.ARENA -> robotController.updateArena(message)
            BluetoothMessageParser.MessageStatus.IMAGE_POSITION -> updateImage(message)
            BluetoothMessageParser.MessageStatus.ROBOT_POSITION -> updateRobot(message)
            BluetoothMessageParser.MessageStatus.INFO -> activityUtil.sendSnack(message)
            BluetoothMessageParser.MessageStatus.ROBOT_STATUS -> statusCardLabel.text = message
        }
    }

    private val explorationCallback: (callback: Callback) -> Unit = { callback ->
        CoroutineScope(Dispatchers.Main).launch {
            when (callback) {
                Callback.WALL_HUGGING -> displayInChat(MessageType.INCOMING, getString(R.string.hugging_wall))
                Callback.SEARCHING -> displayInChat(MessageType.INCOMING, getString(R.string.searching_unexplored))
                Callback.GOING_HOME -> displayInChat(MessageType.INCOMING, getString(R.string.going_home))
                Callback.COMPLETE -> onStartClicked(Mode.NONE)
            }
        }
    }

    private val fastestPathCallback: () -> Unit = {
        CoroutineScope(Dispatchers.Main).launch {
            onStartClicked(Mode.NONE)
        }
    }

    private lateinit var binding                : ActivityMainBinding
    private lateinit var activityUtil           : ActivityUtil
    private lateinit var bluetoothAdapter       : BluetoothAdapter
    private lateinit var robotController        : RobotController
    private lateinit var bluetoothMessageParser : BluetoothMessageParser
    private lateinit var buttonList             : List<View>
    private lateinit var timer                  : CountDownTimer
    private lateinit var exploration            : Exploration
    private lateinit var fastestPath            : FastestPath
    private var lastClickTime                   : Long = 0L
    private var isTablet                        : Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN), 1001)
        activityUtil = ActivityUtil(this)
        activityUtil.toggleProgressBar(View.VISIBLE, opaque = true, instant = true)

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            activityUtil.sendDialog(getString(R.string.error_bluetooth_not_supported), finish = true)
            return
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        buttonList = listOf(exploreButton, fastestPathButton, plotButton, saveMapButton, loadMapButton, clearArenaButton, f1Button, f2Button, messageCardClearButton, messagesSendButton, padForwardButton, padLeftButton, padRightButton, padReverseButton)
        robotController = RobotController(this, binding, robotControllerCallback)
        bluetoothMessageParser = BluetoothMessageParser(messageParserCallback)
        isTablet = resources.getBoolean(R.bool.isTablet)

        padForwardButton.isClickable = false
        padReverseButton.isClickable = false
        padLeftButton.isClickable = false
        padRightButton.isClickable = false
        controllerPad.setOnTouchListener(robotController.getTouchListener())
        messagesOutputEditText.setOnKeyListener(onEnter)
    }

    override fun onStart() {
        super.onStart()
        if (BluetoothAdapter.getDefaultAdapter() == null) return
        if (!bluetoothAdapter.isEnabled) startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1000)
    }

    override fun onResume() {
        super.onResume()
        if (BluetoothAdapter.getDefaultAdapter() == null) return
        if (!isTablet) robotController.toggleSwipeMode(true)
        f1Button.text = sharedPreferences.getString(getString(R.string.app_pref_label_f1), getString(R.string.f1_default))
        f2Button.text = sharedPreferences.getString(getString(R.string.app_pref_label_f2), getString(R.string.f2_default))
        statusCardLabel.text = getString(R.string.idle)
        if (!bluetoothAdapter.isEnabled) activityUtil.sendSnack(getString(R.string.error_bluetooth_off))
        else startBluetoothListener()

        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                activityUtil.toggleProgressBar(View.GONE)
            }
        }
    }

    override fun onBackPressed() {
        activityUtil.sendYesNoDialog(getString(R.string.exit_the_app)) { positive -> if (positive) activityUtil.finishActivity() }
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

    @Suppress("unused")
    fun clickUiButton(view: View) {
        if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) return
        lastClickTime = System.currentTimeMillis()

        when (view.id) {
            R.id.bluetoothButton -> activityUtil.startActivity(SettingsBluetoothActivity::class.java)
            R.id.communicationButton -> activityUtil.startActivity(SettingsCommunicationActivity::class.java)
            R.id.simulationButton -> activityUtil.startActivity(SettingsSimulationActivity::class.java)
            R.id.saveMapButton -> onMapSaveClicked()
            R.id.loadMapButton -> onMapLoadClicked()
            R.id.clearArenaButton -> resetArena(true)

            R.id.darkModeButton -> {
                darkMode = !darkMode
                sharedPreferences.edit().putBoolean(getString(R.string.app_pref_dark_mode), darkMode).apply()
                appTheme = if (darkMode) R.style.AppTheme_Dark
                else R.style.AppTheme
                //recreate()
                activityUtil.startActivity(MainActivity::class.java, fade = true, startNew = true)
            }

            R.id.messageCardClearButton -> {
                activityUtil.sendYesNoDialog(getString(R.string.clear_message_log)) {
                    if (it) messagesTextView?.text = ""
                }
            }

            R.id.exploreButton -> {
                val mode = if (currentMode == Mode.NONE) Mode.EXPLORATION else Mode.NONE
                onStartClicked(mode)
            }

            R.id.fastestPathButton -> {
                if (!robotController.isWaypointSet()) {
                    activityUtil.sendSnack("Please set a waypoint first.")
                    return
                }

                val mode = if (currentMode == Mode.NONE) Mode.FASTEST_PATH else Mode.NONE
                onStartClicked(mode)
            }

            R.id.plotButton -> {
                if (robotController.getCurrentFunction() == ArenaV2.PlotFunction.NONE) {
                    robotController.setPlotFunction(ArenaV2.PlotFunction.PLOT_OBSTACLE)
                    buttonList.forEach { it.isEnabled = false }
                    plotButton.isEnabled = true
                    plotButton.setImageDrawable(getDrawable(R.drawable.ic_done))
                } else {
                    robotController.setPlotFunction(ArenaV2.PlotFunction.NONE)
                    robotController.resetActions()
                    buttonList.forEach { it.isEnabled = true }
                    plotButton.setImageDrawable(getDrawable(R.drawable.ic_plot_obstacles))
                }
            }

            R.id.controlModeButton -> {
                robotController.toggleSwipeMode()
                val typedValue = TypedValue()
                theme.resolveAttribute(if (robotController.getSwipeMode()) R.attr.colorAccentTheme else R.attr.colorAccentLighterTheme, typedValue, true)
                @ColorInt val color = typedValue.data
                controlModeButton.setTextColor(color)

                val animationId: Int = if (robotController.getSwipeMode()) R.anim.view_close else R.anim.view_open
                val animation: Animation =  AnimationUtils.loadAnimation(applicationContext, animationId)
                animation.duration = ANIMATOR_DURATION
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(p0: Animation?) {}

                    override fun onAnimationStart(p0: Animation?) {
                        if (!robotController.getSwipeMode()) {
                            padForwardButton.visibility = View.VISIBLE
                            padReverseButton.visibility = View.VISIBLE
                            padLeftButton.visibility = View.VISIBLE
                            padRightButton.visibility = View.VISIBLE
                        }
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        if (robotController.getSwipeMode()) {
                            padForwardButton.visibility = View.INVISIBLE
                            padReverseButton.visibility = View.INVISIBLE
                            padLeftButton.visibility = View.INVISIBLE
                            padRightButton.visibility = View.INVISIBLE
                        }
                    }
                })

                padForwardButton.startAnimation(animation)
                padReverseButton.startAnimation(animation)
                padLeftButton.startAnimation(animation)
                padRightButton.startAnimation(animation)
            }

            R.id.f1Button, R.id.f2Button -> {
                val text = if (view.id == R.id.f1Button) sharedPreferences.getString(getString(R.string.app_pref_command_f1), getString(R.string.f1_default))
                else sharedPreferences.getString(getString(R.string.app_pref_command_f2), getString(R.string.f2_default))

                when (text) {
                    sharedPreferences.getString(getString(R.string.app_pref_forward), getString(R.string.forward_default)),
                    sharedPreferences.getString(getString(R.string.app_pref_reverse), getString(R.string.reverse_default)),
                    sharedPreferences.getString(getString(R.string.app_pref_turn_left), getString(R.string.turn_left_default)),
                    sharedPreferences.getString(getString(R.string.app_pref_turn_right), getString(R.string.turn_right_default)) -> {
                        activityUtil.sendSnack(getString(R.string.illegal_command))
                        return
                    }
                }

                sendCommand(text!!)
            }

            R.id.messagesSendButton -> {
                val message: String = messagesOutputEditText.text.toString().trim()

                if (message.isNotBlank()) {
                    sendCommand(message)
                    messagesOutputEditText.setText("")
                }
            }
        }
    }

    private fun sendCommand(command: String) {
        displayInChat(MessageType.OUTGOING, command)

        if (!bluetoothAdapter.isEnabled) {
            activityUtil.sendSnack(getString(R.string.error_bluetooth_off))
            return
        }

        if (!BluetoothController.isSocketConnected()) {
            activityUtil.sendSnack(getString(R.string.error_bluetooth_not_connected))
            return
        }

        if (command.isNotEmpty()) BluetoothController.write(command)
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

    private fun onStartClicked(mode: Mode) {
        if (simulationMode) {
            when (mode) {
                Mode.EXPLORATION -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        robotController.saveObstacles()
                        robotController.resetGoalPoint()
                        if (::exploration.isInitialized && exploration.isAlive) exploration.end()
                        exploration = Exploration(robotController, explorationCallback)
                        exploration.start()
                    }
                }

                Mode.FASTEST_PATH -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        robotController.moveRobotToStart()
                        robotController.resetWaypoint()
                        robotController.resetGoalPoint()
                        if (::fastestPath.isInitialized && fastestPath.isAlive) fastestPath.end()
                        fastestPath = FastestPath(robotController, fastestPathCallback)
                        fastestPath.start()
                    }
                }

                else -> {
                    if (::exploration.isInitialized && exploration.isAlive) exploration.end()
                    if (::fastestPath.isInitialized && fastestPath.isAlive) fastestPath.end()
                    statusCardLabel.text = getString(R.string.idle)
                }
            }
        }

        when (mode) {
            Mode.EXPLORATION -> {
                sendCommand(sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(R.string.exploration_default))!!)
                startTimer()
                buttonList.forEach { it.isEnabled = false }
                exploreButton.isEnabled = true
                exploreButton.setImageDrawable(getDrawable(R.drawable.ic_pause))
                modeCardLabel.text = getString(R.string.exploration)
                displayInChat(MessageType.SYSTEM, getString(R.string.started_something, "exploration."))
            }

            Mode.FASTEST_PATH -> {
                sendCommand(sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(R.string.fastest_path_default))!!)
                startTimer()
                buttonList.forEach { it.isEnabled = false }
                fastestPathButton.isEnabled = true
                fastestPathButton.setImageDrawable(getDrawable(R.drawable.ic_pause))
                modeCardLabel.text = getString(R.string.fastest_path)
                displayInChat(MessageType.SYSTEM, getString(R.string.started_something, "fastest path."))
            }

            Mode.NONE -> {
                sendCommand(sharedPreferences.getString(getString(R.string.app_pref_pause), getString(R.string.pause_default))!!)
                stopTimer()
                buttonList.forEach { it.isEnabled = true }
                exploreButton.setImageDrawable(getDrawable(R.drawable.ic_explore))
                fastestPathButton.setImageDrawable(getDrawable(R.drawable.ic_fastest))
                modeCardLabel.text = getString(R.string.none)
            }
        }

        currentMode = mode
    }

    private fun onMapSaveClicked() {
        activityUtil.sendYesNoDialog(getString(R.string.save_map_prompt), leftLabel = getString(R.string.yes), rightLabel = getString(R.string.no)) {
            if (it) {
                val save: String = robotController.getMapDescriptor()
                sharedPreferences.edit().putString(getString(R.string.app_pref_map_descriptor_1), save).apply()
                activityUtil.sendSnack(getString(R.string.map_saved))
            }
        }
    }

    private fun onMapLoadClicked() {
        activityUtil.sendYesNoDialog(getString(R.string.load_map_prompt), leftLabel = getString(R.string.yes), rightLabel = getString(R.string.no)) {
            if (it) {
                val load: String = sharedPreferences.getString(getString(R.string.app_pref_map_descriptor_1), "") ?: ""
                if (load.isEmpty()) {
                    activityUtil.sendSnack(getString(R.string.no_save_data))
                } else {
                    Log.e("LOAD", load)
                    robotController.resetArena()
                    robotController.updateArena(load)
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
                timerCardLabel.text = getString(R.string.timer_minute_second, minutes.toString().padStart(2, '0'), seconds.toString().padStart(2, '0'))
            }

            override fun onFinish() {}
        }

        timer.start()
    }

    private fun stopTimer() {
        val type: String = if (currentMode == Mode.EXPLORATION) getString(R.string.exploration) else getString(R.string.fastest_path)
        displayInChat(MessageType.SYSTEM, "$type - ${timerCardLabel.text.toString().trim()}")
        timer.cancel()
    }

    private fun resetArena(clear: Boolean) {
        val s = if (clear) getString(R.string.clear_arena_timer) else getString(R.string.reset_arena_timer)
        activityUtil.sendYesNoDialog(s) {
            if (it) {
                if (clear) robotController.clearArena()
                else robotController.resetArena()
                timerCardLabel.text = getString(R.string.timer_default)
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

        robotController.updateRobotImage()
    }

    private fun updateImage(data: String) {
        val s = data.split(", ")

        try {
            val x = s[0].toInt()
            val y = s[1].toInt()
            val id = s[2].toInt()
            robotController.setImage(x, y, id)
        } catch (e: NumberFormatException) {
            activityUtil.sendSnack(getString(R.string.something_went_wrong))
            return
        }
    }

    private fun updateRobot(data: String) {
        val s = data.split(", ")

        try {
            val x = s[0].toInt()
            val y = s[1].toInt()
            val r = s[2].toInt()
            CoroutineScope(Dispatchers.Main).launch {
                robotController.updateRobot(x, y, r)
            }
        } catch (e: NumberFormatException) {
            activityUtil.sendSnack(getString(R.string.something_went_wrong))
            return
        }
    }
}
