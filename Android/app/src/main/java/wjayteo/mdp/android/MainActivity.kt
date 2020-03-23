package wjayteo.mdp.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.LocaleList
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import wjayteo.mdp.android.App.Companion.APP_LANGUAGE
import wjayteo.mdp.android.App.Companion.CLICK_DELAY
import wjayteo.mdp.android.App.Companion.DESCRIPTOR_DIVIDER
import wjayteo.mdp.android.App.Companion.EXPLORATION_COMMAND
import wjayteo.mdp.android.App.Companion.FASTEST_PATH_COMMAND
import wjayteo.mdp.android.App.Companion.IS_TABLET
import wjayteo.mdp.android.App.Companion.LAST_CONNECTED_DEVICE
import wjayteo.mdp.android.App.Companion.PAD_MOVABLE
import wjayteo.mdp.android.App.Companion.PC_PREFIX
import wjayteo.mdp.android.App.Companion.TILT_MOVABLE
import wjayteo.mdp.android.App.Companion.ACCELEROMETER
import wjayteo.mdp.android.App.Companion.APP_THEME
import wjayteo.mdp.android.App.Companion.sharedPreferences
import wjayteo.mdp.android.App.Companion.SIM_DELAY
import wjayteo.mdp.android.App.Companion.SIM_MODE
import wjayteo.mdp.android.arena.*
import wjayteo.mdp.android.bluetooth.BluetoothController
import wjayteo.mdp.android.bluetooth.BluetoothMessageParser
import wjayteo.mdp.android.databinding.ActivityMainBinding
import wjayteo.mdp.android.room.AppDatabase
import wjayteo.mdp.android.room.arena.Arena
import wjayteo.mdp.android.bluetooth.BluetoothActivity
import wjayteo.mdp.android.settings.SettingsActivity
import wjayteo.mdp.android.simulation.Callback
import wjayteo.mdp.android.simulation.Exploration
import wjayteo.mdp.android.simulation.FastestPath
import wjayteo.mdp.android.utils.ActivityUtil
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    companion object {
        var currentMode: Mode = Mode.NONE
        const val MAX_MESSAGE_SIZE = 30
        const val APP_EXIT_CODE = 1000
        const val BLUETOOTH_NOT_SUPPORTED_CODE = 1200
        const val SAVE_REQUEST_CODE = 10000
        const val LOAD_REQUEST_CODE = 10001
        const val CLEAR_ARENA_CODE = 11000
        const val CLEAR_MESSAGE_CODE = 12000
        const val LONG_PRESS_CHOICE_CODE = 13000
        const val PLOT_FASTEST_PATH_CODE = 13001
        const val TOGGLE_VISIBILITY_CODE = 14000
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
            onSendEnter(view as EditText)
            return@OnKeyListener true
        }

        false
    }

    private val bluetoothCallback       : (status: BluetoothController.Status, message: String) -> Unit = { status, message -> handleBluetoothCallback(status, message) }
    private val robotControllerCallback : (status: ArenaMap.Callback, message: String) -> Unit = { status, message -> handleRobotControllerCallback(status, message) }
    private val messageParserCallback   : (status: BluetoothMessageParser.MessageStatus, message: String) -> Unit = { status, message -> handleMessageParserCallback(status, message) }
    private val explorationCallback     : (callback: Callback) -> Unit = { callback -> handleSimulationCallback(callback) }
    private val fastestPathCallback     : (callback: Callback) -> Unit = { callback -> handleSimulationCallback(callback) }

    private lateinit var binding                : ActivityMainBinding
    private lateinit var activityUtil           : ActivityUtil
    private lateinit var bluetoothAdapter       : BluetoothAdapter
    private lateinit var arenaMapController     : ArenaMapController
    private lateinit var robotController        : RobotController
    private lateinit var bluetoothMessageParser : BluetoothMessageParser
    private lateinit var database               : AppDatabase
    private lateinit var buttonList             : ArrayList<View>
    private lateinit var timer                  : CountDownTimer
    private lateinit var exploration            : Exploration
    private lateinit var fastestPath            : FastestPath
    private lateinit var sensorManager          : SensorManager
    private lateinit var messageList            : List<String>

    private var gyroscopeSensor : Sensor? = null
    private var lastClickTime   : Long = 0L
    private var reconnectCounter: Int = 0

    override fun attachBaseContext(newBase: Context?) {
        val res: Resources? = newBase?.resources
        val configuration: Configuration? = res?.configuration
        val newLocale = Locale(APP_LANGUAGE)
        configuration?.setLocale(newLocale)
        val localeList = LocaleList(newLocale)
        LocaleList.setDefault(localeList)
        configuration?.setLocales(localeList)

        if (configuration != null) {
            val context = newBase.createConfigurationContext(configuration)
            super.attachBaseContext(ContextWrapper(context))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
       super.applyOverrideConfiguration(baseContext.resources.configuration)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(APP_THEME)
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
        IS_TABLET = resources.getBoolean(R.bool.isTablet)
        buttonList = arrayListOf(infoButton, bluetoothButton, settingsButton, tiltButton, exploreButton, fastestPathButton, plotButton, plotPathButton, saveMapButton, loadMapButton, visibilityButton, clearArenaButton, f1Button, f2Button, messagesOutputEditText, messageCardClearButton, messagesSendButton, padForwardButton, padLeftButton, padRightButton, padReverseButton)
        arenaMapController = ArenaMapController(this, robotControllerCallback)
        robotController = RobotController(this, binding, arenaMapController, robotControllerCallback)
        bluetoothMessageParser = BluetoothMessageParser(messageParserCallback)
        database = AppDatabase.getDatabase(applicationContext)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        controllerPad.setOnTouchListener(robotController.touchListener)
        messagesOutputEditText.setOnKeyListener(onEnter)
        messageList = arrayListOf()

        if (gyroscopeSensor == null) {
            buttonList.remove(tiltButton)
            tiltButton.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (BluetoothAdapter.getDefaultAdapter() == null) return

        if (IS_TABLET) {
            f1Button.text = sharedPreferences.getString(getString(R.string.app_pref_label_f1), getString(R.string.f1_default))
            f2Button.text = sharedPreferences.getString(getString(R.string.app_pref_label_f2), getString(R.string.f2_default))
        }

        statusCardLabel.text = getString(R.string.idle)
        if (ACCELEROMETER && gyroscopeSensor != null) sensorManager.registerListener(robotController.gyroscopeSensorListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        //if (bluetoothAdapter.isEnabled) startBluetoothListener()
        BluetoothController.callback = bluetoothCallback
        reconnectCounter = 0

        CoroutineScope(Dispatchers.Main).launch {
            arenaMapController.updateRobotImage()
        }

        if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
            arenaMapController.showTKL(!BluetoothController.isSocketConnected())
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(robotController.gyroscopeSensorListener)
    }

    override fun onBackPressed() {
        activityUtil.sendYesNoDialog(APP_EXIT_CODE, getString(R.string.exit_the_app))
    }

    private fun startBluetoothListener() {
        if (!BluetoothController.isSocketConnected()) {
            val device: BluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(LAST_CONNECTED_DEVICE)
            BluetoothController.startClient(device, bluetoothCallback)
            return

            // FOR AMDTOOL ONLY
//            BluetoothController.startServer(bluetoothCallback)
//            return
        }
    }

    @Suppress("unused")
    fun clickUiButton(view: View) {
        if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) return
        lastClickTime = System.currentTimeMillis()

        when (view.id) {
            R.id.bluetoothButton -> activityUtil.startActivity(BluetoothActivity::class.java)
            R.id.settingsButton -> activityUtil.startActivity(SettingsActivity::class.java)
            R.id.messageCardClearButton -> activityUtil.sendYesNoDialog(CLEAR_MESSAGE_CODE, getString(R.string.clear_message_log))
            R.id.saveMapButton -> onMapSaveClicked()
            R.id.loadMapButton -> onMapLoadClicked()
            R.id.clearArenaButton -> clearArena()
            R.id.visibilityButton -> activityUtil.sendYesNoDialog(TOGGLE_VISIBILITY_CODE, getString(R.string.set_arena_as), getString(R.string.explored), getString(R.string.unexplored))
            R.id.infoButton -> showMdf();

            R.id.plotPathButton -> {
                if (arenaMapController.isWaypointSet()) arenaMapController.plotFastestPath() //activityUtil.sendYesNoDialog(PLOT_FASTEST_PATH_CODE, "Plot fastest path?")
                else activityUtil.sendSnack(getString(R.string.set_waypoint))
            }

            R.id.tiltButton -> {
                if (gyroscopeSensor == null) return
                ACCELEROMETER = !ACCELEROMETER
                PAD_MOVABLE = !ACCELEROMETER
                TILT_MOVABLE = ACCELEROMETER

                if (ACCELEROMETER) {
                    tiltButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_on_state_list)
                    sensorManager.registerListener(robotController.gyroscopeSensorListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
                    activityUtil.sendSnack(getString(R.string.accelerometer_on))
                } else {
                    tiltButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_state_list)
                    sensorManager.unregisterListener(robotController.gyroscopeSensorListener)
                    robotController.reset()
                    activityUtil.sendSnack(getString(R.string.accelerometer_off))
                }
            }

            R.id.exploreButton -> {
                val mode = if (currentMode == Mode.NONE) Mode.EXPLORATION else Mode.NONE
                onStartClicked(mode)
            }

            R.id.fastestPathButton -> {
                if (!arenaMapController.isWaypointSet()) {
                    activityUtil.sendSnack(getString(R.string.set_waypoint))
                    return
                }

                val mode = if (currentMode == Mode.NONE) Mode.FASTEST_PATH else Mode.NONE
                onStartClicked(mode)
            }

            R.id.plotButton -> {
                if (arenaMapController.getCurrentFunction() == ArenaMap.PlotFunction.NONE) {
                    arenaMapController.setPlotFunction(ArenaMap.PlotFunction.PLOT_OBSTACLE)
                    buttonList.forEach { it.isEnabled = false }
                    plotButton.isEnabled = true
                    plotButton.icon = getDrawable(R.drawable.ic_done)
                } else {
                    arenaMapController.setPlotFunction(ArenaMap.PlotFunction.NONE)
                    arenaMapController.resetActions()
                    buttonList.forEach { it.isEnabled = true }
                    plotButton.icon = getDrawable(R.drawable.ic_plot_obstacles)
                }
            }

            R.id.f1Button, R.id.f2Button -> {
                val text = if (view.id == R.id.f1Button) sharedPreferences.getString(getString(R.string.app_pref_command_f1), getString(R.string.f1_default))
                else sharedPreferences.getString(getString(R.string.app_pref_command_f2), getString(R.string.f2_default))
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
        if (!bluetoothAdapter.isEnabled || !BluetoothController.isSocketConnected()) return
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
        val tempList: ArrayList<String> = arrayListOf()
        tempList.addAll(messageList)
        tempList.add(displayMessage)

        messageList =
            if (tempList.size > MAX_MESSAGE_SIZE) tempList.subList(tempList.size - MAX_MESSAGE_SIZE, tempList.size)
            else tempList.toList()


        var newMessage = ""

        for (m in messageList) {
            if (newMessage.isNotBlank()) newMessage += "\n"
            newMessage += m
        }

        //val previousMessages = messagesTextView?.text.toString().trim()
        //val newMessage = if (previousMessages.isNotBlank()) "$previousMessages\n$displayMessage" else displayMessage
        messagesTextView?.text = newMessage

        CoroutineScope(Dispatchers.Default).launch {
            delay(250)

            withContext(Dispatchers.Main) {
                messagesScrollView?.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun onStartClicked(mode: Mode) {
        if (SIM_MODE) {
            when (mode) {
                Mode.EXPLORATION -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(SIM_DELAY)
                        arenaMapController.moveRobotToStart()
                        arenaMapController.saveObstacles()
                        arenaMapController.resetGoalPoint()
                        if (::exploration.isInitialized) exploration.end()
                        exploration = Exploration(arenaMapController, explorationCallback)
                        exploration.start()
                    }
                }

                Mode.FASTEST_PATH -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(SIM_DELAY)
                        arenaMapController.moveRobotToStart()
                        arenaMapController.resetWaypoint()
                        arenaMapController.resetGoalPoint()
                        if (::fastestPath.isInitialized) fastestPath.end()
                        fastestPath = FastestPath(arenaMapController, fastestPathCallback)
                        fastestPath.start()
                    }
                }

                else -> {
                    if (::exploration.isInitialized) exploration.end()
                    if (::fastestPath.isInitialized) fastestPath.end()
                    statusCardLabel.text = getString(R.string.idle)
                }
            }
        } else {
            if (mode == Mode.EXPLORATION) {
                arenaMapController.resetArena()
                sendCommand("$PC_PREFIX$EXPLORATION_COMMAND")
            } else if (mode == Mode.FASTEST_PATH) {
                sendCommand("$PC_PREFIX$FASTEST_PATH_COMMAND")
            } else {
                sendCommand("${PC_PREFIX}terminate")
            }
        }

        when (mode) {
            Mode.EXPLORATION -> {
                if (ACCELEROMETER && gyroscopeSensor != null) {
                    sensorManager.unregisterListener(robotController.gyroscopeSensorListener)
                    ACCELEROMETER = false
                    tiltButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_state_list)
                    robotController.reset()
                }

                controllerPad.setOnTouchListener(null)
                buttonList.forEach { it.isEnabled = false }
                exploreButton.isEnabled = true
                exploreButton.icon = getDrawable(R.drawable.ic_pause)
                exploreButton.text = getString(R.string.pause)
                modeCardLabel.text = getString(R.string.exploration)
                displayInChat(MessageType.SYSTEM, getString(R.string.started_something, getString(R.string.exploration)))
                if (!SIM_MODE) startTimer()
            }

            Mode.FASTEST_PATH -> {
                if (ACCELEROMETER && gyroscopeSensor != null) {
                    sensorManager.unregisterListener(robotController.gyroscopeSensorListener)
                    ACCELEROMETER = false
                    tiltButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_state_list)
                    robotController.reset()
                }

                controllerPad.setOnTouchListener(null)
                buttonList.forEach { it.isEnabled = false }
                fastestPathButton.isEnabled = true
                fastestPathButton.icon = getDrawable(R.drawable.ic_pause)
                fastestPathButton.text = getString(R.string.pause)
                modeCardLabel.text = getString(R.string.fastest)
                displayInChat(MessageType.SYSTEM, getString(R.string.started_something, getString(R.string.fastest_path)))
                if (!SIM_MODE) startTimer()
            }

            Mode.NONE -> {
                //sendCommand(sharedPreferences.getString(getString(R.string.app_pref_pause), getString(R.string.pause_default))!!)
                stopTimer()
                controllerPad.setOnTouchListener(robotController.touchListener)
                buttonList.forEach { it.isEnabled = true }
                exploreButton.icon = getDrawable(R.drawable.ic_explore)
                exploreButton.text = getString(R.string.ex)
                fastestPathButton.icon = getDrawable(R.drawable.ic_fastest)
                fastestPathButton.text = getString(R.string.fp)
                modeCardLabel.text = getString(R.string.none)
                if (currentMode == Mode.EXPLORATION) showMdf()
            }
        }

        currentMode = mode
    }

    private fun startTimer() {
        timerCardLabel.text = getString(R.string.timer_default)

        timer = object: CountDownTimer(Long.MAX_VALUE, 1000) {
            var timerCounter: Int = -1

            override fun onFinish() {}
            override fun onTick(p0: Long) {
                timerCounter++
                val seconds: Int = timerCounter % 60
                val minutes: Int = Math.floorDiv(timerCounter, 60)
                timerCardLabel.text = getString(R.string.timer_minute_second, minutes.toString().padStart(2, '0'), seconds.toString().padStart(2, '0'))
            }
        }

        timer.start()
    }

    private fun stopTimer() {
        val type: String = if (currentMode == Mode.EXPLORATION) getString(R.string.exploration) else getString(R.string.fastest_path)
        displayInChat(MessageType.SYSTEM, "$type - ${timerCardLabel.text.toString().trim()}")
        if (::timer.isInitialized) timer.cancel()
    }

    private fun clearArena() {
        activityUtil.sendYesNoDialog(CLEAR_ARENA_CODE, getString(R.string.clear_arena_timer))
    }

    private fun connectionChanged(status: BluetoothController.Status) {
        CoroutineScope(Dispatchers.Main).launch {
            arenaMapController.updateRobotImage()
        }

        when (status) {
            BluetoothController.Status.DISCONNECTED, BluetoothController.Status.CONNECT_FAILED -> {
                arenaMapController.showTKL(true)
                startBluetoothListener()
            }

            BluetoothController.Status.CONNECTED -> {
                arenaMapController.showTKL(false)
                //ArenaMap.isWaitingUpdate = true
                //sendCommand(SEND_ARENA_COMMAND)
            }

            else -> {}
        }
    }

    private fun showMdf() {
        val descriptors: ArrayList<String> = arenaMapController.getMapDescriptorList()
        var message = "Map Descriptor:\n${descriptors[0]}\n\nObstacle Descriptor:\n${descriptors[1]}"
        val images: ArrayList<String> = arenaMapController.getImageList()
        if (images.isNotEmpty()) message += "\n\nImages Found:"
        for (image in images) message += "\n$image"
        activityUtil.sendSnackIndefinite(message)
    }

    private fun updateImage(data: String) {
        arenaMapController.updateImages(data)
    }

    private fun updateRobot(data: String) {
        val s = data.split(", ")

        try {
            val x = s[0].toInt()
            val y = s[1].toInt()
            val r = s[2].toInt()
            CoroutineScope(Dispatchers.Main).launch {
                arenaMapController.updateRobot(x, y, r)
            }
        } catch (e: NumberFormatException) {
            activityUtil.sendSnack(getString(R.string.something_went_wrong))
            return
        }
    }

    private fun handleMessageParserCallback(status: BluetoothMessageParser.MessageStatus, message: String) {
        when (status) {
            BluetoothMessageParser.MessageStatus.GARBAGE -> displayInChat(MessageType.INCOMING, message)
            BluetoothMessageParser.MessageStatus.ARENA -> arenaMapController.updateArena(message)
            BluetoothMessageParser.MessageStatus.IMAGE_POSITION -> updateImage(message)
            BluetoothMessageParser.MessageStatus.ROBOT_POSITION -> updateRobot(message)
            BluetoothMessageParser.MessageStatus.INFO -> activityUtil.sendSnack(message)
            BluetoothMessageParser.MessageStatus.ROBOT_STATUS -> statusCardLabel.text = message
            BluetoothMessageParser.MessageStatus.RUN_ENDED -> onStartClicked(Mode.NONE)
        }

    }

    private fun handleRobotControllerCallback(status: ArenaMap.Callback, message: String) {
        when (status) {
            ArenaMap.Callback.MESSAGE -> activityUtil.sendSnack(message)
            ArenaMap.Callback.SEND_COMMAND -> sendCommand(message)
            ArenaMap.Callback.UPDATE_COORDINATES -> coordinatesCardLabel.text = message
            ArenaMap.Callback.UPDATE_STATUS -> statusCardLabel.text = message
            ArenaMap.Callback.LONG_PRESS_CHOICE -> activityUtil.sendYesNoDialog(LONG_PRESS_CHOICE_CODE, "Plot which?", leftLabel = "START", rightLabel = "GOAL")
        }
    }

    private fun handleBluetoothCallback(status: BluetoothController.Status, message: String) {
        when (status) {
            BluetoothController.Status.CONNECTED -> {
                activityUtil.sendSnack(message)
                displayInChat(MessageType.SYSTEM, getString(R.string.bluetooth_connection_successful))
                connectionChanged(status)
                reconnectCounter = 0
            }

            BluetoothController.Status.CONNECT_FAILED, BluetoothController.Status.DISCONNECTED -> {
                activityUtil.sendSnack(message)

                if (reconnectCounter >= 12) {
                    displayInChat(MessageType.SYSTEM, getString(R.string.failed_reconnection))
                    return
                }

                Handler().postDelayed({
                    reconnectCounter++
                    displayInChat(MessageType.SYSTEM, getString(R.string.attempt_reconnection))
                    connectionChanged(status)
                }, 5000)
            }

            BluetoothController.Status.READ -> bluetoothMessageParser.parse(message)
            BluetoothController.Status.WRITE_SUCCESS -> Log.d(this::class.simpleName ?: "-", message)
            else -> activityUtil.sendSnack(message)
        }
    }

    private fun onSendEnter(view: EditText) {
        val message: String = view.text.toString().trim()

        if (message.isNotBlank()) {
            sendCommand(message)
            messagesOutputEditText.setText("")
        }
    }

    private fun handleSimulationCallback(callback: Callback) {
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

    private fun onMapSaveClicked() {
        startActivityForResult(Intent(this, MapSaveActivity::class.java), SAVE_REQUEST_CODE)
    }

    private fun onMapLoadClicked() {
        startActivityForResult(Intent(this, MapLoadActivity::class.java), LOAD_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LONG_PRESS_CHOICE_CODE) {
            if (resultCode == Activity.RESULT_OK) arenaMapController.selectPoint(true)
            else if (resultCode == Activity.RESULT_CANCELED) arenaMapController.selectPoint(false)
            return
        }

        if (requestCode == TOGGLE_VISIBILITY_CODE) {
            if (resultCode == Activity.RESULT_OK) arenaMapController.setAllExplored(true)
            else if (resultCode == Activity.RESULT_CANCELED) arenaMapController.setAllExplored(false)
            return
        }

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            BLUETOOTH_NOT_SUPPORTED_CODE, APP_EXIT_CODE -> activityUtil.finishActivity()
            CLEAR_MESSAGE_CODE -> messagesTextView?.text = ""
            PLOT_FASTEST_PATH_CODE -> arenaMapController.plotFastestPath()

            CLEAR_ARENA_CODE -> {
                arenaMapController.clearArena()
                timerCardLabel.text = getString(R.string.timer_default)
            }

            SAVE_REQUEST_CODE -> {
                val name: String? = data?.getStringExtra(getString(R.string.app_result_map_name))

                if (name == null) {
                    activityUtil.sendSnack(getString(R.string.something_went_wrong))
                    return
                }

                val save: List<String> = arenaMapController.getMapDescriptor().split(DESCRIPTOR_DIVIDER)

                if (save.size != 2) {
                    activityUtil.sendSnack(getString(R.string.something_went_wrong))
                    return
                }

                val start: IntArray = arenaMapController.getStartPosition()
                val waypoint: IntArray = arenaMapController.getWaypointPosition()
                val goal: IntArray = arenaMapController.getGoalPosition()
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
                        arenaMapController.emptyArena()
                        arenaMapController.setStartPoint(arena.startX, arena.startY)
                        arenaMapController.setGoalPoint(arena.goalX, arena.goalY)
                        if (arenaMapController.isValidCoordinates(arena.waypointX, arena.waypointY)) arenaMapController.setWaypoint(arena.waypointX, arena.waypointY)

                        val descriptor = "${arena.map_descriptor}${DESCRIPTOR_DIVIDER}${arena.obstacle_descriptor}"
                        arenaMapController.updateArena(descriptor)
                    }
                }.invokeOnCompletion {
                    activityUtil.sendSnack(getString(R.string.map_loaded))
                }
            }
        }
    }
}
