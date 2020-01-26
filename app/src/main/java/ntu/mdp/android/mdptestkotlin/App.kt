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
        var SEND_ARENA_COMMAND = "sendArena"
        var BLUETOOTH_CONNECTED_DEVICE = "-"
        const val ROBOT_FOOTPRINT = 3
        const val ANIMATOR_DURATION = 200L
        const val BUTTON_CLICK_DELAY_INTERVAL = 500

        var appTheme: Int = R.style.AppTheme
        var socket: BluetoothSocket? = null
        var bluetoothServerThread: BluetoothServer? = null
        var bluetoothClientThread: BluetoothClient? = null
        var bluetoothConnectionManagerThread: BluetoothConnectionManager? = null

        // Persistent Data
        lateinit var sharedPreferences: SharedPreferences
        var autoUpdateArena = false
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = this.getSharedPreferences(getString(R.string.app_pref_key), Context.MODE_PRIVATE)
        SEND_ARENA_COMMAND = sharedPreferences.getString(getString(R.string.app_pref_send_arena), getString(R.string.send_arena_default))!!
        val darkMode: Boolean = sharedPreferences.getBoolean(getString(R.string.app_pref_dark_mode), false)
        if (darkMode) appTheme = R.style.AppTheme_Dark
    }
}