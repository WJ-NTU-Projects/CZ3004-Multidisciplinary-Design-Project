package ntu.mdp.android.mdptestkotlin.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings_experimental.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.allowDiagonalExploration
import ntu.mdp.android.mdptestkotlin.App.Companion.darkMode
import ntu.mdp.android.mdptestkotlin.App.Companion.fastSimulation
import ntu.mdp.android.mdptestkotlin.App.Companion.isSimple
import ntu.mdp.android.mdptestkotlin.App.Companion.plotPathChosen
import ntu.mdp.android.mdptestkotlin.App.Companion.plotSearch
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationDelay
import ntu.mdp.android.mdptestkotlin.App.Companion.testExplore
import ntu.mdp.android.mdptestkotlin.AppStartActivity
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.SettingsExperimentalBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil

class SettingsExperimentalActivity : AppCompatActivity() {
    private lateinit var binding: SettingsExperimentalBinding
    private lateinit var activityUtil: ActivityUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(App.appTheme)
        super.onCreate(savedInstanceState)
        binding = SettingsExperimentalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityUtil = ActivityUtil(this)
        activityUtil.setTitle(getString(R.string.experimental))

        darkModeSwitch.isChecked = darkMode
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_dark_mode), isChecked).apply()
            darkMode = isChecked
            if (isChecked) App.appTheme = R.style.AppTheme_Dark
            else App.appTheme = R.style.AppTheme
            activityUtil.startActivity(AppStartActivity::class.java, fade = true, startNew = true)
        }

        sadSwitch.isChecked = isSimple
        sadSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_sad_mode), isChecked).apply()
            isSimple = isChecked
            activityUtil.startActivity(AppStartActivity::class.java, fade = true, startNew = true)
        }

        testExploreSwitch.isChecked = testExplore
        testExploreSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_test_explore), isChecked).apply()
            testExplore = isChecked
        }

        plotPathChosenSwitch.isChecked = plotPathChosen
        plotPathChosenSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_plot_path_chosen), isChecked).apply()
            plotPathChosen = isChecked
        }

        plotSearchSwitch.isChecked = plotSearch
        plotSearchSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_plot_search), isChecked).apply()
            plotSearch = isChecked
        }

        diagonalExploreSwitch.isChecked = allowDiagonalExploration
        diagonalExploreSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_diagonal_exploration), isChecked).apply()
            allowDiagonalExploration = isChecked
        }

        fastSimulationSwitch.isChecked = fastSimulation
        fastSimulationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_fast_simulation), isChecked).apply()
            fastSimulation = isChecked
            simulationDelay = if (fastSimulation)  50L else 250L
        }
    }

    override fun onResume() {
        super.onResume()
        BluetoothController.callback = { status, message ->
            if (status != BluetoothController.Status.READ) {
                activityUtil.sendSnack(message)
            }
        }
    }
}
