package ntu.mdp.android.mdptestkotlin.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_communication_general.*
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.usingAmd
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.databinding.FragmentSettingsCommunicationGeneralBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil

class SettingsCommunicationGeneralFragment : Fragment() {

    private var binding: FragmentSettingsCommunicationGeneralBinding? = null
    private lateinit var activityUtil: ActivityUtil

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingsCommunicationGeneralBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activityUtil = (activity as SettingsCommunicationActivity).activityUtil

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
