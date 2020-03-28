package wjayteo.mdp.android.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.LocaleList
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.dialog_ok.*
import wjayteo.mdp.android.App
import wjayteo.mdp.android.App.Companion.DIALOG_THEME
import wjayteo.mdp.android.R
import wjayteo.mdp.android.databinding.DialogOkBinding
import java.util.*


class OkDialog : AppCompatActivity() {
    private lateinit var binding        : DialogOkBinding
    private lateinit var activityUtil   : ActivityUtil

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
        setTheme(DIALOG_THEME)
        super.onCreate(savedInstanceState)
        binding = DialogOkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.setFinishOnTouchOutside(false)
        activityUtil = ActivityUtil(this)

        val message: String = intent.getStringExtra("message") ?: ""
        val label: String = intent.getStringExtra("label") ?: ""
        val title: String = intent.getStringExtra("title") ?: ""

        okMessageLabel.text = message
        okDialogButton.text = label

        if (title.isNotBlank()) {
            okTitleLabel.text = title
        } else {
            okTitleLabel.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        // DO NOTHING
    }

    @Suppress("unused")
    fun clickUiButton(view: View) {
        when (view.id) {
            R.id.okDialogButton -> {
                setResult(Activity.RESULT_OK, Intent())
                finish()
            }
        }
    }
}
