package ntu.mdp.android.mdptestkotlin

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textview.MaterialTextView
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothActivity
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothService
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothService.Companion.receiver
import ntu.mdp.android.mdptestkotlin.main.ArenaController
import ntu.mdp.android.mdptestkotlin.main.MessageParser
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil


class SettingsActivity : AppCompatActivity() {
    private lateinit var activityUtil: ActivityUtil
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var explorationEditText: EditText
    private lateinit var fastestEditText: EditText
    private lateinit var pauseEditText: EditText
    private lateinit var forwardEditText: EditText
    private lateinit var turnLeftEditText: EditText
    private lateinit var turnRightEditText: EditText
    private lateinit var f1LabelEditText: EditText
    private lateinit var f1CommandEditText: EditText
    private lateinit var f2LabelEditText: EditText
    private lateinit var f2CommandEditText: EditText

    private var onEnter = View.OnKeyListener { view, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
            val id = (view as EditText).id
            val input = view.text.toString().trim()
            if (input.isBlank()) return@OnKeyListener true
            save(id, input)
            view.text.clear()
            view.clearFocus()
            return@OnKeyListener true
        }

        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        activityUtil = ActivityUtil(this)
        activityUtil.setTitle("Settings")
        sharedPreferences = this.applicationContext.getSharedPreferences(getString(R.string.app_pref_key), Context.MODE_PRIVATE)

        explorationEditText = findViewById(R.id.settings_edittext_exploration)
        explorationEditText.setOnKeyListener(onEnter)

        fastestEditText = findViewById(R.id.settings_edittext_fastest)
        fastestEditText.setOnKeyListener(onEnter)

        pauseEditText = findViewById(R.id.settings_edittext_pause)
        pauseEditText.setOnKeyListener(onEnter)

        forwardEditText = findViewById(R.id.settings_edittext_forward)
        forwardEditText.setOnKeyListener(onEnter)

        turnLeftEditText = findViewById(R.id.settings_edittext_turnleft)
        turnLeftEditText.setOnKeyListener(onEnter)

        turnRightEditText = findViewById(R.id.settings_edittext_turnright)
        turnRightEditText.setOnKeyListener(onEnter)

        f1LabelEditText = findViewById(R.id.settings_edittext_f1_label)
        f1LabelEditText.setOnKeyListener(onEnter)

        f1CommandEditText = findViewById(R.id.settings_edittext_f1_command)
        f1CommandEditText.setOnKeyListener(onEnter)

        f2LabelEditText = findViewById(R.id.settings_edittext_f2_label)
        f2LabelEditText.setOnKeyListener(onEnter)

        f2CommandEditText = findViewById(R.id.settings_edittext_f2_command)
        f2CommandEditText.setOnKeyListener(onEnter)

        refreshHints()
    }

    private fun save(id: Int, input: String) {
        when (id) {
            R.id.settings_edittext_exploration -> sharedPreferences.edit().putString(getString(R.string.app_pref_exploration), input).apply()
            R.id.settings_edittext_fastest -> sharedPreferences.edit().putString(getString(R.string.app_pref_fastest), input).apply()
            R.id.settings_edittext_pause -> sharedPreferences.edit().putString(getString(R.string.app_pref_pause), input).apply()
            R.id.settings_edittext_forward -> sharedPreferences.edit().putString(getString(R.string.app_pref_forward), input).apply()
            R.id.settings_edittext_reverse -> sharedPreferences.edit().putString(getString(R.string.app_pref_reverse), input).apply()
            R.id.settings_edittext_turnleft -> sharedPreferences.edit().putString(getString(R.string.app_pref_turn_left), input).apply()
            R.id.settings_edittext_turnright -> sharedPreferences.edit().putString(getString(R.string.app_pref_turn_right), input).apply()
            R.id.settings_edittext_f1_label -> sharedPreferences.edit().putString(getString(R.string.app_pref_label_f1), input).apply()
            R.id.settings_edittext_f1_command -> sharedPreferences.edit().putString(getString(R.string.app_pref_command_f1), input).apply()
            R.id.settings_edittext_f2_label -> sharedPreferences.edit().putString(getString(R.string.app_pref_label_f2), input).apply()
            R.id.settings_edittext_f2_command -> sharedPreferences.edit().putString(getString(R.string.app_pref_command_f2), input).apply()
        }

        refreshHints()
    }

    private fun refreshHints() {
        explorationEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(R.string.settings_default_exploration))
        fastestEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(R.string.settings_default_fastest))
        pauseEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_pause), getString(R.string.settings_default_pause))
        forwardEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_forward), getString(R.string.settings_default_forward))
        turnLeftEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_turn_left), getString(R.string.settings_default_turn_left))
        turnRightEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_turn_right), getString(R.string.settings_default_turn_right))
        f1LabelEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_label_f1), getString(R.string.main_f1))
        f1CommandEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_f1), getString(R.string.main_f2))
        f2LabelEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_label_f2), getString(R.string.main_f1))
        f2CommandEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_f2), getString(R.string.main_f2))
    }
}
