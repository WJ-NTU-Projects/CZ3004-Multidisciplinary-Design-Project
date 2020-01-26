package ntu.mdp.android.mdptestkotlin.settings

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings_custom_buttons.*
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.SettingsCustomButtonsBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil


class SettingsCustomButtonsActivity : AppCompatActivity() {
    private var enterPressed = false
    private lateinit var binding: SettingsCustomButtonsBinding
    private lateinit var activityUtil: ActivityUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsCustomButtonsBinding.inflate(layoutInflater)
        setContentView(R.layout.settings_custom_buttons)
        activityUtil = ActivityUtil(this)
        activityUtil.setTitle("Custom Buttons")

        f1LabelEditText.setOnKeyListener(onEnter)
        f1LabelEditText.onFocusChangeListener = onFocusLost

        f1CommandEditText.setOnKeyListener(onEnter)
        f1CommandEditText.onFocusChangeListener = onFocusLost

        f2LabelEditText.setOnKeyListener(onEnter)
        f2LabelEditText.onFocusChangeListener = onFocusLost

        f2CommandEditText.setOnKeyListener(onEnter)
        f2CommandEditText.onFocusChangeListener = onFocusLost
    }

    override fun onResume() {
        super.onResume()
        refreshHints()
        BluetoothController.callback = { status, message ->
            if (status != BluetoothController.Status.READ) {
                activityUtil.sendSnack(message)
            }
        }
    }

    private fun save(view: EditText) {
        val id = view.id
        val input = view.text.toString().trim()
        if (input.isBlank()) return

        when (id) {
            R.id.f1LabelEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_label_f1
            ), input).apply()
            R.id.f1CommandEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_command_f1
            ), input).apply()
            R.id.f2LabelEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_label_f2
            ), input).apply()
            R.id.f2CommandEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_command_f2
            ), input).apply()
        }

        view.text.clear()
        view.clearFocus()
        refreshHints()
    }

    private fun refreshHints() {
        f1LabelEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_label_f1), getString(
            R.string.f1_default
        ))
        f1CommandEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_f1), getString(
            R.string.f1_default
        ))
        f2LabelEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_label_f2), getString(
            R.string.f2_default
        ))
        f2CommandEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_f2), getString(
            R.string.f2_default
        ))
    }

    @Suppress("UNUSED_PARAMETER")
    fun clickRestore(view: View) {
        activityUtil.sendYesNoDialog("Restore defaults?", { positive ->
            if (positive) {
                sharedPreferences.edit().putString(getString(R.string.app_pref_label_f1), getString(
                    R.string.f1_default
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_label_f2), getString(
                    R.string.f2_default
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_command_f1), getString(
                    R.string.f1_default
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_command_f2), getString(
                    R.string.f2_default
                )).apply()
                refreshHints()
            }
        })
    }

    private val onEnter = View.OnKeyListener { view, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
            enterPressed = true
            save(view as EditText)
            return@OnKeyListener true
        }

        false
    }

    private val onFocusLost = View.OnFocusChangeListener { view, hasFocus ->
        if (!hasFocus && !enterPressed && (view as EditText).text.toString().isNotBlank()) {
            activityUtil.sendYesNoDialog("Unsaved changes. Save or discard?", yesLabel = "Save", noLabel = "Discard", callback = { positive ->
                if (positive) save(view)
                view.text.clear()
            })
        }

        enterPressed = false
    }
}
