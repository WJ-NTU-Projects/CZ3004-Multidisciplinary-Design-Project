package ntu.mdp.android.mdptestkotlin

import android.app.Application
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.SharedPreferences
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothClient
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothConnectionManager
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothServer

class App: Application() {

    companion object {
        const val BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        const val ANIMATOR_DURATION = 200L
        const val CLICK_DELAY = 100L
        private const val SLOW_SIM_DELAY = 320L

        @Volatile var appTheme: Int = R.style.AppTheme
        @Volatile var dialogTheme: Int = R.style.DialogTheme
        var socket: BluetoothSocket? = null
        var bluetoothServerThread: BluetoothServer? = null
        var bluetoothClientThread: BluetoothClient? = null
        var bluetoothConnectionManagerThread: BluetoothConnectionManager? = null

        // Persistent Data
        lateinit var sharedPreferences: SharedPreferences
        @Volatile var autoUpdateArena = false
        @Volatile var darkMode = false
        @Volatile var accelerometer = false
        @Volatile var usingAmd = true
        @Volatile var simulationMode = false
        @JvmStatic @Volatile var simulationDelay = SLOW_SIM_DELAY
        @JvmStatic @Volatile var coverageLimit = 100
        @Volatile var SEND_ARENA_COMMAND = "sendArena"
        @Volatile var FORWARD_COMMAND = "f"
        @Volatile var REVERSE_COMMAND = "r"
        @Volatile var TURN_LEFT_COMMAND = "tl"
        @Volatile var TURN_RIGHT_COMMAND = "tr"
        @Volatile var BLUETOOTH_CONNECTED_DEVICE = "-"
        @JvmStatic @Volatile var ROBOT_MOVABLE = true
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = this.getSharedPreferences(getString(R.string.app_pref_key), Context.MODE_PRIVATE)
        SEND_ARENA_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_send_arena), getString(R.string.send_arena_default))!!
        FORWARD_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_forward), getString(R.string.forward_default))!!
        REVERSE_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_reverse), getString(R.string.reverse_default))!!
        TURN_LEFT_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_turn_left), getString(R.string.turn_left_default))!!
        TURN_RIGHT_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_turn_right), getString(R.string.turn_right_default))!!

        darkMode = sharedPreferences.getBoolean(getString(R.string.app_pref_dark_mode), false)

        if (darkMode) {
            appTheme = R.style.AppTheme_Dark
            dialogTheme = R.style.DialogTheme_Dark
        }

        autoUpdateArena = sharedPreferences.getBoolean(getString(R.string.app_pref_auto_update), true)
        usingAmd = sharedPreferences.getBoolean(getString(R.string.app_pref_using_amd), true)
        //allowDiagonalExploration = sharedPreferences.getBoolean(getString(R.string.app_pref_diagonal_exploration), false)

        simulationMode = sharedPreferences.getBoolean(getString(R.string.app_pref_simulation_mode), false)
        accelerometer = sharedPreferences.getBoolean(getString(R.string.app_pref_accelerometer), false)
        simulationDelay = if (simulationMode) 1000L / (sharedPreferences.getInt(getString(R.string.app_pref_simulation_speed), 2) + 1) else (1000 / 3)
        coverageLimit = if (simulationMode) sharedPreferences.getInt(getString(R.string.app_pref_simulation_coverage), 100) else 100
    }
}