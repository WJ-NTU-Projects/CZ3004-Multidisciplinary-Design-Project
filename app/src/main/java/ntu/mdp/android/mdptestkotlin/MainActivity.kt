package ntu.mdp.android.mdptestkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
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
import ntu.mdp.android.mdptestkotlin.App.Companion.appTheme
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.testExplore
import ntu.mdp.android.mdptestkotlin.MainActivityController.Companion.currentMode
import ntu.mdp.android.mdptestkotlin.MainActivityController.Companion.isPlotting
import ntu.mdp.android.mdptestkotlin.MainActivityController.Companion.robotAutonomous
import ntu.mdp.android.mdptestkotlin.arena.ArenaController
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil
import ntu.mdp.android.mdptestkotlin.utils.ScratchPad
import ntu.mdp.android.mdptestkotlin.utils.TouchController
import ntu.mdp.android.mdptestkotlin.utils.TouchController.Companion.isSwipeMode


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var activityUtil: ActivityUtil
    private lateinit var mainActivityController: MainActivityController
    private lateinit var touchController: TouchController
    private lateinit var normalModeViewList: List<View>
    private lateinit var plotModeViewList: List<View>
    private lateinit var buttonList: List<View>

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var startFabOpened = false
    private var mapButtonsOpened = false

    private val onEnter = View.OnKeyListener { view, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
            val message: String = (view as EditText).text.toString().trim()

            if (message.isNotBlank()) {
                mainActivityController.sendCommand(message)
                messagesOutputEditText.setText("")
            }

            return@OnKeyListener true
        }

        false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN), 1001)
        activityUtil = ActivityUtil(this)
        activityUtil.toggleProgressBar(View.VISIBLE, opaque = true, instant = true)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            activityUtil.sendDialog(getString(R.string.error_bluetooth_not_supported), finish = true)
            return
        }

        autoUpdateArena = sharedPreferences.getBoolean(getString(R.string.app_pref_auto_update), true)
        isPlotting = false
        val animation: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.main_fab_close_init)
        startExplorationFab.startAnimation(animation)
        startFastestPathFab.startAnimation(animation)
        saveMapButton.startAnimation(animation)
        loadMapButton.startAnimation(animation)

        normalModeViewList = listOf(settingsButton, startButton, plotButton, mapButton, resetButton, f1Button, f2Button, statusCard, modeCard, coordinatesCard, timerCard, messagesCard, messagesInputCard, controlPadCard)
        plotModeViewList = listOf(plotObstacleButton, removeObstacleButton, clearObstacleButton, doneButton)
        buttonList = listOf(settingsButton, plotButton, mapButton, resetButton, f1Button, f2Button, messagesSendButton, padForwardButton, padLeftButton, padRightButton, padReverseButton)

        mainActivityController = MainActivityController(this, activityUtil, binding)
        touchController = TouchController(this, mainActivityController, binding) { statusLabel.text = it }
        padForwardButton.isClickable = false
        padReverseButton.isClickable = false
        padLeftButton.isClickable = false
        padRightButton.isClickable = false
        swipePadLayout.setOnTouchListener(touchController.touchListener)
        messagesOutputEditText.setOnKeyListener(onEnter)

        CoroutineScope(Dispatchers.Default).launch {
            delay(100)
            withContext(Dispatchers.Main) {
                startExplorationFab.visibility = View.GONE
                startFastestPathFab.visibility = View.GONE
                saveMapButton.visibility = View.GONE
                loadMapButton.visibility = View.GONE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (bluetoothAdapter == null) return
        mainActivityController.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (bluetoothAdapter == null) return
        if (startFabOpened) toggleFabs()
        f1Button.text = sharedPreferences.getString(getString(R.string.app_pref_label_f1), getString(R.string.f1_default))
        f2Button.text = sharedPreferences.getString(getString(R.string.app_pref_label_f2), getString(R.string.f2_default))
        mainActivityController.onResume()

        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                activityUtil.toggleProgressBar(View.GONE)
            }
        }
    }

    override fun onBackPressed() {
        mainActivityController.onBackPressed()
    }

    @Suppress("unused")
    fun clickUiButton(view: View) {
        if (!mainActivityController.isClickDelayOver()) return

        when (view.id) {
            R.id.mapButton -> {
                if (startFabOpened) toggleFabs()
                toggleMapButtons()
            }

            R.id.startButton -> {
                if (robotAutonomous) {
                    mainActivityController.sendCommand(sharedPreferences.getString(getString(R.string.app_pref_pause), getString(R.string.pause_default))!!)
                    mainActivityController.onStartClicked(buttonList)
                } else {
                    if (mapButtonsOpened) toggleMapButtons()
                    toggleFabs()
                }
            }

            R.id.plotButton -> {
                if (isPlotting) mainActivityController.arenaController.resetActions()
                onPlotClicked()
            }

            R.id.startExplorationFab -> {
                mainActivityController.sendCommand(sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(R.string.exploration_default))!!)
                currentMode = MainActivityController.Mode.EXPLORATION
                mainActivityController.onStartClicked(buttonList)
                toggleFabs()
            }

            R.id.startFastestPathFab -> {
                if (!mainActivityController.arenaController.isWaypointSet()) {
                    activityUtil.sendSnack("Please set a waypoint first.")
                    return
                }

                mainActivityController.sendCommand(sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(R.string.fastest_path_default))!!)
                currentMode = MainActivityController.Mode.FASTEST_PATH
                mainActivityController.onStartClicked(buttonList)
                toggleFabs()
            }

            R.id.controlModeButton -> {
                isSwipeMode = !isSwipeMode
                val typedValue = TypedValue()
                theme.resolveAttribute(if (isSwipeMode) R.attr.colorAccentTheme else R.attr.colorAccentLighterTheme, typedValue, true)
                @ColorInt val color = typedValue.data
                val visibility: Int = if (isSwipeMode) View.INVISIBLE else View.VISIBLE
                controlModeButton.setTextColor(color)
                padForwardButton.visibility = visibility
                padReverseButton.visibility = visibility
                padLeftButton.visibility = visibility
                padRightButton.visibility = visibility
            }

            R.id.doneButton -> {
                plotModeViewList.forEach { it.isEnabled = true }
                mainActivityController.arenaController.resetActions()
                onPlotClicked()
            }

            R.id.plotObstacleButton -> {
                if (mainActivityController.arenaController.plotMode != ArenaController.PlotMode.PLOT_OBSTACLE) {
                    plotModeViewList.forEach { it.isEnabled = false }
                    view.isEnabled = true
                    mainActivityController.arenaController.plotMode = ArenaController.PlotMode.PLOT_OBSTACLE
                } else {
                    plotModeViewList.forEach { it.isEnabled = true }
                    mainActivityController.arenaController.plotMode = ArenaController.PlotMode.NONE
                    mainActivityController.arenaController.resetActions()
                }
            }

            R.id.removeObstacleButton -> {
                if (mainActivityController.arenaController.plotMode != ArenaController.PlotMode.REMOVE_OBSTACLE) {
                    plotModeViewList.forEach { it.isEnabled = false }
                    view.isEnabled = true
                    mainActivityController.arenaController.plotMode = ArenaController.PlotMode.REMOVE_OBSTACLE
                } else {
                    plotModeViewList.forEach { it.isEnabled = true }
                    mainActivityController.arenaController.plotMode = ArenaController.PlotMode.NONE
                    mainActivityController.arenaController.resetActions()
                }
            }

            R.id.saveMapButton -> {
                toggleMapButtons()
                mainActivityController.onMapSaveClicked()
            }

            R.id.loadMapButton -> {
                toggleMapButtons()
                mainActivityController.onMapLoadClicked()
            }

            else -> mainActivityController.clickUiButton(view)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun clickSendMessage(view: View) {
        if (!mainActivityController.isClickDelayOver()) return
        val message: String = messagesOutputEditText.text.toString().trim()

        if (message.isNotBlank()) {
            mainActivityController.sendCommand(message)
            messagesOutputEditText.setText("")
        }
    }

    private fun toggleFabs() {
        startFabOpened = !startFabOpened
        val animationId: Int = if (startFabOpened) R.anim.main_fab_open else R.anim.main_fab_close
        val animation: Animation =  AnimationUtils.loadAnimation(applicationContext, animationId)
        animation.duration = ANIMATOR_DURATION
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationStart(p0: Animation?) {
                if (startFabOpened) {
                    startExplorationFab.visibility = View.VISIBLE
                    startFastestPathFab.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(p0: Animation?) {
                if (!startFabOpened) {
                    startExplorationFab.visibility = View.GONE
                    startFastestPathFab.visibility = View.GONE
                }
            }
        })

        startExplorationFab.startAnimation(animation)
        startFastestPathFab.startAnimation(animation)
    }

    private fun toggleMapButtons() {
        mapButtonsOpened = !mapButtonsOpened
        val animationId: Int = if (mapButtonsOpened) R.anim.main_fab_open else R.anim.main_fab_close
        val animation: Animation =  AnimationUtils.loadAnimation(applicationContext, animationId)
        animation.duration = ANIMATOR_DURATION
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationStart(p0: Animation?) {
                if (mapButtonsOpened) {
                    saveMapButton.visibility = View.VISIBLE
                    loadMapButton.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(p0: Animation?) {
                if (!mapButtonsOpened) {
                    saveMapButton.visibility = View.GONE
                    loadMapButton.visibility = View.GONE
                }
            }
        })

        saveMapButton.startAnimation(animation)
        loadMapButton.startAnimation(animation)
    }

    private fun onPlotClicked() {
        isPlotting = !isPlotting
        if (startFabOpened) toggleFabs()
        val firstList = if (isPlotting) normalModeViewList else plotModeViewList // to hide
        val secondList = if (isPlotting) plotModeViewList else normalModeViewList // to show

        activityUtil.scaleViews(firstList, false) {
            activityUtil.toggleProgressBar(View.VISIBLE, opaque = true) {
                gridParent.pivotX = 0.0f
                gridParent.pivotY = 0.0f
                gridParent.scaleX = if (isPlotting) 1.44f else 1.0f
                gridParent.scaleY = if (isPlotting) 1.44f else 1.0f

                activityUtil.toggleProgressBar(View.GONE) {
                    activityUtil.scaleViews(secondList, true)
                    if (!isPlotting) {
                        CoroutineScope(Dispatchers.Default).launch {
                            delay(250)

                            withContext(Dispatchers.Main) {
                                messagesScrollView.fullScroll(View.FOCUS_DOWN)
                            }
                        }
                    }
                }
            }
        }
    }
}
