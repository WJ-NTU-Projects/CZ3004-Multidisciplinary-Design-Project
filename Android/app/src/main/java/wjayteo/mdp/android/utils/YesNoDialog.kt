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
import kotlinx.android.synthetic.main.dialog_yes_no.*
import wjayteo.mdp.android.App
import wjayteo.mdp.android.App.Companion.dialogTheme
import wjayteo.mdp.android.R
import wjayteo.mdp.android.databinding.DialogYesNoBinding
import java.util.*


class YesNoDialog : AppCompatActivity() {
    private lateinit var binding        : DialogYesNoBinding
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
        super.applyOverrideConfiguration(baseContext.resources.configuration);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(dialogTheme)
        super.onCreate(savedInstanceState)
        binding = DialogYesNoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.setFinishOnTouchOutside(false)
        activityUtil = ActivityUtil(this)

        val message: String = intent.getStringExtra("message") ?: ""
        val leftLabel: String = intent.getStringExtra("leftLabel") ?: ""
        val rightLabel: String = intent.getStringExtra("rightLabel") ?: ""
        val title: String = intent.getStringExtra("title") ?: ""

        yesNoMessageLabel.text = message
        yesNoPositiveButton.text = leftLabel
        yesNoNegativeButton.text = rightLabel

        if (title.isNotBlank()) {
            yesNoTitleLabel.text = title
        } else {
            yesNoTitleLabel.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_FIRST_USER, Intent())
        finish()
    }

    @Suppress("unused")
    fun clickUiButton(view: View) {
        when (view.id) {
            R.id.yesNoPositiveButton -> {
                setResult(Activity.RESULT_OK, Intent())
                finish()
            }

            R.id.yesNoNegativeButton -> {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
        }
    }
}
