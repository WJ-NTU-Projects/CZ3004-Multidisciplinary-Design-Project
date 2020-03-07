package wjayteo.mdp.android.settings

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_identifier.*
import wjayteo.mdp.android.App.Companion.ARDUINO_PREFIX
import wjayteo.mdp.android.App.Companion.COMMAND_DIVIDER
import wjayteo.mdp.android.App.Companion.COMMAND_PREFIX
import wjayteo.mdp.android.App.Companion.DESCRIPTOR_DIVIDER
import wjayteo.mdp.android.App.Companion.EXPLORATION_COMMAND
import wjayteo.mdp.android.App.Companion.FASTEST_PATH_COMMAND
import wjayteo.mdp.android.App.Companion.FORWARD_COMMAND
import wjayteo.mdp.android.App.Companion.GRID_IDENTIFIER
import wjayteo.mdp.android.App.Companion.PC_PREFIX
import wjayteo.mdp.android.App.Companion.REVERSE_COMMAND
import wjayteo.mdp.android.App.Companion.ROBOT_POSITION_IDENTIFIER
import wjayteo.mdp.android.App.Companion.SEND_ARENA_COMMAND
import wjayteo.mdp.android.App.Companion.SET_IMAGE_IDENTIFIER
import wjayteo.mdp.android.App.Companion.TURN_LEFT_COMMAND
import wjayteo.mdp.android.App.Companion.TURN_RIGHT_COMMAND
import wjayteo.mdp.android.App.Companion.WAYPOINT_COMMAND
import wjayteo.mdp.android.App.Companion.sharedPreferences
import wjayteo.mdp.android.R
import wjayteo.mdp.android.databinding.FragmentSettingsIdentifierBinding
import wjayteo.mdp.android.utils.ActivityUtil

class SettingsFragmentIdentifier : Fragment() {

    private var binding: FragmentSettingsIdentifierBinding? = null
    private lateinit var activityUtil: ActivityUtil
    private lateinit var viewOnHold: EditText
    private var enterPressed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingsIdentifierBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activityUtil = (activity as SettingsActivity).activityUtil

        sendArenaEditText.setOnKeyListener(onEnter)
        sendArenaEditText.onFocusChangeListener = onFocusLost

        explorationEditText.setOnKeyListener(onEnter)
        explorationEditText.onFocusChangeListener = onFocusLost

        fastestPathEditText.setOnKeyListener(onEnter)
        fastestPathEditText.onFocusChangeListener = onFocusLost

        //pauseEditText.setOnKeyListener(onEnter)
        //pauseEditText.onFocusChangeListener = onFocusLost

        forwardEditText.setOnKeyListener(onEnter)
        forwardEditText.onFocusChangeListener = onFocusLost

        reverseEditText.setOnKeyListener(onEnter)
        reverseEditText.onFocusChangeListener = onFocusLost

        turnLeftEditText.setOnKeyListener(onEnter)
        turnLeftEditText.onFocusChangeListener = onFocusLost

        turnRightEditText.setOnKeyListener(onEnter)
        turnRightEditText.onFocusChangeListener = onFocusLost

        commandPrefixEditText.setOnKeyListener(onEnter)
        commandPrefixEditText.onFocusChangeListener = onFocusLost

        stringDividerEditText.setOnKeyListener(onEnter)
        stringDividerEditText.onFocusChangeListener = onFocusLost

        descriptorDividerEditText.setOnKeyListener(onEnter)
        descriptorDividerEditText.onFocusChangeListener = onFocusLost

        gridDescriptorEditText.setOnKeyListener(onEnter)
        gridDescriptorEditText.onFocusChangeListener = onFocusLost

        setImageEditText.setOnKeyListener(onEnter)
        setImageEditText.onFocusChangeListener = onFocusLost

        robotPositionEditText.setOnKeyListener(onEnter)
        robotPositionEditText.onFocusChangeListener = onFocusLost

        //robotStatusEditText.setOnKeyListener(onEnter)
        //robotStatusEditText.onFocusChangeListener = onFocusLost

        //startPointEditText.setOnKeyListener(onEnter)
        //startPointEditText.onFocusChangeListener = onFocusLost

        //goalPointEditText.setOnKeyListener(onEnter)
        //goalPointEditText.onFocusChangeListener = onFocusLost

        waypointEditText.setOnKeyListener(onEnter)
        waypointEditText.onFocusChangeListener = onFocusLost

        arduinoPrefixEditText.setOnKeyListener(onEnter)
        arduinoPrefixEditText.onFocusChangeListener = onFocusLost

