package ntu.mdp.android.mdptestkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.App.Companion.CLICK_DELAY
import ntu.mdp.android.mdptestkotlin.App.Companion.PAD_MOVABLE
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.TILT_MOVABLE
import ntu.mdp.android.mdptestkotlin.App.Companion.accelerometer
import ntu.mdp.android.mdptestkotlin.App.Companion.appTheme
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.darkMode
import ntu.mdp.android.mdptestkotlin.App.Companion.dialogTheme
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationDelay
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationMode
import ntu.mdp.android.mdptestkotlin.arena.ArenaV2
import ntu.mdp.android.mdptestkotlin.arena.RobotController
import ntu.mdp.android.mdptestkotlin.arena.ManualController
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothMessageParser
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainBinding
import ntu.mdp.android.mdptestkotlin.room.AppDatabase
import ntu.mdp.android.mdptestkotlin.room.arena.Arena
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
        const val APP_EXIT_CODE = 1000
        const val BLUETOOTH_ENABLE_CODE = 1100
        const val BLUETOOTH_NOT_SUPPORTED_CODE = 1200
        const val SAVE_REQUEST_CODE = 10000
        const val LOAD_REQUEST_CODE = 10001
        const val RESET_ARENA_CODE = 11000
        const val CLEAR_ARENA_CODE = 11001
        const val CLEAR_MESSAGE_CODE = 12000
        const val LONG_PRESS_CHOICE_CODE = 13000
        const val PLOT_FASTEST_PATH_CODE = 13001
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
                activityUtil.sendYesNoDialog(LONG_PRESS_CHOICE_CODE, "Plot which?", leftLabel = "START", rightLabel = "GOAL")
            }
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
                Callback.START_CLOCK -> startTimer()
            }
        }
    }

    private val fastestPathCallback: (callback: Callback) -> Unit = { callback ->
        CoroutineScope(Dispatchers.Main).launch {
            when (callback) {
                Callback.COMPLETE -> onStartClicked(Mode.NONE)
                Callback.START_CLOCK -> startTimer()
                else -> return@launch
            }
        }
    }

    private lateinit var binding                : ActivityMainBinding
    private lateinit var activityUtil           : ActivityUtil
    private lateinit var bluetoothAdapter       : BluetoothAdapter
    private lateinit var robotController        : RobotController
    private lateinit var manualController        : ManualController
    private lateinit var bluetoothMessageParser : BluetoothMessageParser
    private lateinit var database               : AppDatabase
    private lateinit var buttonList             : List<View>
    private lateinit var timer                  : CountDownTimer
    private lateinit var exploration            : Exploration
    private lateinit var fastestPath            : FastestPath
    private lateinit var sensorManager          : SensorManager
    private lateinit var gyroscopeSensor        : Sensor
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

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            activityUtil.sendDialog(BLUETOOTH_NOT_SUPPORTED_CODE, getString(R.string.error_bluetooth_not_supported))
            return
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        buttonList = listOf(bluetoothButton, communicationButton, tiltButton, darkModeButton, view3dButton, testButton, exploreButton, fastestPathButton, plotButton, plotPathButton, saveMapButton, loadMapButton, resetArenaButton, clearArenaButton, f1Button, f2Button, messagesOutputEditText, messageCardClearButton, messagesSendButton, padForwardButton, padLeftButton, padRightButton, padReverseButton)
        robotController = RobotController(this, robotControllerCallback)
        manualController = ManualController(this, binding, robotController, robotControllerCallback)
        bluetoothMessageParser = BluetoothMessageParser(messageParserCallback)
        database = AppDatabase.getDatabase(applicationContext)
        isTablet = resources.getBoolean(R.bool.isTablet)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscopeSensor =  sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        padForwardButton.isClickable = false
        padReverseButton.isClickable = false
        padLeftButton.isClickable = false
        padRightButton.isClickable = false
        controllerPad.setOnTouchListener(manualController.touchListener)
        messagesOutputEditText.setOnKeyListener(onEnter)

    }

    override fun onStart() {
        super.onStart()
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BLUETOOTH_ENABLE_CODE)
        }
    }

    override fun onResume() {
        super.onResume()

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            return
        }

        if (!isTablet) {
            manualController.toggleSwipeMode(true)
        }

        f1Button.text = sharedPreferences.getString(getString(R.string.app_pref_label_f1), getString(R.string.f1_default))
        f2Button.text = sharedPreferences.getString(getString(R.string.app_pref_label_f2), getString(R.string.f2_default))
        statusCardLabel.text = getString(R.string.idle)

        if (accelerometer) {
            sensorManager.registerListener(manualController.gyroscopeSensorListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        if (!bluetoothAdapter.isEnabled) {
            activityUtil.sendSnack(getString(R.string.error_bluetooth_off))
        } else {
            startBluetoothListener()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(manualController.gyroscopeSensorListener)
    }

    override fun onBackPressed() {
        activityUtil.sendYesNoDialog(APP_EXIT_CODE, getString(R.string.exit_the_app))
    }

    private fun startBluetoothListener() {
        if (!BluetoothController.isSocketConnected()) {
            BluetoothController.startServer(bluetoothCallback)
            return
        }

        BluetoothController.callback = bluetoothCallback

        if (autoUpdateArena) {
            sendCommand(SEND_ARENA_COMMAND)
        }
    }

    @Suppress("unused")
    fun clickUiButton(view: View) {
        if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) {
            return
        }

        lastClickTime = System.currentTimeMillis()

        when (view.id) {
            R.id.bluetoothButton        -> activityUtil.startActivity(SettingsBluetoothActivity::class.java)
            R.id.communicationButton    -> activityUtil.startActivity(SettingsCommunicationActivity::class.java)
            R.id.testButton             -> activityUtil.startActivity(SettingsSimulationActivity::class.java)
            R.id.view3dButton           -> activityUtil.sendDialog(0, "NO", "HAHA")
            R.id.messageCardClearButton -> activityUtil.sendYesNoDialog(CLEAR_MESSAGE_CODE, getString(R.string.clear_message_log))
            R.id.saveMapButton          -> onMapSaveClicked()
            R.id.loadMapButton          -> onMapLoadClicked()
            R.id.clearArenaButton       -> resetArena(true)
            R.id.resetArenaButton       -> resetArena(false)
            R.id.plotPathButton         -> if (robotController.isWaypointSet()) activityUtil.sendYesNoDialog(PLOT_FASTEST_PATH_CODE, "Plot fastest path?")

            R.id.tiltButton -> {
                accelerometer = !accelerometer
                PAD_MOVABLE = !accelerometer
                TILT_MOVABLE = accelerometer

                if (accelerometer) {
                    tiltButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_on_state_list)
                    sensorManager.registerListener(manualController.gyroscopeSensorListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
                    activityUtil.sendSnack(getString(R.string.accelerometer_on))
                } else {
                    tiltButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_state_list)
                    sensorManager.unregisterListener(manualController.gyroscopeSensorListener)
                    manualController.reset()
                    activityUtil.sendSnack(getString(R.string.accelerometer_off))
                }
            }

            R.id.darkModeButton -> {
                darkMode = !darkMode
                sharedPreferences.edit().putBoolean(getString(R.string.app_pref_dark_mode), darkMode).apply()

                if (darkMode) {
                    appTheme = R.style.AppTheme_Dark
                    dialogTheme = R.style.DialogTheme_Dark
                } else {
                    appTheme = R.style.AppTheme
                    dialogTheme = R.style.DialogTheme
                }

                activityUtil.startActivity(MainActivity::class.java, fade = true, startNew = true)
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
                    plotButton.icon = getDrawable(R.drawable.ic_done)
                } else {
                    robotController.setPlotFunction(ArenaV2.PlotFunction.NONE)
                    robotController.resetActions()
                    buttonList.forEach { it.isEnabled = true }
                    plotButton.icon = getDrawable(R.drawable.ic_plot_obstacles)
                }
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
                        delay(simulationDelay)
                        robotController.saveObstacles()
                        robotController.resetGoalPoint()
                        if (::exploration.isInitialized) exploration.end()
                        exploration = Exploration(robotController, explorationCallback)
                        exploration.start()
                    }
                }

                Mode.FASTEST_PATH -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(simulationDelay)
                        robotController.resetWaypoint()
                        robotController.resetGoalPoint()
                        if (::fastestPath.isInitialized) fastestPath.end()
                        fastestPath = FastestPath(robotController, fastestPathCallback)
                        fastestPath.start()
                    }
                }

                else -> {
                    if (::exploration.isInitialized) exploration.end()
                    if (::fastestPath.isInitialized) fastestPath.end()
                    statusCardLabel.text = getString(R.string.idle)
                }
            }
        }

        when (mode) {
            Mode.EXPLORATION -> {
                if (accelerometer) {
                    sensorManager.unregisterListener(manualController.gyroscopeSensorListener)
                    accelerometer = false
                    tiltButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_state_list)
                    manualController.reset()
                }

                controllerPad.setOnTouchListener(null)

                sendCommand(sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(R.string.exploration_default))!!)
                buttonList.forEach { it.isEnabled = false }
                exploreButton.isEnabled = true
                exploreButton.icon = getDrawable(R.drawable.ic_pause)
                modeCardLabel.text = getString(R.string.exploration)
                displayInChat(MessageType.SYSTEM, getString(R.string.started_something, "exploration."))
                if (!simulationMode) startTimer()
            }

            Mode.FASTEST_PATH -> {
                if (accelerometer) {
                    sensorManager.unregisterListener(manualController.gyroscopeSensorListener)
                    accelerometer = false
                    tiltButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_state_list)
                    manualController.reset()
                }

                controllerPad.setOnTouchListener(null)

                sendCommand(sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(R.string.fastest_path_default))!!)
                buttonList.forEach { it.isEnabled = false }
                fastestPathButton.isEnabled = true
                fastestPathButton.icon = getDrawable(R.drawable.ic_pause)
                modeCardLabel.text = getString(R.string.fastest_path)
                displayInChat(MessageType.SYSTEM, getString(R.string.started_something, "fastest path."))
                if (!simulationMode) startTimer()
            }

            Mode.NONE -> {
                sendCommand(sharedPreferences.getString(getString(R.string.app_pref_pause), getString(R.string.pause_default))!!)
                stopTimer()
                controllerPad.setOnTouchListener(manualController.touchListener)
                buttonList.forEach { it.isEnabled = true }
                exploreButton.icon = getDrawable(R.drawable.ic_explore)
                fastestPathButton.icon = getDrawable(R.drawable.ic_fastest)
                modeCardLabel.text = getString(R.string.none)
            }
        }

        currentMode = mode
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
        if (::timer.isInitialized) timer.cancel()
    }

    private fun resetArena(clear: Boolean) {
        val s = if (clear) getString(R.string.clear_arena_timer) else getString(R.string.reset_arena_timer)
        activityUtil.sendYesNoDialog(if (clear) CLEAR_ARENA_CODE else RESET_ARENA_CODE, s)
    }

    private fun connectionChanged(status: BluetoothController.Status) {
        if (status == BluetoothController.Status.CONNECTED) {
            ArenaV2.isWaitingUpdate = true
            sendCommand(SEND_ARENA_COMMAND)
        } else {
            startBluetoothListener()
        }

        CoroutineScope(Dispatchers.Main).launch {
            robotController.updateRobotImage()
        }
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


    private fun onMapSaveClicked() {
        startActivityForResult(Intent(this, MapSaveActivity::class.java), SAVE_REQUEST_CODE)

        /*
        activityUtil.sendYesNoDialog(getString(R.string.save_map_prompt), leftLabel = getString(R.string.yes), rightLabel = getString(R.string.no)) {
            if (it) {
                val save: String = robotController.getMapDescriptor()
                sharedPreferences.edit().putString(getString(R.string.app_pref_map_descriptor_1), save).apply()
                activityUtil.sendSnack(getString(R.string.map_saved))
            }
        }
        */
    }

    private fun onMapLoadClicked() {
        startActivityForResult(Intent(this, MapLoadActivity::class.java), LOAD_REQUEST_CODE)
        /*
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

         */
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LONG_PRESS_CHOICE_CODE) {
            if (resultCode == Activity.RESULT_OK) robotController.selectPoint(true)
            else if (resultCode == Activity.RESULT_CANCELED) robotController.selectPoint(false)
            return
        }

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            BLUETOOTH_NOT_SUPPORTED_CODE, APP_EXIT_CODE -> activityUtil.finishActivity()
            CLEAR_MESSAGE_CODE -> messagesTextView?.text = ""
            PLOT_FASTEST_PATH_CODE -> robotController.plotFastestPath()

            CLEAR_ARENA_CODE -> {
                robotController.clearArena()
                timerCardLabel.text = getString(R.string.timer_default)
            }

            RESET_ARENA_CODE -> {
                robotController.resetArena()
                timerCardLabel.text = getString(R.string.timer_default)
            }

            SAVE_REQUEST_CODE -> {
                val name: String? = data?.getStringExtra(getString(R.string.app_result_map_name))

                if (name == null) {
                    activityUtil.sendSnack(getString(R.string.something_went_wrong))
                    return
                }

                val save: List<String> = robotController.getMapDescriptor().split("//")

                if (save.size != 2) {
                    activityUtil.sendSnack(getString(R.string.something_went_wrong))
                    return
                }

                val start: IntArray = robotController.getStartPosition()
                val waypoint: IntArray = robotController.getWaypointPosition()
                val goal: IntArray = robotController.getGoalPosition()
                val arena = Arena(0, name, save[0], save[1], startX = start[0], startY = start[1], waypointX = waypoint[0], waypointY = waypoint[1], goalX = goal[0], goalY = goal[1])

                CoroutineScope(Dispatchers.IO).launch {
                    database.arenaDao().insert(arena)
                }.invokeOnCompletion {
                    activityUtil.sendSnack(getString(R.string.map_saved))
                }

                return
            }

            LOAD_REQUEST_CODE -> {
                val id: Int = data?.getIntExtra(getString(R.string.app_result_map_id), -1) ?: -1

                if (id < 0) {
                    activityUtil.sendSnack(getString(R.string.something_went_wrong))
                    return
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val arena: Arena? = database.arenaDao().selectById(id)

                    if (arena == null) {
                        activityUtil.sendSnack(getString(R.string.something_went_wrong))
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        robotController.emptyArena()
                        robotController.setStartPoint(arena.startX, arena.startY)
                        robotController.setGoalPoint(arena.goalX, arena.goalY)
                        if (robotController.isValidCoordinates(arena.waypointX, arena.waypointY)) robotController.setWaypoint(arena.waypointX, arena.waypointY)

                        val descriptor = "${arena.map_descriptor}//${arena.obstacle_descriptor}"
                        robotController.updateArena(descriptor)
                    }
                }.invokeOnCompletion {
                    activityUtil.sendSnack(getString(R.string.map_loaded))
                }
            }
        }
    }
}
