package ntu.mdp.android.mdptestkotlin.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.settings_communication.*
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.COMMAND_DIVIDER
import ntu.mdp.android.mdptestkotlin.App.Companion.COMMAND_PREFIX
import ntu.mdp.android.mdptestkotlin.App.Companion.DESCRIPTOR_DIVIDER
import ntu.mdp.android.mdptestkotlin.App.Companion.FORWARD_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.GRID_IDENTIFIER
import ntu.mdp.android.mdptestkotlin.App.Companion.REVERSE_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.ROBOT_POSITION_IDENTIFIER
import ntu.mdp.android.mdptestkotlin.App.Companion.ROBOT_STATUS_IDENTIFIER
import ntu.mdp.android.mdptestkotlin.App.Companion.SEND_ARENA_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.SET_IMAGE_IDENTIFIER
import ntu.mdp.android.mdptestkotlin.App.Companion.TURN_LEFT_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.TURN_RIGHT_COMMAND
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import ntu.mdp.android.mdptestkotlin.databinding.SettingsCommunicationBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil

class SettingsCommunicationActivity : AppCompatActivity() {
    private lateinit var binding: SettingsCommunicationBinding
    lateinit var activityUtil: ActivityUtil
    private lateinit var mainTab: TabLayout.Tab
    private lateinit var generalFragment: SettingsCommunicationGeneralFragment
    private lateinit var identifierFragment: SettingsCommunicationIdentifierFragment
    private lateinit var customFragment: SettingsCommunicationCustomFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(App.appTheme)
        super.onCreate(savedInstanceState)
        binding = SettingsCommunicationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityUtil = ActivityUtil(this)
        activityUtil.setTitle(getString(R.string.robot_communication))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        mainTab = tabLayout.newTab().apply {
            text = getString(R.string.arena)
        }

        tabLayout.addTab(mainTab)

        tabLayout.addTab(tabLayout.newTab().apply {
            text = getString(R.string.identifiers)
        })

        tabLayout.addTab(tabLayout.newTab().apply {
            text = getString(R.string.custom_buttons)
        })

        val viewPagerAdapter = ViewPagerAdapter()
        viewPager.adapter = viewPagerAdapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null)  {
                    viewPager.currentItem = tab.position
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        BluetoothController.callback = { status, message ->
            if (status != BluetoothController.Status.READ) {
                activityUtil.sendSnack(message)
            }
        }
    }

    @Suppress("unused")
    fun clickUiButton(view: View) {
        when (view.id) {
            R.id.restoreCustom -> {
                activityUtil.sendYesNoDialog(10000, getString(R.string.restore_command_defaults))
            }

            R.id.restoreStringCommon -> {
                activityUtil.sendYesNoDialog(10001, getString(R.string.restore_command_defaults))
            }

            R.id.restoreStringCommands -> {
                activityUtil.sendYesNoDialog(10002, getString(R.string.restore_command_defaults))
            }

            R.id.restoreStringInfo -> {
                activityUtil.sendYesNoDialog(10003, getString(R.string.restore_command_defaults))
            }
        }
    }

    private inner class ViewPagerAdapter: FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            when (position) {
                2 -> {
                    customFragment = SettingsCommunicationCustomFragment()
                    return customFragment
                }

                1 -> {
                    identifierFragment = SettingsCommunicationIdentifierFragment()
                    return identifierFragment
                }

                else -> {
                    generalFragment = SettingsCommunicationGeneralFragment()
                    return generalFragment
                }
            }
        }

        override fun getCount(): Int = tabLayout.tabCount
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
                customFragment.refreshHints()
            }

            10001 -> {
                if (resultCode == Activity.RESULT_CANCELED) return
                COMMAND_PREFIX = getString(R.string.command_prefix_default)
                COMMAND_DIVIDER = getString(R.string.string_divider_default)
                DESCRIPTOR_DIVIDER = getString(R.string.descriptor_divider_default)

                sharedPreferences.edit().putString(getString(R.string.app_pref_command_prefix), COMMAND_PREFIX).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_command_divider), COMMAND_DIVIDER).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_descriptor_divider), DESCRIPTOR_DIVIDER).apply()

                identifierFragment.refreshHints()
            }

            10002 -> {
                if (resultCode == Activity.RESULT_CANCELED) return
                SEND_ARENA_COMMAND = getString(R.string.send_arena_default)
                FORWARD_COMMAND = getString(R.string.forward_default)
                REVERSE_COMMAND = getString(R.string.reverse_default)
                TURN_LEFT_COMMAND = getString(R.string.turn_left_default)
                TURN_RIGHT_COMMAND = getString(R.string.turn_right_default)

                sharedPreferences.edit().putString(getString(R.string.app_pref_send_arena), SEND_ARENA_COMMAND).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_exploration), getString(R.string.exploration_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_fastest), getString(R.string.fastest_path_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_pause), getString(R.string.pause_default)).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_forward), FORWARD_COMMAND).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_reverse), REVERSE_COMMAND).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_turn_left), TURN_LEFT_COMMAND).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_turn_right), TURN_RIGHT_COMMAND).apply()

                identifierFragment.refreshHints()
            }

            10003 -> {
                if (resultCode == Activity.RESULT_CANCELED) return
                GRID_IDENTIFIER = getString(R.string.grid_descriptor_default)
                SET_IMAGE_IDENTIFIER = getString(R.string.set_image_default)
                ROBOT_POSITION_IDENTIFIER = getString(R.string.robot_position_default)
                ROBOT_STATUS_IDENTIFIER = getString(R.string.robot_status_default)

                sharedPreferences.edit().putString(getString(R.string.app_pref_grid_identifier), GRID_IDENTIFIER).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_set_image_identifier), SET_IMAGE_IDENTIFIER).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_robot_position_identifier), ROBOT_POSITION_IDENTIFIER).apply()
                sharedPreferences.edit().putString(getString(R.string.app_pref_robot_status_identifier), ROBOT_STATUS_IDENTIFIER).apply()

                identifierFragment.refreshHints()
            }

            11000 -> identifierFragment.saveOnHold(resultCode)
            11001 -> customFragment.saveOnHold(resultCode)
        }
    }
}
