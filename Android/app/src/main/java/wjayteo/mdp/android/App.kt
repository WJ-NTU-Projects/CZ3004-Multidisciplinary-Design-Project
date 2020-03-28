package wjayteo.mdp.android

import android.app.Application
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.SharedPreferences
import wjayteo.mdp.android.bluetooth.BluetoothClient
import wjayteo.mdp.android.bluetooth.BluetoothConnectionManager
import wjayteo.mdp.android.bluetooth.BluetoothServer

class App: Application() {

    companion object {
        const val BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        const val ANIMATOR_DURATION = 200L
        const val CLICK_DELAY = 100L
        const val DEFAULT_DELAY = 250L

        // Bluetooth Sockets & Threads
        var socket: BluetoothSocket? = null
        var bluetoothServerThread: BluetoothServer? = null
        var bluetoothClientThread: BluetoothClient? = null
        var bluetoothConnectionManagerThread: BluetoothConnectionManager? = null

        // Persistent Data
        lateinit var sharedPreferences: SharedPreferences
        @Volatile var APP_LANGUAGE: String = "en"
        @Volatile var APP_THEME: Int = R.style.AppTheme
        @Volatile var DIALOG_THEME: Int = R.style.DialogTheme
        @Volatile var AUTO_UPDATE_ARENA = false
        @Volatile var DARK_MODE = false
        @Volatile var USING_AMD = false
        @Volatile var SIM_MODE = false
        @Volatile var BLUETOOTH_CONNECTED_DEVICE = "-"
        @Volatile var LAST_CONNECTED_DEVICE = "FF:FF:FF:FF:FF:FF" //"20:16:B9:85:72:07"
        @Volatile var IS_TABLET = false

        // COMMANDS
        @Volatile var EXPLORATION_COMMAND = "exs"
        @Volatile var FASTEST_PATH_COMMAND = "beginFastest"
        @Volatile var SEND_ARENA_COMMAND = "sendArena"
        @Volatile var FORWARD_COMMAND = "M"
        @Volatile var REVERSE_COMMAND = "V"
        @Volatile var TURN_LEFT_COMMAND = "L"
        @Volatile var TURN_RIGHT_COMMAND = "R"
        @Volatile var START_POINT_COMMAND = "startpoint"
        @Volatile var WAYPOINT_COMMAND = "waypoint"
        @Volatile var GOAL_POINT_COMMAND = "goalpoint"

        @Volatile var ARDUINO_PREFIX = "A"
        @Volatile var PC_PREFIX = "P"
        @Volatile var COMMAND_PREFIX = "#"
        @Volatile var COMMAND_DIVIDER = ":"
        @Volatile var GRID_IDENTIFIER = "r"
        @Volatile var DESCRIPTOR_DIVIDER = "/"
        @Volatile var SET_IMAGE_IDENTIFIER = "im"
        @Volatile var ROBOT_POSITION_IDENTIFIER = "robotPosition"
        @Volatile var ROBOT_STATUS_IDENTIFIER = "robotStatus"

        // Java Compatible
        @JvmStatic @Volatile var ACCELEROMETER = false
        @JvmStatic @Volatile var PAD_MOVABLE = true
        @JvmStatic @Volatile var TILT_MOVABLE = true
        @JvmStatic @Volatile var SIM_DELAY = 250L
        @JvmStatic @Volatile var COVERAGE_LIMIT = 100
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = this.getSharedPreferences(getString(R.string.app_pref_key), Context.MODE_PRIVATE)


        // LEADERBOARD OVERWRITES
        sharedPreferences.edit().putBoolean(getString(R.string.app_pref_auto_update), true).apply()
        sharedPreferences.edit().putBoolean(getString(R.string.app_pref_using_amd), false).apply()
        sharedPreferences.edit().putBoolean(getString(R.string.app_pref_simulation_mode), false).apply()

        EXPLORATION_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_exploration), getString(R.string.exploration_default))!!
        FASTEST_PATH_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_fastest), getString(R.string.fastest_path_default))!!
        SEND_ARENA_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_send_arena), getString(R.string.send_arena_default))!!
        FORWARD_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_forward), getString(R.string.forward_default))!!
        REVERSE_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_reverse), getString(R.string.reverse_default))!!
        TURN_LEFT_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_turn_left), getString(R.string.turn_left_default))!!
        TURN_RIGHT_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_turn_right), getString(R.string.turn_right_default))!!
        START_POINT_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_command_start_point), getString(R.string.start_point_default))!!
        GOAL_POINT_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_command_goal_point), getString(R.string.goal_point_default))!!
        WAYPOINT_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_command_waypoint), getString(R.string.waypoint_default))!!
        ARDUINO_PREFIX = sharedPreferences.getString(getString(R.string.app_pref_arduino_prefix), getString(R.string.arduino_prefix_default))!!
        PC_PREFIX = sharedPreferences.getString(getString(R.string.app_pref_pc_prefix), getString(R.string.pc_prefix_default))!!

        COMMAND_PREFIX = sharedPreferences.getString(getString(R.string.app_pref_command_prefix), getString(R.string.command_prefix_default))!!
        COMMAND_DIVIDER = sharedPreferences.getString(getString(R.string.app_pref_command_divider), getString(R.string.string_divider_default))!!
        GRID_IDENTIFIER = sharedPreferences.getString(getString(R.string.app_pref_grid_identifier), getString(R.string.grid_identifier_default))!!
        DESCRIPTOR_DIVIDER = sharedPreferences.getString(getString(R.string.app_pref_descriptor_divider), getString(R.string.descriptor_divider_default))!!
        SET_IMAGE_IDENTIFIER = sharedPreferences.getString(getString(R.string.app_pref_set_image_identifier), getString(R.string.set_image_default))!!
        ROBOT_POSITION_IDENTIFIER = sharedPreferences.getString(getString(R.string.app_pref_robot_position_identifier), getString(R.string.robot_position_default))!!
        ROBOT_STATUS_IDENTIFIER = sharedPreferences.getString(getString(R.string.app_pref_robot_status_identifier), getString(R.string.robot_status_default))!!

        DARK_MODE = sharedPreferences.getBoolean(getString(R.string.app_pref_dark_mode), false)

        if (DARK_MODE) {
            APP_THEME = R.style.AppTheme_Dark
            DIALOG_THEME = R.style.DialogTheme_Dark
        }

        AUTO_UPDATE_ARENA = sharedPreferences.getBoolean(getString(R.string.app_pref_auto_update), true)
        USING_AMD = sharedPreferences.getBoolean(getString(R.string.app_pref_using_amd), false)
        SIM_MODE = sharedPreferences.getBoolean(getString(R.string.app_pref_simulation_mode), false)
        SIM_DELAY = if (SIM_MODE) 1000L / (sharedPreferences.getInt(getString(R.string.app_pref_simulation_speed), 2) + 1) else DEFAULT_DELAY
        COVERAGE_LIMIT = if (SIM_MODE) sharedPreferences.getInt(getString(R.string.app_pref_simulation_coverage), 100) else 100
        APP_LANGUAGE = sharedPreferences.getString(getString(R.string.app_pref_language), getString(R.string.language_default)) ?: getString(R.string.language_default)
    }
}