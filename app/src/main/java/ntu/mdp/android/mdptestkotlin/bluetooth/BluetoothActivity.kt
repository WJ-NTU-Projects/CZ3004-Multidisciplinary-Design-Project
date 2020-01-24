package ntu.mdp.android.mdptestkotlin.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textview.MaterialTextView
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil

class BluetoothActivity : AppCompatActivity() {

    private lateinit var activityUtil: ActivityUtil
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var bondedLayout: LinearLayout
    private lateinit var bondedRecycler: RecyclerView
    private lateinit var othersRecycler: RecyclerView
    private lateinit var otherDeviceList: ArrayList<BluetoothDevice>
    private lateinit var progressBar: ProgressBar

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action ?: "") {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    progressBar.visibility = View.VISIBLE
                    activityUtil.sendSnack("Scanning for devices...")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    progressBar.visibility = View.INVISIBLE
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
                    if (device.name.isNullOrBlank()) return

                    otherDeviceList.add(device)
                    othersRecycler.adapter = DeviceAdapter(context, otherDeviceList)
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState: Int = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        BluetoothService.init(BluetoothService.State.CLIENT, callback = { status, message ->
                            if (status != BluetoothService.Status.READ) {
                                activityUtil.sendSnack(message)
                            }
                        }, device = device)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)
        activityUtil = ActivityUtil(this)
        activityUtil.setTitle("Connect to Bluetooth Devices")

        swipeLayout = findViewById(R.id.bt_layout_swipe)
        swipeLayout.setOnRefreshListener {
            if (checkBluetoothState()) {
                bondedRecycler.adapter = null
                othersRecycler.adapter = null
                getDevices()
                swipeLayout.isRefreshing = false
            } else {
                activityUtil.sendDialog(getString(R.string.error_bluetooth_off))
            }
        }

        bondedLayout = findViewById(R.id.bt_layout_bonded)
        bondedLayout.visibility = View.GONE
        progressBar = findViewById(R.id.bt_progress)
        progressBar.visibility = View.INVISIBLE

        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(receiver, filter)

        filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(BluetoothService.receiver, filter)
        getDevices()
    }

    override fun onResume() {
        super.onResume()

        if (!checkBluetoothState()) {
            activityUtil.sendDialog(getString(R.string.error_bluetooth_off))
            activityUtil.finishActivity()
            return
        }

        BluetoothService.setCallback {status, message ->
            if (status != BluetoothService.Status.READ) {
                activityUtil.sendSnack(message)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (BluetoothService.isConnected() != true) {
            BluetoothService.reset()
        }

        unregisterReceiver(receiver)
        unregisterReceiver(BluetoothService.receiver)
    }

    private fun getDevices() {
        if (!checkBluetoothState()) return
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices

        if (!pairedDevices.isNullOrEmpty()) {
            bondedLayout.visibility = View.VISIBLE
            bondedRecycler = findViewById<RecyclerView>(R.id.bt_recycler_bonded).apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(this@BluetoothActivity)
                adapter = DeviceAdapter(this@BluetoothActivity, ArrayList(pairedDevices))
            }

            findViewById<MaterialTextView>(R.id.bt_label_others).text = getString(R.string.bt_other_devices)
        }

        bluetoothAdapter.startDiscovery()
        otherDeviceList = ArrayList()
        othersRecycler = findViewById<RecyclerView>(R.id.bt_recycler_others).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@BluetoothActivity)
            adapter = DeviceAdapter(this@BluetoothActivity, otherDeviceList)
        }
    }

    class DeviceAdapter(private val context: Context, private val data: ArrayList<BluetoothDevice>) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
            init {
                view.setOnClickListener(this)
            }

            val nameLabel: MaterialTextView = (view.findViewById(R.id.bt_listitem_name) as MaterialTextView)
            val addressLabel: MaterialTextView = (view.findViewById(R.id.bt_listitem_address) as MaterialTextView)
            var pos: Int = -1

            override fun onClick(v: View?) {
                if (pos >= 0) {
                    BluetoothService.init(BluetoothService.State.CLIENT, callback = { status, message ->
                        if (status != BluetoothService.Status.READ) {
                            ActivityUtil(context).sendSnack(message)
                        }
                    }, device = data[pos])
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val textView = LayoutInflater.from(parent.context).inflate(R.layout.listitem_bluetooth, parent, false)
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val bluetoothDevice: BluetoothDevice = data[position]
            holder.nameLabel.text = bluetoothDevice.name
            holder.addressLabel.text = bluetoothDevice.address
            holder.pos = position
        }

        override fun getItemCount() = data.size
    }

    private fun checkBluetoothState(): Boolean {
        return (BluetoothAdapter.getDefaultAdapter().isEnabled)
    }
}
