package ntu.mdp.android.mdptestkotlin.utils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.dialog_ok.*
import ntu.mdp.android.mdptestkotlin.App.Companion.dialogTheme
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.databinding.DialogOkBinding


class OkDialog : AppCompatActivity() {
    private lateinit var binding        : DialogOkBinding
    private lateinit var activityUtil   : ActivityUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(dialogTheme)
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
