package ntu.mdp.android.mdptestkotlin

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.main.ArenaController
import ntu.mdp.android.mdptestkotlin.main.MessageParser
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    companion object {
        var isUpdating = false
    }

    enum class MessageType {
        INCOMING,
        OUTGOING,
        SYSTEM
    }

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var activityUtil: ActivityUtil
    private lateinit var buttonArrayList: ArrayList<MaterialButton>
    private lateinit var startFabArrayList: ArrayList<FloatingActionButton>
    private lateinit var wayPointFabArrayList: ArrayList<FloatingActionButton>
    private lateinit var statusLabel: MaterialTextView
    private lateinit var timerLabel: MaterialTextView
    private lateinit var timerTitleLabel: MaterialTextView
    private lateinit var chatDisplayText: TextView
    private lateinit var chatScrollView: ScrollView
    private lateinit var chatSendEditText: EditText
    private lateinit var coordinatesLabel: MaterialTextView
    private lateinit var timer: CountDownTimer
    private lateinit var arenaController: ArenaController
    private lateinit var messageParser: MessageParser

    private var robotAutonomous = false
    private var startFabOpened = false
    private var wayPointFabOpened = false
    private var timerCounter = 0
    private val callback: (status: BluetoothController.Status, message: String) -> Unit = { status, message ->
        if (status == BluetoothController.Status.READ) {
            messageParser.parse(message)
        }

        if (status != BluetoothController.Status.READ && status != BluetoothController.Status.WRITE_SUCCESS) {
            //displayInChat(MessageType.SYSTEM, message)
            activityUtil.sendSnack(message)
        }

        if (status == BluetoothController.Status.CONNECTED) {
            statusLabel.text = getString(R.string.main_connected)
            isUpdating = true
            toggleConnectButton()
            sendCommand(SEND_ARENA_COMMAND)
        }

        if (status == BluetoothController.Status.DISCONNECTED) {
            statusLabel.text = getString(R.string.main_disconnected)
            toggleConnectButton()
            startBluetoothListener()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION),
            1001
        )

        activityUtil = ActivityUtil(this)
        activityUtil.toggleProgressBar(View.VISIBLE, opaque = true)
        autoUpdateArena = sharedPreferences.getBoolean("AUTO_UPDATE", false)
        toggleAutoManualMode()

        if (bluetoothAdapter == null) {
            activityUtil.sendDialog(getString(R.string.error_bluetooth_not_supported), finish = true)
            return
        }

        buttonArrayList = arrayListOf()
        buttonArrayList.add(findViewById(R.id.main_button_settings))
        buttonArrayList.add(findViewById(R.id.main_button_connect))
        buttonArrayList.add(findViewById(R.id.main_button_way_points))
        startFabArrayList = arrayListOf()
        startFabArrayList.add(findViewById(R.id.main_button_start_exploration))
        startFabArrayList.add(findViewById(R.id.main_button_start_fastest))
        wayPointFabArrayList = arrayListOf()
        wayPointFabArrayList.add(findViewById(R.id.main_button_way_points_start))
        wayPointFabArrayList.add(findViewById(R.id.main_button_way_points_point))
        wayPointFabArrayList.add(findViewById(R.id.main_button_way_points_clear))

        val animation: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.main_fab_close_init)
        for (fab in startFabArrayList) fab.startAnimation(animation)
        for (fab in wayPointFabArrayList) fab.startAnimation(animation)

        statusLabel = findViewById(R.id.main_status)
        timerLabel = findViewById(R.id.main_label_timer)
        timerTitleLabel = findViewById(R.id.main_label_timer_title)
        chatDisplayText = findViewById(R.id.main_chat_display)
        chatScrollView = findViewById(R.id.main_chat_scrollview)
        chatSendEditText = findViewById(R.id.main_chat_edittext_send)
        coordinatesLabel = findViewById(R.id.main_label_coordinates)

        arenaController = ArenaController(this) { status, message ->
            when (status) {
                ArenaController.Status.INFO -> activityUtil.sendSnack(message)
                ArenaController.Status.WRITE -> sendCommand(message)
                ArenaController.Status.ROBOT -> displayInChat(MessageType.INCOMING, message)
                ArenaController.Status.COORDINATES -> coordinatesLabel.text = message
            }
        }
        messageParser = MessageParser(this) {status, message ->
            when (status) {
                MessageParser.Status.GARBAGE -> displayInChat(MessageType.INCOMING, message)
                MessageParser.Status.ARENA -> arenaController.updateArena(message)
                MessageParser.Status.IMAGE_POSITION -> arenaController.updateImage(message)
                MessageParser.Status.ROBOT_POSITION -> arenaController.updateRobot(message)
                MessageParser.Status.ROBOT_STATUS -> statusLabel.text = message
            }}
    }

    override fun onStart() {
        super.onStart()
        if (!bluetoothAdapter!!.isEnabled) startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1000)
    }

    override fun onResume() {
        super.onResume()

        if (startFabOpened) {
            toggleFabs(startFabArrayList, startFabOpened)
            startFabOpened = !startFabOpened
        }

        if (wayPointFabOpened) {
            toggleFabs(wayPointFabArrayList, wayPointFabOpened)
            wayPointFabOpened = !wayPointFabOpened
        }

        findViewById<MaterialButton>(R.id.main_button_f1).text = sharedPreferences.getString(getString(R.string.app_pref_label_f1), "F1")
        findViewById<MaterialButton>(R.id.main_button_f2).text = sharedPreferences.getString(getString(R.string.app_pref_label_f2), "F2")

        statusLabel.text = if (BluetoothController.isSocketConnected()) getString(R.string.main_connected)
        else  getString(R.string.main_disconnected)
        toggleConnectButton()

        if (bluetoothAdapter != null && !bluetoothAdapter!!.isEnabled) activityUtil.sendSnack(getString(R.string.error_bluetooth_off))
        else startBluetoothListener()

        CoroutineScope(Dispatchers.Default).launch {
            delay(500)
            withContext(Dispatchers.Main) {
                for (fab in startFabArrayList) fab.visibility = View.GONE
                for (fab in wayPointFabArrayList) fab.visibility = View.GONE
                activityUtil.toggleProgressBar(View.GONE)
            }
        }
    }

    fun clickSettings(view: View) {
        activityUtil.startActivity(SettingsActivity::class.java)
    }

    fun clickConnect(view: View) {
        if (bluetoothAdapter!!.isEnabled) {
            if (BluetoothController.isSocketConnected()) {
                BluetoothController.disconnect()
                toggleConnectButton()
            } else {
                activityUtil.startActivity(BluetoothActivity::class.java)
            }
        }

        else activityUtil.sendDialog(getString(R.string.error_bluetooth_off))
    }

    fun clickStart(view: View) {
        if (!robotAutonomous) {
            if (wayPointFabOpened) {
                toggleFabs(wayPointFabArrayList, wayPointFabOpened)
                wayPointFabOpened = !wayPointFabOpened
            }

            toggleFabs(startFabArrayList, startFabOpened)
            startFabOpened = !startFabOpened
        } else {
            sendCommand(sharedPreferences.getString(getString(R.string.app_pref_pause), getString(R.string.settings_default_pause))!!)
            onStartClicked()
        }
    }

    fun clickWayPoints(view: View) {
        if (startFabOpened) {
            toggleFabs(startFabArrayList, startFabOpened)
            startFabOpened = !startFabOpened
        }

        toggleFabs(wayPointFabArrayList, wayPointFabOpened)
        wayPointFabOpened = !wayPointFabOpened
    }

    fun clickAutoManualToggle(view: View) {
        autoUpdateArena = !autoUpdateArena
        sharedPreferences.edit().putBoolean("AUTO_UPDATE", autoUpdateArena).apply()
        toggleAutoManualMode()
    }

    fun clickResetArena(view: View) {
        arenaController.resetArena()
        timerLabel.text = "00:00"
    }

    fun clickUpdateArena(view: View) {
        isUpdating = true
        sendCommand(SEND_ARENA_COMMAND)
    }


    fun clickCustomButton(view: View) {
        val text = if (view.id == R.id.main_button_f1) sharedPreferences.getString(getString(R.string.app_pref_command_f1), "F1")
        else sharedPreferences.getString(getString(R.string.app_pref_command_f2), "F2")

        if (text == "f" || text == "r") {
            val dir = if (text == "f") 1 else -1
            if (!arenaController.checkClear(dir)) return
        }

        sendCommand(text!!)
    }

    fun clickStartExploration(view: View) {
        toggleFabs(startFabArrayList, startFabOpened)
        startFabOpened = !startFabOpened
        sendCommand(sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(R.string.settings_default_exploration))!!)
        onStartClicked()
        timerTitleLabel.text = getString(R.string.main_timer_exploration)
    }

    fun clickStartFastestPath(view: View) {
        toggleFabs(startFabArrayList, startFabOpened)
        startFabOpened = !startFabOpened
        sendCommand(sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(R.string.settings_default_fastest))!!)
        onStartClicked()
        timerTitleLabel.text = getString(R.string.main_timer_fastest_path)
    }

    fun clickWayPointsClear(view: View) {
        toggleFabs(wayPointFabArrayList, wayPointFabOpened)
        wayPointFabOpened = !wayPointFabOpened
        arenaController.resetGridColors()
    }

    fun clickAddWayPoint(view: View) {
        toggleFabs(wayPointFabArrayList, wayPointFabOpened)
        wayPointFabOpened = !wayPointFabOpened
        arenaController.currentOp = ArenaController.Op.SET_WAY_POINT
    }

    fun clickAddStartPoint(view: View) {
        toggleFabs(wayPointFabArrayList, wayPointFabOpened)
        wayPointFabOpened = !wayPointFabOpened
        arenaController.currentOp = ArenaController.Op.SET_START_POINT
    }

    fun clickForward(view: View) {
        if (arenaController.checkClear(1)) {
            if (!sendCommand(sharedPreferences.getString(getString(R.string.app_pref_forward), getString(R.string.settings_default_forward))!!)) {
            }
        }
    }

    fun clickTurnLeft(view: View) {
        sendCommand(sharedPreferences.getString(getString(R.string.app_pref_turn_left), getString(R.string.settings_default_turn_left))!!)
    }

    fun clickTurnRight(view: View) {
        sendCommand(sharedPreferences.getString(getString(R.string.app_pref_turn_right), getString(R.string.settings_default_turn_right))!!)
    }

    fun clickReverse(view: View) {
        if (arenaController.checkClear(-1)) sendCommand(sharedPreferences.getString(getString(R.string.app_pref_reverse), getString(R.string.settings_default_reverse))!!)
    }

    fun clickSendMessage(view: View) {
        val message: String = chatSendEditText.text.toString().trim()

        if (message.isNotBlank()) {
            sendCommand(message)
            chatSendEditText.setText("")
        }
    }

    private fun toggleFabs(fabArrayList: ArrayList<FloatingActionButton>, state: Boolean) {
        val animationId: Int = if (state) R.anim.main_fab_close else R.anim.main_fab_open
        val animation: Animation =  AnimationUtils.loadAnimation(applicationContext, animationId)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationStart(p0: Animation?) {
                if (!state) {
                    for (fab in fabArrayList) fab.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(p0: Animation?) {
                if (state) {
                    for (fab in fabArrayList) fab.visibility = View.GONE
                }
            }
        })

        for (fab in fabArrayList) fab.startAnimation(animation)
    }

    private fun toggleAutoManualMode() {
        val button: MaterialButton = findViewById(R.id.main_button_update_mode)
        val updateButton: MaterialButton = findViewById(R.id.main_button_update)

        if (autoUpdateArena) {
            button.text = getString(R.string.main_auto)
            button.icon = getDrawable(R.drawable.ic_auto)
            updateButton.startAnimation(AnimationUtils.loadAnimation(applicationContext, R.anim.main_fab_close_up))
        } else {
            button.text = getString(R.string.main_manual)
            button.icon = getDrawable(R.drawable.ic_manual)
            updateButton.startAnimation(AnimationUtils.loadAnimation(applicationContext, R.anim.main_fab_open_up))
        }
    }

    private fun startBluetoothListener() {
        if (!BluetoothController.isSocketConnected()) BluetoothController.startServer(callback)
        else BluetoothController.callback = callback
    }

    private fun sendCommand(command: String): Boolean {
        if (!bluetoothAdapter!!.isEnabled) {
            activityUtil.sendSnack(getString(R.string.error_bluetooth_off))
            return false
        }

        if (!BluetoothController.isSocketConnected()) {
            activityUtil.sendSnack(getString(R.string.error_bluetooth_not_connected))
            return false
        }

        if (command.isNotEmpty()) {
            displayInChat(MessageType.OUTGOING, command)
            BluetoothController.write(command)
            return true
        }

        return false
    }

    private fun onStartClicked() {
        robotAutonomous = !robotAutonomous
        val button: MaterialButton = findViewById(R.id.main_button_start)
        val text: String?

        if (robotAutonomous) {
            button.text = getString(R.string.main_pause)
            button.icon = getDrawable(R.drawable.ic_pause)

            timer = object: CountDownTimer(Long.MAX_VALUE, 1000) {
                override fun onTick(p0: Long) {
                    timerCounter++
                    val seconds: Int = timerCounter % 60
                    val minutes: Int = Math.floorDiv(timerCounter, 60)
                    val s = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                    timerLabel.text = s
                }

                override fun onFinish() {
                    displayInChat(MessageType.INCOMING, "WHY AM I SO SLOW???")
                }
            }

            timer.start()
        } else {
            button.text = getString(R.string.main_start)
            button.icon = getDrawable(R.drawable.ic_start)
            timer.cancel()
            timerCounter = 0
        }

        for (b in buttonArrayList) b.isEnabled = !robotAutonomous
    }

    private fun toggleConnectButton() {
        val button: MaterialButton = findViewById(R.id.main_button_connect)

        if (BluetoothController.isSocketConnected()) {
            button.text = getString(R.string.main_disconnect)
            button.icon = getDrawable(R.drawable.ic_disconnect)
        } else {
            button.text = getString(R.string.main_connect)
            button.icon = getDrawable(R.drawable.ic_connect)
        }
    }

    private fun displayInChat(messageType: MessageType, message: String) {
        val prefix: String = if (messageType == MessageType.INCOMING) "[ROBOT] " else if (messageType == MessageType.OUTGOING) "[ME] " else ""
        val displayMessage = "$prefix$message"
        val previousMessages = chatDisplayText.text.toString().trim()
        val newMessage = "$previousMessages\n$displayMessage"
        chatDisplayText.text = newMessage
        chatScrollView.fullScroll(View.FOCUS_DOWN)

        CoroutineScope(Dispatchers.Default).launch {
            delay(500)

            withContext(Dispatchers.Main) {
                chatScrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
}
