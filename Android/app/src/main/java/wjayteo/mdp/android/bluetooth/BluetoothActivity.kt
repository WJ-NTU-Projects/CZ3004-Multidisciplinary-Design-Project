package wjayteo.mdp.android.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kotlinx.android.synthetic.main.activity_bluetooth.*
import wjayteo.mdp.android.App
import wjayteo.mdp.android.R
import wjayteo.mdp.android.databinding.ActivityBluetoothBinding
import wjayteo.mdp.android.utils.ActivityUtil
import java.util.*
import kotlin.collections.ArrayList

class BluetoothActivity : AppCompatActivity() {

    companion object {
        const val DISCOVERABILITY_REQUEST: Int = 1001
        const val BLUETOOTH_DISABLED_CODE: Int = 10000
    }

    private lateinit var binding: ActivityBluetoothBinding
    private lateinit var activityUtil: ActivityUtil
    private lateinit var otherDeviceList: ArrayList<BluetoothDevice>
    private lateinit var othersAdapter: DeviceAdapter

    override fun attachBaseContext(newBase: Context?) {
        val res: Resources? = newBase?.resources
        val configuration: Configuration? = res?.configuration
        val newLocale = Locale(App.APP_LANGUAGE)
        configuration?.setLocale(newLocale)
        val localeList = LocaleList(newLocale)
        LocaleList.setDefault(localeList)
        configuration?.setLocales(localeList)

        if (configuration != null) {
            val context = newBase.createConfigurationContext(configuration)
            super.attachBaseContext(ContextWrapper(context))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        super.applyOverrideConfiguration(baseContext.resources.configuration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(App.APP_THEME)
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityUtil = ActivityUtil(this)
        activityUtil.setTitle(getString(R.string.bluetooth))

        bluetoothSwipeRefreshLayout.setOnRefreshListener {
            refresh()
        }

        bluetoothBondedCard.visibility = View.GONE
        bluetoothProgressBar.visibility = View.INVISIBLE
        bluetoothDiscoverySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (BluetoothAdapter.getDefaultAdapter().scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30)
                    startActivityForResult(discoverableIntent, DISCOVERABILITY_REQUEST)
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(receiver, filter)

        bluetoothBondedRecycler.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        bluetoothOthersRecycler.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
    }

    override fun onResume() {
        super.onResume()

        if (!checkBluetoothState()) {
            activityUtil.sendDialog(BLUETOOTH_DISABLED_CODE, getString(R.string.error_bluetooth_off))
            activityUtil.finishActivity()
            return
        }

        BluetoothController.callback = callback
        bluetoothMyNameLabel.text = getString(R.string.device_name, BluetoothAdapter.getDefaultAdapter().name)
        refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    @Suppress("UNUSED_PARAMETER")
    fun clickDisconnect(view: View) {
        BluetoothController.disconnect()
    }

    private fun getDevices() {
        if (!checkBluetoothState()) return
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices

        if (!pairedDevices.isNullOrEmpty()) {
            bluetoothBondedCard.visibility = View.VISIBLE
            bluetoothBondedRecycler.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(this@BluetoothActivity)
                adapter = DeviceAdapter(ArrayList(pairedDevices))
            }

            bluetoothOthersLabel.text = getString(R.string.other_devices)
        }

        otherDeviceList = ArrayList()
        othersAdapter = DeviceAdapter(ArrayList(otherDeviceList))
        bluetoothOthersRecycler.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(this@BluetoothActivity)
            adapter = othersAdapter
        }

        bluetoothAdapter.cancelDiscovery()
        bluetoothAdapter.startDiscovery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == DISCOVERABILITY_REQUEST) {
            if(resultCode == Activity.RESULT_OK) {
                bluetoothDiscoverySwitch.isChecked = true
            }
            else if(resultCode == Activity.RESULT_CANCELED) {
                bluetoothDiscoverySwitch.isChecked = false
            }

            return
        }

        if (requestCode == BLUETOOTH_DISABLED_CODE) {
            activityUtil.finishActivity()
            return
        }
    }

    private fun refresh() {
        if (checkBluetoothState()) {
            bluetoothBondedRecycler.adapter = null
            bluetoothOthersRecycler.adapter = null
            bluetoothDisconnectButton.isEnabled = false
            bluetoothNameLabel.text = "-"
            getDevices()
            bluetoothSwipeRefreshLayout.isRefreshing = false
        }
    }

    private fun checkBluetoothState(): Boolean {
        return (BluetoothAdapter.getDefaultAdapter().isEnabled)
    }

    private val callback: (status: BluetoothController.Status, message: String) -> Unit = { status, message ->
        when (status) {
            BluetoothController.Status.CONNECTED -> {
                activityUtil.sendSnack(message)
                refresh()
                onBackPressed()
            }

            BluetoothController.Status.DISCONNECTED -> {
                activityUtil.sendSnack(message)
                refresh()
            }

            BluetoothController.Status.READ -> {}
            BluetoothController.Status.WRITE_SUCCESS -> Log.d(this::class.simpleName ?: "-", message)
            else -> activityUtil.sendSnack(message)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action ?: "") {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    bluetoothProgressBar.visibility = View.VISIBLE
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    bluetoothProgressBar.visibility = View.INVISIBLE
                }

                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE) != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                        bluetoothDiscoverySwitch.isChecked = false
                    }
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return

                    if (!device.name.isNullOrBlank()) {
                        othersAdapter.addItem(device)
                    }
                }

//                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
//                    val bondState: Int = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
//                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
//                    if (bondState == BluetoothDevice.BOND_BONDED) {
//                        BluetoothController.startClient(device, callback)
//                    }
//                }
            }
        }
    }

    private inner class DeviceAdapter(private val dataList: ArrayList<BluetoothDevice>) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
            init {
                view.setOnClickListener(this)
            }

            val nameLabel: MaterialTextView = (view.findViewById(R.id.bt_listitem_name) as MaterialTextView)
            val connectedLabel: MaterialTextView = (view.findViewById(R.id.bt_listitem_connected) as MaterialTextView)
            var pos: Int = -1

            override fun onClick(v: View?) {
                if (pos >= 0) {
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    BluetoothController.startClient(dataList[pos], callback)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val textView = LayoutInflater.from(parent.context).inflate(R.layout.listitem_bluetooth, parent, false)
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val bluetoothDevice: BluetoothDevice = dataList[position]
            holder.pos = position
            holder.nameLabel.text = bluetoothDevice.name

            if (App.socket?.isConnected == true && bluetoothDevice.address == App.socket?.remoteDevice?.address) {
                holder.connectedLabel.text = getString(R.string.connected)
                bluetoothNameLabel.text = bluetoothDevice.name
                bluetoothDisconnectButton.isEnabled = true
            } else {
                holder.connectedLabel.text = ""
            }
        }

        override fun getItemCount() = dataList.size

        fun addItem(device: BluetoothDevice) {
            dataList.add(device)
            notifyItemInserted(itemCount - 1)
        }
    }
}
