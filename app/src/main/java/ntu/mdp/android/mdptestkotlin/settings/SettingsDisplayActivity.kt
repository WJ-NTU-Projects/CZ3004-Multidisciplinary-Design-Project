package ntu.mdp.android.mdptestkotlin.settings

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.settings_display.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothActivity
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.ActivitySettingsBinding
import ntu.mdp.android.mdptestkotlin.databinding.SettingsDisplayBinding
import ntu.mdp.android.mdptestkotlin.main.MainActivity
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
            activityUtil.startActivity(MainActivity::class.java, fade = true, startNew = true)
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