        pcPrefixEditText.setOnKeyListener(onEnter)
        pcPrefixEditText.onFocusChangeListener = onFocusLost
    }

    override fun onResume() {
        super.onResume()
        refreshHints()
        activityUtil.setTitle(getString(R.string.identifiers))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun save(view: EditText) {
        val id = view.id
        val input = view.text.toString().trim()
        if (input.isBlank()) return

        when (id) {
            //R.id.pauseEditText -> sharedPreferences.edit().putString(getString(R.string.app_pref_pause), input).apply()

            R.id.explorationEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_exploration), input).apply()
                EXPLORATION_COMMAND = input
            }

            R.id.fastestPathEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_fastest), input).apply()
                FASTEST_PATH_COMMAND = input
            }

            R.id.arduinoPrefixEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_arduino_prefix), input).apply()
                ARDUINO_PREFIX = input
            }

            R.id.pcPrefixEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_pc_prefix), input).apply()
                PC_PREFIX = input
            }

//            R.id.startPointEditText -> {
//                sharedPreferences.edit().putString(getString(R.string.app_pref_command_start_point), input).apply()
//                START_POINT_COMMAND = input
//            }
//
//            R.id.goalPointEditText -> {
//                sharedPreferences.edit().putString(getString(R.string.app_pref_command_goal_point), input).apply()
//                GOAL_POINT_COMMAND = input
//            }

            R.id.waypointEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_command_waypoint), input).apply()
                WAYPOINT_COMMAND = input
            }

            R.id.forwardEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_forward), input).apply()
                FORWARD_COMMAND = input
            }

            R.id.reverseEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_reverse), input).apply()
                REVERSE_COMMAND = input
            }

            R.id.turnLeftEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_turn_left), input).apply()
                TURN_LEFT_COMMAND = input
            }

            R.id.turnRightEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_turn_right), input).apply()
                TURN_RIGHT_COMMAND = input
            }

            R.id.sendArenaEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_send_arena), input).apply()
                SEND_ARENA_COMMAND = input
            }

            R.id.commandPrefixEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_command_prefix), input).apply()
                COMMAND_PREFIX = input
            }

            R.id.stringDividerEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_command_divider), input).apply()
                COMMAND_DIVIDER = input
            }

            R.id.descriptorDividerEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_descriptor_divider), input).apply()
                DESCRIPTOR_DIVIDER = input
            }

            R.id.gridDescriptorEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_grid_identifier), input).apply()
                GRID_IDENTIFIER = input
            }

            R.id.setImageEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_set_image_identifier), input).apply()
                SET_IMAGE_IDENTIFIER = input
            }

            R.id.robotPositionEditText -> {
                sharedPreferences.edit().putString(getString(R.string.app_pref_robot_position_identifier), input).apply()
                ROBOT_POSITION_IDENTIFIER = input
            }

//            R.id.robotStatusEditText -> {
//                sharedPreferences.edit().putString(getString(R.string.app_pref_robot_status_identifier), input).apply()
//                ROBOT_STATUS_IDENTIFIER = input
//            }
        }

        view.text.clear()
        view.clearFocus()
        refreshHints()
    }

    fun refreshHints() {
        sendArenaEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_send_arena), getString(R.string.send_arena_default))
        explorationEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(R.string.exploration_default))
        fastestPathEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(R.string.fastest_path_default))
        //pauseEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_pause), getString(R.string.pause_default))
        forwardEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_forward), getString(R.string.forward_default))
        turnLeftEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_turn_left), getString(R.string.turn_left_default))
        turnRightEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_turn_right), getString(R.string.turn_right_default))
        commandPrefixEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_prefix), getString(R.string.command_prefix_default))
        stringDividerEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_divider), getString(R.string.string_divider_default))
        descriptorDividerEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_descriptor_divider), getString(R.string.descriptor_divider_default))
        gridDescriptorEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_grid_identifier), getString(R.string.grid_descriptor_default))
        setImageEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_set_image_identifier), getString(R.string.set_image_default))
        robotPositionEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_robot_position_identifier), getString(R.string.robot_position_default))
        //robotStatusEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_robot_status_identifier), getString(R.string.robot_status_default))
        //startPointEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_start_point), getString(R.string.start_point_default))
        //goalPointEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_goal_point), getString(R.string.goal_point_default))
        waypointEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_waypoint), getString(R.string.waypoint_default))
        arduinoPrefixEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_arduino_prefix), getString(R.string.arduino_prefix_default))
        pcPrefixEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_pc_prefix), getString(R.string.pc_prefix_default))
    }

    fun saveOnHold(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK && ::viewOnHold.isInitialized) save(viewOnHold)
        viewOnHold.text.clear()
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
}
