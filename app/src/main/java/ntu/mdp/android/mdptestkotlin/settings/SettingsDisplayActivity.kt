package ntu.mdp.android.mdptestkotlin.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings_display.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.coverageLimit
import ntu.mdp.android.mdptestkotlin.App.Companion.darkMode
import ntu.mdp.android.mdptestkotlin.App.Companion.isSimple
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationDelay
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationMode
import ntu.mdp.android.mdptestkotlin.AppStartActivity
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.SettingsDisplayBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil

class SettingsDisplayActivity : AppCompatActivity() {
    private lateinit var binding: SettingsDisplayBinding
    private lateinit var activityUtil: ActivityUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(App.appTheme)
        super.onCreate(savedInstanceState)
        binding = SettingsDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityUtil = ActivityUtil(this)
        activityUtil.setTitle(getString(R.string.display))

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
            if (!simulationMode) simulationDelay = 200L
            if (!simulationMode) coverageLimit = 100
            activityUtil.startActivity(AppStartActivity::class.java, fade = true, startNew = true)
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
