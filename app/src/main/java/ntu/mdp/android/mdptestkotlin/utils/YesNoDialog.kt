package ntu.mdp.android.mdptestkotlin.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.dialog_yes_no.*
import ntu.mdp.android.mdptestkotlin.App.Companion.dialogTheme
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.databinding.DialogYesNoBinding


class YesNoDialog : AppCompatActivity() {
    private lateinit var binding        : DialogYesNoBinding
    private lateinit var activityUtil   : ActivityUtil

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
