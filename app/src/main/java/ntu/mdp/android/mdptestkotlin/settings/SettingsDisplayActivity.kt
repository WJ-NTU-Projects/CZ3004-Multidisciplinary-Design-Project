package ntu.mdp.android.mdptestkotlin.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings_display.*
import ntu.mdp.android.mdptestkotlin.App
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
        val isDarkMode: Boolean = App.sharedPreferences.getBoolean(getString(R.string.app_pref_dark_mode), false)
        darkModeSwitch.isChecked = isDarkMode

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            App.sharedPreferences.edit().putBoolean(getString(R.string.app_pref_dark_mode), isChecked).apply()
            if (isChecked) App.appTheme = R.style.AppTheme_Dark
            else App.appTheme = R.style.AppTheme
            activityUtil.startActivity(AppStartActivity::class.java, fade = true, startNew = true)
        }

        val isSadMode: Boolean = App.sharedPreferences.getBoolean(getString(R.string.app_pref_sad_mode), false)
        sadSwitch.isChecked = isSadMode
        App.isSimple = isSadMode

        sadSwitch.setOnCheckedChangeListener { _, isChecked ->
            App.sharedPreferences.edit().putBoolean(getString(R.string.app_pref_sad_mode), isChecked).apply()
            App.isSimple = isChecked
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
