package ntu.mdp.android.mdptestkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main_simple.*
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.App.Companion.appTheme
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.isSimple
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.testExplore
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainSimpleBinding
import ntu.mdp.android.mdptestkotlin.MainActivityController.Companion.currentMode
import ntu.mdp.android.mdptestkotlin.MainActivityController.Companion.robotAutonomous
import ntu.mdp.android.mdptestkotlin.arena.ArenaV2
import ntu.mdp.android.mdptestkotlin.arena.ArenaV2.Companion.isPlotting
import ntu.mdp.android.mdptestkotlin.arena.ArenaV2.Companion.isWaitingUpdate
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil
import ntu.mdp.android.mdptestkotlin.utils.ScratchPad


class MainSimpleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainSimpleBinding
    private lateinit var activityUtil: ActivityUtil
    private lateinit var mainActivityController: MainActivityController
    private lateinit var buttonList: List<FloatingActionButton>

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
        mainActivityController = MainActivityController(this, activityUtil, binding)

        isPlotting = true
        isWaitingUpdate = true
        buttonList = listOf(startExplorationButton2, startFastestPathButton2, settingsButton2, plotObstacleButton2, removeObstacleButton2, clearObstacleButton2, saveMapButton2, loadMapButton2)
    }

    override fun onStart() {
        super.onStart()
        mainActivityController.onStart()
    }

    override fun onResume() {
        super.onResume()
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

    fun clickUiButton(view: View) {
        if (!mainActivityController.isClickDelayOver()) return

        when (view.id) {
            R.id.loadMapButton2 -> mainActivityController.onMapLoadClicked()
            R.id.saveMapButton2 -> mainActivityController.onMapSaveClicked()

            R.id.startExplorationButton2 -> {
                if (!robotAutonomous) {
                    mainActivityController.sendCommand(sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(R.string.exploration_default))!!)
                    currentMode = MainActivityController.Mode.EXPLORATION
                }

                mainActivityController.onStartClicked(buttonList)
            }

            R.id.startFastestPathButton2 -> {
                if (!robotAutonomous) {
                    if (!mainActivityController.getArena().isWaypointSet()) {
                        activityUtil.sendSnack("Please set a waypoint first.")
                        return
                    }

                    mainActivityController.sendCommand(sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(R.string.fastest_path_default))!!)
                    currentMode = MainActivityController.Mode.FASTEST_PATH
                }

                mainActivityController.onStartClicked(buttonList)
            }

            R.id.plotObstacleButton2 -> {
                if (ArenaV2.currentPlotFunction != ArenaV2.PlotFunction.PLOT_OBSTACLE) {
                    buttonList.forEach { it.isEnabled = false }
                    view.isEnabled = true
                    ArenaV2.currentPlotFunction = ArenaV2.PlotFunction.PLOT_OBSTACLE
                } else {
                    buttonList.forEach { it.isEnabled = true }
                    ArenaV2.currentPlotFunction = ArenaV2.PlotFunction.NONE
                    mainActivityController.getArena().resetActions()
                }
            }

            R.id.removeObstacleButton2 -> {
                if (ArenaV2.currentPlotFunction != ArenaV2.PlotFunction.REMOVE_OBSTACLE) {
                    buttonList.forEach { it.isEnabled = false }
                    view.isEnabled = true
                    ArenaV2.currentPlotFunction = ArenaV2.PlotFunction.REMOVE_OBSTACLE
                } else {
                    buttonList.forEach { it.isEnabled = true }
                    ArenaV2.currentPlotFunction = ArenaV2.PlotFunction.NONE
                    mainActivityController.getArena().resetActions()
                }
            }

            else -> mainActivityController.clickUiButton(view)
        }
    }
}
