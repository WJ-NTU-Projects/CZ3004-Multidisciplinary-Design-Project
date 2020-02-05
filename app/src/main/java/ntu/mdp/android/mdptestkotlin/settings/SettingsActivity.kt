package ntu.mdp.android.mdptestkotlin.settings

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kotlinx.android.synthetic.main.settings.*
import kotlinx.android.synthetic.main.settings.darkModeSwitch
import kotlinx.android.synthetic.main.settings_display.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.MainActivity
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.SettingsBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsBinding
    private lateinit var activityUtil: ActivityUtil
    private lateinit var drawableList: ArrayList<Drawable>
    private lateinit var labelList: ArrayList<String>
    private lateinit var descriptionList: ArrayList<String>
    private lateinit var colorList: ArrayList<Int>
    private lateinit var activityList: ArrayList<Class<*>>

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(App.appTheme)
        super.onCreate(savedInstanceState)
        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityUtil = ActivityUtil(this)
        activityUtil.setTitle(getString(R.string.settings))

        settingsRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = DeviceAdapter()
        }

        settingsRecycler.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        drawableList = arrayListOf(
            getDrawable(R.drawable.ic_bluetooth_white)!!,
            getDrawable(R.drawable.ic_pi_white)!!,
            getDrawable(R.drawable.ic_f1_white)!!,
            getDrawable(R.drawable.ic_sim)!!
        )

        labelList = arrayListOf(
            getString(R.string.bluetooth),
            getString(R.string.robot_communication),
            getString(R.string.custom_buttons),
            getString(R.string.simulation)
        )

        descriptionList = arrayListOf(
            getString(R.string.bluetooth_description),
            getString(R.string.robot_communication_description),
            getString(R.string.custom_buttons_description),
            "???"
        )

        colorList = arrayListOf(
            getColor(R.color.icon_bg_bluetooth),
            getColor(R.color.icon_bg_commands),
            getColor(R.color.icon_bg_custom_buttons),
            getColor(R.color.icon_bg_simulation)
        )

        activityList = arrayListOf(
            SettingsBluetoothActivity::class.java,
            SettingsCommunicationActivity::class.java,
            SettingsCustomButtonsActivity::class.java,
            SettingsSimulationActivity::class.java
        )

        darkModeSwitch.isChecked = App.darkMode
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            App.sharedPreferences.edit().putBoolean(getString(R.string.app_pref_dark_mode), isChecked).apply()
            App.darkMode = isChecked
            if (isChecked) App.appTheme = R.style.AppTheme_Dark
            else App.appTheme = R.style.AppTheme
            activityUtil.startActivity(MainActivity::class.java, fade = true, startNew = true)
        }
    }

    override fun onResume() {
        super.onResume()
        BluetoothController.callback = { status, message ->
            if (status != BluetoothController.Status.READ) {
                activityUtil.sendSnack(message)
            }
        }
    }

    private inner class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
            init {
                view.setOnClickListener(this)
            }

            val label: MaterialTextView = (view.findViewById(R.id.settings_listitem_label) as MaterialTextView)
            val description: MaterialTextView = (view.findViewById(R.id.settings_listitem_description) as MaterialTextView)
            val icon: ImageView = (view.findViewById(R.id.settings_listitem_icon) as ImageView)
            val cardBackground: CardView = (view.findViewById(R.id.settings_listitem_icon_background) as CardView)
            var pos: Int = -1

            override fun onClick(v: View?) {
                //if (pos == itemCount - 1 && !isSimple) return
                activityUtil.startActivity(activityList[pos])
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val textView = LayoutInflater.from(parent.context).inflate(R.layout.listitem_settings, parent, false)
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.label.text = labelList[position]
            holder.description.text = descriptionList[position]
            holder.icon.setImageDrawable(drawableList[position])
            holder.cardBackground.setCardBackgroundColor(colorList[position])
            holder.pos = position
        }

        override fun getItemCount() = labelList.size
    }
}
