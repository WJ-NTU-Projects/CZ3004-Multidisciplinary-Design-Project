package ntu.mdp.android.mdptestkotlin.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kotlinx.android.synthetic.main.fragment_settings_general.*
import ntu.mdp.android.mdptestkotlin.App.Companion.APP_LANGUAGE
import ntu.mdp.android.mdptestkotlin.App.Companion.appTheme
import ntu.mdp.android.mdptestkotlin.App.Companion.autoUpdateArena
import ntu.mdp.android.mdptestkotlin.App.Companion.darkMode
import ntu.mdp.android.mdptestkotlin.App.Companion.dialogTheme
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.App.Companion.usingAmd
import ntu.mdp.android.mdptestkotlin.MainActivity
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.databinding.FragmentSettingsGeneralBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil

class SettingsFragmentGeneral : Fragment() {

    private var binding: FragmentSettingsGeneralBinding? = null
    private lateinit var activityUtil: ActivityUtil
    private lateinit var languageList: ArrayList<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingsGeneralBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activityUtil = (activity as SettingsActivity).activityUtil
        languageList = arrayListOf(getString(R.string.language), getString(R.string.language))

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

        themeSwitch.isChecked = darkMode
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(getString(R.string.app_pref_dark_mode), isChecked).apply()
            darkMode = isChecked

            if (darkMode) {
                appTheme = R.style.AppTheme_Dark
                dialogTheme = R.style.DialogTheme_Dark
            } else {
                appTheme = R.style.AppTheme
                dialogTheme = R.style.DialogTheme
            }

            activityUtil.startActivity(MainActivity::class.java, fade = true, startNew = true)
        }

        languageRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = LanguageAdapter()
        }

        languageRecycler.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
    }

    override fun onResume() {
        super.onResume()
        activityUtil.setTitle(getString(R.string.settings))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private inner class LanguageAdapter : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
            init {
                view.setOnClickListener(this)
            }

            val label: MaterialTextView = (view.findViewById(R.id.listitem_language_label) as MaterialTextView)
            val selectedLabel: MaterialTextView = (view.findViewById(R.id.listitem_language_selected_label) as MaterialTextView)

            override fun onClick(v: View?) {
                when (adapterPosition) {
                    0 -> if (APP_LANGUAGE == "en") return else sharedPreferences.edit().putString(getString(R.string.app_pref_language), "en").apply()
                    1 -> if (APP_LANGUAGE == "zh") return else sharedPreferences.edit().putString(getString(R.string.app_pref_language), "zh").apply()
                }

                APP_LANGUAGE = sharedPreferences.getString(getString(R.string.app_pref_language), getString(R.string.language_default)) ?: getString(R.string.language_default)
                activityUtil.startActivity(MainActivity::class.java, fade = true, startNew = true)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.listitem_language, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.label.text = languageList[position]
            holder.selectedLabel.text = ""

            when (position) {
                0 -> if (APP_LANGUAGE == "en") holder.selectedLabel.text = context?.getString(R.string.selected)
                1 -> if (APP_LANGUAGE == "zh") holder.selectedLabel.text = context?.getString(R.string.selected)
            }

        }

        override fun getItemCount() = languageList.size
    }
}
