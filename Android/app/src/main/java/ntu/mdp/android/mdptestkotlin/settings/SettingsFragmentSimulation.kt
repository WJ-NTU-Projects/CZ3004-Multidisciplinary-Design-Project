package ntu.mdp.android.mdptestkotlin.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_simulation.*
import ntu.mdp.android.mdptestkotlin.App.Companion.DEFAULT_DELAY
import ntu.mdp.android.mdptestkotlin.App.Companion.coverageLimit
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationDelay
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationMode
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.databinding.FragmentSettingsSimulationBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil

class SettingsFragmentSimulation : Fragment() {

    private var binding: FragmentSettingsSimulationBinding? = null
    private lateinit var activityUtil: ActivityUtil

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingsSimulationBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activityUtil = (activity as SettingsActivity).activityUtil

        simulationSwitch.isChecked = simulationMode
        simulationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_simulation_mode), isChecked).apply()
            simulationMode = isChecked
            simulationSpeedSeekBar.isEnabled = isChecked
            coverageLimitSeekBar.isEnabled = isChecked
            simulationDelay = if (simulationMode) 1000L / (sharedPreferences.getInt(getString(R.string.app_pref_simulation_speed), 2) + 1) else DEFAULT_DELAY
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
        activityUtil.setTitle(getString(R.string.simulation))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
