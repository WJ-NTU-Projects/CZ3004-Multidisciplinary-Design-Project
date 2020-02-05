package ntu.mdp.android.mdptestkotlin.settings

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings_simulation.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.coverageLimit
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationDelay
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationMode
import ntu.mdp.android.mdptestkotlin.MainActivity
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.SettingsSimulationBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil

class SettingsSimulationActivity : AppCompatActivity() {
    private lateinit var binding: SettingsSimulationBinding
    private lateinit var activityUtil: ActivityUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(App.appTheme)
        super.onCreate(savedInstanceState)
        binding = SettingsSimulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityUtil = ActivityUtil(this)
        activityUtil.setTitle(getString(R.string.simulation))


        darkModeSwitch.isChecked = App.darkMode
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            App.sharedPreferences.edit().putBoolean(getString(R.string.app_pref_dark_mode), isChecked).apply()
            App.darkMode = isChecked
            if (isChecked) App.appTheme = R.style.AppTheme_Dark
            else App.appTheme = R.style.AppTheme
            activityUtil.startActivity(MainActivity::class.java, fade = true, startNew = true)
        }

        simulationSwitch.isChecked = simulationMode
        simulationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_simulation_mode), isChecked).apply()
            simulationMode = isChecked
            simulationSpeedSeekBar.isEnabled = isChecked
            coverageLimitSeekBar.isEnabled = isChecked
            simulationDelay = if (simulationMode) 1000L / (sharedPreferences.getInt(getString(R.string.app_pref_simulation_speed), 2) + 1) else 200L
            coverageLimit = if (simulationMode) sharedPreferences.getInt(getString(R.string.app_pref_simulation_coverage), 100) else 100

            simulationSpeedSeekBar.progress = (1000.0 / simulationDelay).toInt() - 1
            coverageLimitSeekBar.progress = coverageLimit
        }

        val progressActual: Int = (1000.0 / simulationDelay).toInt()
        simulationSpeedSeekBar.isEnabled = simulationMode
        simulationSpeedLabel.text = getString(R.string.simulation_speed_placeholder_aps, progressActual)
        simulationSpeedSeekBar.progress = progressActual - 1
        simulationSpeedSeekBar.incrementProgressBy(1)
        simulationSpeedSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            var progress = 0

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                this.progress = progress
                simulationDelay = (1000 / (progress + 1)).toLong()
                simulationSpeedLabel.text = getString(R.string.simulation_speed_placeholder_aps, progress + 1)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sharedPreferences.edit().putInt(getString(R.string.app_pref_simulation_speed), this.progress).apply()
            }
        })

        coverageLimitSeekBar.isEnabled = simulationMode
        coverageLimitLabel.text = getString(R.string.simulation_coverage_placeholder, coverageLimit)
        coverageLimitSeekBar.progress = coverageLimit
        coverageLimitSeekBar.incrementProgressBy(5)
        coverageLimitSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                var progressRounded = progress / 5
                progressRounded *= 5
                coverageLimit = progressRounded
                coverageLimitLabel.text = getString(R.string.simulation_coverage_placeholder, coverageLimit)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sharedPreferences.edit().putInt(getString(R.string.app_pref_simulation_coverage), coverageLimit).apply()
            }
        })
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