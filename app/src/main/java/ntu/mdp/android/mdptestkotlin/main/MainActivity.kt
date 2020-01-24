package ntu.mdp.android.mdptestkotlin.main

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.SettingsActivity
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothActivity
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothService
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothService.Companion.receiver
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothService2
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil


class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_PERMISSIONS = 20200
        const val REQUEST_ENABLE_BLUETOOTH = 20201
        var autoUpdate = false
        var isUpdating = false
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var activityUtil: ActivityUtil
    private lateinit var arenaController: ArenaController
    private lateinit var messageParser: MessageParser
    private var isFabOpened = false
    private var isFab2Opened = false
    private var button1IsStart = true
    private var button2IsStart = true

    private val callback: (status: BluetoothService.Status, message: String) -> Unit = { status, message ->
        if (status == BluetoothService.Status.READ) {
            messageParser.parse(message)
        }

        if (status != BluetoothService.Status.READ && status != BluetoothService.Status.WRITE_SUCCESS) {
            activityUtil.sendSnack(message)
        }

        if (status == BluetoothService.Status.CONNECTED) {
            setConnectionStatus("Connected")
            isUpdating = true
            sendCommand("sendArena")
        }

        if (status == BluetoothService.Status.DISCONNECTED) {
            setConnectionStatus("Disconnected")
            startBluetoothListener()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Loading of persistent data from shared preferences.
        sharedPreferences = this.applicationContext.getSharedPreferences(getString(R.string.app_pref_key), Context.MODE_PRIVATE)
        autoUpdate = sharedPreferences.getBoolean("AUTO_UPDATE", false)
        setAutoManualIcon()
        setStatus("NO DATA")
        setConnectionStatus("Disconnected")

        val toolbarIcon: ImageView = findViewById(R.id.toolbar_icon)
        toolbarIcon.setImageResource(R.drawable.ic_settings)
        toolbarIcon.visibility = View.VISIBLE

        findViewById<ImageView>(R.id.toolbar_icon2).visibility = View.VISIBLE
        findViewById<ImageView>(R.id.toolbar_icon3).visibility = View.VISIBLE
        findViewById<ImageView>(R.id.toolbar_icon5).visibility = View.VISIBLE
        if (!autoUpdate) findViewById<ImageView>(R.id.toolbar_icon4).visibility = View.VISIBLE

        activityUtil = ActivityUtil(this)
        activityUtil.setTitle(getString(R.string.app_name))
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSIONS)

        if (bluetoothAdapter == null) {
            activityUtil.sendDialog(getString(R.string.error_bluetooth_not_supported), finish = true)
            return
        }

        arenaController = ArenaController(this) { status, message ->
            if (status == ArenaController.Status.WRITE) sendCommand(message)
            else if (status == ArenaController.Status.INFO) activityUtil.sendSnack(message)
        }

        messageParser = MessageParser(this) { status, message ->
            when (status) {
                MessageParser.Status.GARBAGE -> activityUtil.sendSnack(message)
                MessageParser.Status.ARENA -> arenaController.updateArena(message)
                MessageParser.Status.IMAGE_POSITION -> arenaController.updateImage(message)
                MessageParser.Status.ROBOT_POSITION -> arenaController.updateRobot(message)
                MessageParser.Status.ROBOT_STATUS -> setStatus(message)
            }
        }

        initFab()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(receiver, filter)
    }

    override fun onStart() {
        super.onStart()

        if (!bluetoothAdapter!!.isEnabled) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH)
            return
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<MaterialButton>(R.id.main_button_f1).text = sharedPreferences.getString(getString(R.string.app_pref_label_f1), "F1")
        findViewById<MaterialButton>(R.id.main_button_f2).text = sharedPreferences.getString(getString(R.string.app_pref_label_f2), "F2")

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            activityUtil.sendSnack(getString(R.string.error_bluetooth_off))
            return
        }

        startBluetoothListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothService.reset()
        unregisterReceiver(receiver)
    }

    private fun setDiscoverable() {
        if (bluetoothAdapter!!.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)
        }
    }

    private fun sendCommand(command: String): Boolean {
        if (!bluetoothAdapter!!.isEnabled) {
            activityUtil.sendSnack(getString(R.string.error_bluetooth_off))
            return false
        }

        if (BluetoothService.isConnected() != true) {
            activityUtil.sendSnack(getString(R.string.error_bluetooth_not_connected))
            return false
        }

        if (command.isNotEmpty()) {
            BluetoothService.writeToDevice(command)
            return true
        }

        return false
    }

    private fun setStatus(status: String) {
        val statusLabel: MaterialTextView = findViewById(R.id.main_status)
        statusLabel.text = status
    }

    private fun setConnectionStatus(status: String) {
        val connectionStatusLabel: MaterialTextView = findViewById(R.id.main_connection_status)
        connectionStatusLabel.text = status
    }

    private fun setAutoManual() {
        autoUpdate = !autoUpdate
        sharedPreferences.edit().putBoolean("AUTO_UPDATE", autoUpdate).apply()
        setAutoManualIcon()
        val s = if (autoUpdate) "automatically" else "manually"
        activityUtil.sendSnack("Arena information will be updated $s.")
    }

    private fun setAutoManualIcon() {
        var icon: ImageView = findViewById(R.id.toolbar_icon2)
        if (autoUpdate) icon.setImageResource(R.drawable.ic_auto)
        else icon.setImageResource(R.drawable.ic_manual)

        icon = findViewById(R.id.toolbar_icon4)
        icon.isEnabled = !autoUpdate
        if (autoUpdate) icon.visibility = View.GONE
        else icon.visibility = View.VISIBLE
    }

    fun clickConnect(view: View) {
        if (!bluetoothAdapter!!.isEnabled) {
            activityUtil.sendDialog(getString(R.string.error_bluetooth_off))
            return
        }

        activityUtil.startActivity(BluetoothActivity::class.java)
    }

    fun clickAutoManual(view: View) {
        setAutoManual()
    }

    fun clickRefresh(view: View) {
        isUpdating = true
        sendCommand("sendArena")
    }

    fun clickTrash(view: View) {
        arenaController.resetArena()
    }

    fun clickSettings(view: View) {
        activityUtil.startActivity(SettingsActivity::class.java)
    }

    fun clickStart(view: View) {
        val text =
            if (button1IsStart) sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(R.string.settings_default_exploration))
            else sharedPreferences.getString(getString(R.string.app_pref_pause), getString(R.string.settings_default_pause))
        if (sendCommand(text!!)) changeButtonState()
    }

    fun clickStart2(view: View) {
        val text =
            if (button2IsStart) sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(R.string.settings_default_fastest))
            else sharedPreferences.getString(getString(R.string.app_pref_pause), getString(R.string.settings_default_pause))
        if (sendCommand(text!!)) changeButtonState2()
    }

    private fun changeButtonState() {
        val button: MaterialButton = findViewById(R.id.main_button_start)
        val button2: MaterialButton = findViewById(R.id.main_button_start2)
        val fab1: FloatingActionButton = findViewById(R.id.main_button_wp)
        val fab2: FloatingActionButton = findViewById(R.id.main_button_pad)
        val text = button.text.toString().trim()

        if (text == getString(R.string.main_start_exploration)) {
            button.text = getString(R.string.main_pause)
            button.icon = getDrawable(R.drawable.ic_pause)
            button1IsStart = false
        }
        else {
            button.text = getString(R.string.main_start_exploration)
            button.icon = getDrawable(R.drawable.ic_start)
            button1IsStart = true
        }

        button2.isEnabled = button1IsStart
        fab1.isEnabled = button1IsStart
        fab2.isEnabled = button1IsStart
        findViewById<ImageView>(R.id.toolbar_icon).isEnabled = button1IsStart
        findViewById<ImageView>(R.id.toolbar_icon3).isEnabled = button1IsStart
        findViewById<ImageView>(R.id.toolbar_icon5).isEnabled = button1IsStart
    }

    private fun changeButtonState2() {
        val button2: MaterialButton = findViewById(R.id.main_button_start)
        val button: MaterialButton = findViewById(R.id.main_button_start2)
        val fab1: FloatingActionButton = findViewById(R.id.main_button_wp)
        val fab2: FloatingActionButton = findViewById(R.id.main_button_pad)
        val text = button.text.toString().trim()

        if (text == getString(R.string.main_start_fastest)) {
            button.text = getString(R.string.main_pause)
            button.icon = getDrawable(R.drawable.ic_pause)
            button2IsStart = false
        }
        else {
            button.text = getString(R.string.main_start_fastest)
            button.icon = getDrawable(R.drawable.ic_start)
            button2IsStart = true
        }

        button2.isEnabled = button2IsStart
        fab1.isEnabled = button2IsStart
        fab2.isEnabled = button2IsStart
        findViewById<ImageView>(R.id.toolbar_icon).isEnabled = button2IsStart
        findViewById<ImageView>(R.id.toolbar_icon3).isEnabled = button2IsStart
        findViewById<ImageView>(R.id.toolbar_icon5).isEnabled = button2IsStart
    }

    fun clickF1(view: View) {
        val text = sharedPreferences.getString(getString(R.string.app_pref_command_f1), "F1")
        processCustomCommand(text!!)
    }
    fun clickF2(view: View) {
        val text = sharedPreferences.getString(getString(R.string.app_pref_command_f2), "F2")
        processCustomCommand(text!!)
    }

    fun clickWPFab(view: View) {
        if (isFab2Opened) toggleFab2()
        toggleFab()
    }

    fun clickWPWay(view: View) {
        arenaController.currentOp = ArenaController.Op.SET_WAYPOINT
        toggleFab()
    }

    fun clickWPStart(view: View) {
        arenaController.currentOp = ArenaController.Op.SET_STARTPOINT
        toggleFab()
    }

    private fun initFab() {
        val fab1: ExtendedFloatingActionButton = findViewById(R.id.main_button_wp_start)
        val fab2: ExtendedFloatingActionButton = findViewById(R.id.main_button_wp_waypoint)
        val fab3: ExtendedFloatingActionButton = findViewById(R.id.main_button_wp_clear)
        val fabForward: FloatingActionButton = findViewById(R.id.main_button_pad_forward)
        val fabReverse: FloatingActionButton = findViewById(R.id.main_button_pad_reverse)
        val fabLeft: FloatingActionButton = findViewById(R.id.main_button_pad_left)
        val fabRight: FloatingActionButton = findViewById(R.id.main_button_pad_right)
        fab1.visibility = View.INVISIBLE
        fab2.visibility = View.INVISIBLE
        fab3.visibility = View.INVISIBLE
        fabForward.visibility = View.INVISIBLE
        fabReverse.visibility = View.INVISIBLE
        fabLeft.visibility = View.INVISIBLE
        fabRight.visibility = View.INVISIBLE

        val fabCloseAnimation: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.main_fab_close_init_right)
        fabCloseAnimation.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                fab1.visibility = View.GONE
                fab2.visibility = View.GONE
                fab3.visibility = View.GONE
            }
        })

        val fabCloseAnimation2: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.main_fab_close_init)
        fabCloseAnimation.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                fabForward.visibility = View.GONE
                fabReverse.visibility = View.GONE
                fabLeft.visibility = View.GONE
                fabRight.visibility = View.GONE
            }
        })

        fab1.startAnimation(fabCloseAnimation)
        fab2.startAnimation(fabCloseAnimation)
        fab3.startAnimation(fabCloseAnimation)
        fabForward.startAnimation(fabCloseAnimation2)
        fabReverse.startAnimation(fabCloseAnimation2)
        fabLeft.startAnimation(fabCloseAnimation2)
        fabRight.startAnimation(fabCloseAnimation2)
    }

    private fun toggleFab() {
        val fab1: ExtendedFloatingActionButton = findViewById(R.id.main_button_wp_start)
        val fab2: ExtendedFloatingActionButton = findViewById(R.id.main_button_wp_waypoint)
        val fab3: ExtendedFloatingActionButton = findViewById(R.id.main_button_wp_clear)
        val fabOpenAnimation: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.main_fab_open_right)
        val fabCloseAnimation: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.main_fab_close_right)

        fabOpenAnimation.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                fab1.visibility = View.VISIBLE
                fab2.visibility = View.VISIBLE
                fab3.visibility = View.VISIBLE
            }
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {}
        })

        fabCloseAnimation.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                fab1.visibility = View.GONE
                fab2.visibility = View.GONE
                fab3.visibility = View.GONE
            }
        })

        if (isFabOpened) {
            fab1.startAnimation(fabCloseAnimation)
            fab2.startAnimation(fabCloseAnimation)
            fab3.startAnimation(fabCloseAnimation)
        } else {
            fab1.startAnimation(fabOpenAnimation)
            fab2.startAnimation(fabOpenAnimation)
            fab3.startAnimation(fabOpenAnimation)
        }

        isFabOpened = !isFabOpened
    }

    private fun toggleFab2() {
        val fabForward: FloatingActionButton = findViewById(R.id.main_button_pad_forward)
        val fabReverse: FloatingActionButton = findViewById(R.id.main_button_pad_reverse)
        val fabLeft: FloatingActionButton = findViewById(R.id.main_button_pad_left)
        val fabRight: FloatingActionButton = findViewById(R.id.main_button_pad_right)
        val fabOpenAnimation: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.main_fab_open)
        val fabCloseAnimation: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.main_fab_close)

        fabOpenAnimation.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                fabForward.visibility = View.VISIBLE
                fabReverse.visibility = View.VISIBLE
                fabLeft.visibility = View.VISIBLE
                fabRight.visibility = View.VISIBLE
            }
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {}
        })

        fabCloseAnimation.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                fabForward.visibility = View.GONE
                fabReverse.visibility = View.GONE
                fabLeft.visibility = View.GONE
                fabRight.visibility = View.GONE
            }
        })

        if (isFab2Opened) {
            fabForward.startAnimation(fabCloseAnimation)
            fabReverse.startAnimation(fabCloseAnimation)
            fabLeft.startAnimation(fabCloseAnimation)
            fabRight.startAnimation(fabCloseAnimation)
        } else {
            fabForward.startAnimation(fabOpenAnimation)
            fabReverse.startAnimation(fabOpenAnimation)
            fabLeft.startAnimation(fabOpenAnimation)
            fabRight.startAnimation(fabOpenAnimation)
        }

        isFab2Opened = !isFab2Opened
    }

    fun clickWPClear(view: View) {
        arenaController.resetGridColors()
    }

    private fun startBluetoothListener() {
        if (BluetoothService.isConnected() != true) {
            Log.e("TEST", "TEST")
            BluetoothService.init(BluetoothService.State.SERVER, callback = callback)
        } else {
            BluetoothService.setCallback(callback)
        }
    }

    private fun processCustomCommand(text: String) {
        if (text == "f" || text == "r") {
            val dir = if (text == "f") 1 else -1
            if (arenaController.checkClear(dir)) sendCommand(text)
            return
        }

        sendCommand(text)
    }

    fun clickPadFab(view: View) {
        if (isFabOpened) toggleFab()
        toggleFab2()
    }

    fun clickForward(view: View) {
        if (arenaController.checkClear(1)) sendCommand("f")
    }

    fun clickReverse(view: View) {
        if (arenaController.checkClear(-1)) sendCommand("r")
    }

    fun clickLeft(view: View) {
        sendCommand("tl")
    }

    fun clickRight(view: View) {
        sendCommand("tr")
    }
}
