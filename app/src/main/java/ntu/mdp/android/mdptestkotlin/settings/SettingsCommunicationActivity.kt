package ntu.mdp.android.mdptestkotlin.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings_communication.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.FORWARD_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.REVERSE_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.TURN_LEFT_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.TURN_RIGHT_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
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
    private lateinit var viewOnHold: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(App.appTheme)
        super.onCreate(savedInstanceState)
        binding = SettingsCommunicationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityUtil = ActivityUtil(this)
        activityUtil.setTitle(getString(R.string.robot_communication))

        autoSwitch.isChecked = autoUpdateArena
        autoSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_auto_update), isChecked).apply()
            autoUpdateArena = isChecked
        }

        amdSwitch.isChecked = usingAmd
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

            R.id.f1LabelEditText -> sharedPreferences.edit().putString(getString(R.string.app_pref_label_f1), input).apply()
            R.id.f1CommandEditText -> sharedPreferences.edit().putString(getString(R.string.app_pref_command_f1), input).apply()
            R.id.f2LabelEditText -> sharedPreferences.edit().putString(getString(R.string.app_pref_label_f2), input).apply()
            R.id.f2CommandEditText -> sharedPreferences.edit().putString(getString(R.string.app_pref_command_f2), input).apply()
            R.id.explorationEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_exploration
            ), input).apply()
            R.id.fastestPathEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_fastest
            ), input).apply()
            R.id.pauseEditText -> sharedPreferences.edit().putString(getString(
                R.string.app_pref_pause
            ), input).apply()

            R.id.forwardEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_forward), input).apply()
                FORWARD_COMMAND = input
            }

            R.id.reverseEditText -> {
                sharedPreferences.edit().putString(getString(
                    R.string.app_pref_reverse
                ), input).apply()
                REVERSE_COMMAND = input
            }

            R.id.turnLeftEditText -> {
                sharedPreferences.edit().putString(getString(
                    R.string.app_pref_turn_left
                ), input).apply()
                TURN_LEFT_COMMAND = input
            }

            R.id.turnRightEditText -> {
                sharedPreferences.edit().putString(getString(
                    R.string.app_pref_turn_right
                ), input).apply()
                TURN_RIGHT_COMMAND = input
            }

            R.id.sendArenaEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_send_arena), input).apply()
                SEND_ARENA_COMMAND = input
            }
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
        f1LabelEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_label_f1), getString(R.string.f1_default))
        f1CommandEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_f1), getString(R.string.f1_default))
        f2LabelEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_label_f2), getString(R.string.f2_default))
        f2CommandEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_f2), getString(R.string.f2_default))
    }

    @Suppress("UNUSED_PARAMETER")
    fun clickRestore(view: View) {
        when (view.id) {
            R.id.restoreCustom -> {
                activityUtil.sendYesNoDialog(10000, getString(R.string.restore_command_defaultsk))
            }

            R.id.restoreCommands -> {
                activityUtil.sendYesNoDialog(10001, getString(R.string.restore_command_defaultsk))
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
            viewOnHold = view
            activityUtil.sendYesNoDialog(11000, getString(R.string.unsaved_changes), leftLabel = getString(R.string.save), rightLabel = getString(R.string.discard))
        }

        enterPressed = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            10000 -> {
                if (resultCode == Activity.RESULT_CANCELED) return
                sharedPreferences.edit().putString(getString(R.string.app_pref_label_f1), getString(R.string.f1_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_label_f2), getString(R.string.f2_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_command_f1), getString(R.string.f1_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_command_f2), getString(R.string.f2_default)).apply()
                refreshHints()
            }

            10001 -> {
                if (resultCode == Activity.RESULT_CANCELED) return
                sharedPreferences.edit().putString(getString(R.string.app_pref_send_arena), getString(R.string.send_arena_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_exploration), getString(R.string.exploration_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_fastest), getString(R.string.fastest_path_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_pause), getString(R.string.pause_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_forward), getString(R.string.forward_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_reverse), getString(R.string.reverse_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_turn_left), getString(R.string.turn_left_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_turn_right), getString(R.string.turn_right_default)).apply()

                SEND_ARENA_COMMAND = getString(R.string.send_arena_default)
                FORWARD_COMMAND = getString(R.string.forward_default)
                REVERSE_COMMAND = getString(R.string.reverse_default)
                TURN_LEFT_COMMAND = getString(R.string.turn_left_default)
                TURN_RIGHT_COMMAND = getString(R.string.turn_right_default)
                refreshHints()
            }

            11000 -> {
                if (resultCode == Activity.RESULT_OK && ::viewOnHold.isInitialized) save(viewOnHold)
                viewOnHold.text.clear()
            }
        }
    }
}
