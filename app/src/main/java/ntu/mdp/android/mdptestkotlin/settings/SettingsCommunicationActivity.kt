package ntu.mdp.android.mdptestkotlin.settings

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings_communication.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.isSimple
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.usingAmd
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.SettingsCommunicationBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil


class SettingsCommunicationActivity : AppCompatActivity() {
    private var enterPressed = false
    private lateinit var binding: SettingsCommunicationBinding
    private lateinit var activityUtil: ActivityUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(App.appTheme)
        super.onCreate(savedInstanceState)
        binding = SettingsCommunicationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityUtil = ActivityUtil(this)
        activityUtil.setTitle(getString(R.string.robot_communication))

        autoSwitch.isChecked = autoUpdateArena
        autoSwitch.isEnabled = !isSimple
        autoSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_auto_update), isChecked).apply()
            autoUpdateArena = isChecked
        }

        amdSwitch.isChecked = usingAmd
        amdSwitch.isEnabled = false
        amdSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_using_amd), isChecked).apply()
            usingAmd = isChecked
        }

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
            R.string.send_arena_default
        ))
        explorationEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(
            R.string.exploration_default
        ))
        fastestPathEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(
            R.string.fastest_path_default
        ))
        pauseEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_pause), getString(
            R.string.pause_default
        ))
        forwardEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_forward), getString(
            R.string.forward_default
        ))
        turnLeftEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_turn_left), getString(
            R.string.turn_left_default
        ))
        turnRightEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_turn_right), getString(
            R.string.turn_right_default
        ))
    }

    @Suppress("UNUSED_PARAMETER")
    fun clickRestore(view: View) {
        activityUtil.sendYesNoDialog(getString(R.string.restore_command_defaultsk)) { positive ->
            if (positive) {
                sharedPreferences.edit().putString(getString(R.string.app_pref_send_arena), getString(
                    R.string.send_arena_default
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_exploration), getString(
                    R.string.exploration_default
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_fastest), getString(
                    R.string.fastest_path_default
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_pause), getString(
                    R.string.pause_default
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_forward), getString(
                    R.string.forward_default
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_reverse), getString(
                    R.string.reverse_default
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_turn_left), getString(
                    R.string.turn_left_default
                )).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_turn_right), getString(
                    R.string.turn_right_default
                )).apply()
                refreshHints()
                App.SEND_ARENA_COMMAND = getString(
                    R.string.send_arena_default
                )
            }
        }
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
            activityUtil.sendYesNoDialog(getString(R.string.unsaved_changes), getString(R.string.save), getString(R.string.discard)) { positive ->
                if (positive) {
                    save(view)
                }

                view.text.clear()
            }
        }

        enterPressed = false
    }
}
