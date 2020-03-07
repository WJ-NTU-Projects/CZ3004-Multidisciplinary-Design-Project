package wjayteo.mdp.android.settings

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_custom.*
import wjayteo.mdp.android.App.Companion.sharedPreferences
import wjayteo.mdp.android.R
import wjayteo.mdp.android.databinding.FragmentSettingsCustomBinding
import wjayteo.mdp.android.utils.ActivityUtil

class SettingsFragmentCustom : Fragment() {

    private var binding: FragmentSettingsCustomBinding? = null
    private lateinit var activityUtil: ActivityUtil
    private lateinit var viewOnHold: EditText
    private var enterPressed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingsCustomBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activityUtil = (activity as SettingsActivity).activityUtil

        f1LabelEditText.setOnKeyListener(onEnter)
        f1LabelEditText.onFocusChangeListener = onFocusLost

        f1CommandEditText.setOnKeyListener(onEnter)
        f1CommandEditText.onFocusChangeListener = onFocusLost

        f2LabelEditText.setOnKeyListener(onEnter)
        f2LabelEditText.onFocusChangeListener = onFocusLost

        f2CommandEditText.setOnKeyListener(onEnter)
        f2CommandEditText.onFocusChangeListener = onFocusLost
    }

    override fun onResume() {
        super.onResume()
        activityUtil.setTitle(getString(R.string.customise))
        refreshHints()
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
            R.id.f1LabelEditText -> sharedPreferences.edit().putString(getString(R.string.app_pref_label_f1), input).apply()
            R.id.f1CommandEditText -> sharedPreferences.edit().putString(getString(R.string.app_pref_command_f1), input).apply()
            R.id.f2LabelEditText -> sharedPreferences.edit().putString(getString(R.string.app_pref_label_f2), input).apply()
            R.id.f2CommandEditText -> sharedPreferences.edit().putString(getString(R.string.app_pref_command_f2), input).apply()
        }

        view.text.clear()
        view.clearFocus()
        refreshHints()
    }

    fun refreshHints() {
        f1LabelEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_label_f1), getString(R.string.f1_default))
        f1CommandEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_f1), getString(R.string.f1_default))
        f2LabelEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_label_f2), getString(R.string.f2_default))
        f2CommandEditText.hint = sharedPreferences.getString(getString(R.string.app_pref_command_f2), getString(R.string.f2_default))
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
            activityUtil.sendYesNoDialog(11001, getString(R.string.unsaved_changes), leftLabel = getString(R.string.save), rightLabel = getString(R.string.discard))
        }

        enterPressed = false
    }
}
