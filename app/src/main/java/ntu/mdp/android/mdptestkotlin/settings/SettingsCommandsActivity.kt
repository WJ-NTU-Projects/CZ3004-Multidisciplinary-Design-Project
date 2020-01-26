package ntu.mdp.android.mdptestkotlin.settings

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings_commands.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.ActivityBluetoothBinding
import ntu.mdp.android.mdptestkotlin.databinding.SettingsCommandsBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil


class SettingsCommandsActivity : AppCompatActivity() {
    private var enterPressed = false
    private lateinit var binding: SettingsCommandsBinding
    private lateinit var activityUtil: ActivityUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsCommandsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityUtil = ActivityUtil(this)
        activityUtil.setTitle("String Commands")

        sendArenaEditText.setOnKeyListener(onEnter)
        sendArenaEditText.onFocusChangeListener = onFocusLost

        explorationEditText.setOnKeyListener(onEnter)
        explorationEditText.onFocusChangeListener = onFocusLost

        fastestPathEditText.setOnKeyListener(onEnter)
        fastestPathEditText.onFocusChangeListener = onFocusLost

        pauseEditText.setOnKeyListener(onEnter)
        pauseEditText.onFocusChangeListener = onFocusLost

        forwardEditText.setOnKeyListener(onEnter)
        forwardEditText.onFocusChangeListener = onFocusLost

        turnLeftEditText.setOnKeyListener(onEnter)
        turnLeftEditText.onFocusChangeListener = onFocusLost

        turnRightEditText.setOnKeyListener(onEnter)
        turnRightEditText.onFocusChangeListener = onFocusLost
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
            R.id.sendArenaEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_send_arena), input).apply()
                App.SEND_ARENA_COMMAND = input
            }

            R.id.explorationEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_exploration
            ), input).apply()
            R.id.fastestPathEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_fastest
            ), input).apply()
            R.id.pauseEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_pause
            ), input).apply()
            R.id.forwardEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_forward
            ), input).apply()
            R.id.reverseEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_reverse
            ), input).apply()
            R.id.turnLeftEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_turn_left
            ), input).apply()
            R.id.turnRightEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_turn_right
            ), input).apply()
        }

        view.text.clear()
        view.clearFocus()
        refreshHints()
    }

    private fun refreshHints() {
        sendArenaEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_send_arena), getString(
            R.string.settings_default_send_arena
        ))
        explorationEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(
            R.string.settings_default_exploration
        ))
        fastestPathEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(
            R.string.settings_default_fastest
        ))
        pauseEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_pause), getString(
            R.string.settings_default_pause
        ))
        forwardEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_forward), getString(
            R.string.settings_default_forward
        ))
        turnLeftEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_turn_left), getString(
            R.string.settings_default_turn_left
        ))
        turnRightEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_turn_right), getString(
            R.string.settings_default_turn_right
        ))
    }

    @Suppress("UNUSED_PARAMETER")
    fun clickRestore(view: View) {
        activityUtil.sendYesNoDialog("Restore defaults?", { positive ->
            if (positive) {
                sharedPreferences.edit().putString(getString(R.string.app_pref_send_arena), getString(
                    R.string.settings_default_send_arena
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_exploration), getString(
                    R.string.settings_default_exploration
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_fastest), getString(
                    R.string.settings_default_fastest
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_pause), getString(
                    R.string.settings_default_pause
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_forward), getString(
                    R.string.settings_default_forward
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_reverse), getString(
                    R.string.settings_default_reverse
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_turn_left), getString(
                    R.string.settings_default_turn_left
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_turn_right), getString(
                    R.string.settings_default_turn_right
                )).apply()
                refreshHints()
                App.SEND_ARENA_COMMAND = getString(
                    R.string.settings_default_send_arena
                )
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
                if (positive) {
                    save(view)
                }

                view.text.clear()
            })
        }

        enterPressed = false
    }
}
