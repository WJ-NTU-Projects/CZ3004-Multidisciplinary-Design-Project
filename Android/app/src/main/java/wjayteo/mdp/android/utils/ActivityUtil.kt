package wjayteo.mdp.android.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar
import wjayteo.mdp.android.App.Companion.DARK_MODE
import wjayteo.mdp.android.R
import java.io.Serializable
import java.util.*

class ActivityUtil(private val context: Context) {
    fun startActivity(c: Class<*>, extras: HashMap<String, Any> = hashMapOf(), fade: Boolean = false, startNew: Boolean = false) {
        val intent = Intent(context, c)

        for (extra in extras) {
            when (val value = extra.value) {
                is Boolean -> intent.putExtra(extra.key, value)
                is BooleanArray -> intent.putExtra(extra.key, value)
                is Byte -> intent.putExtra(extra.key, value)
                is ByteArray -> intent.putExtra(extra.key, value)
                is Char -> intent.putExtra(extra.key, value)
                is CharArray -> intent.putExtra(extra.key, value)
                is CharSequence -> intent.putExtra(extra.key, value)
                is Double -> intent.putExtra(extra.key, value)
                is DoubleArray -> intent.putExtra(extra.key, value)
                is Float -> intent.putExtra(extra.key, value)
                is FloatArray -> intent.putExtra(extra.key, value)
                is Int -> intent.putExtra(extra.key, value)
                is IntArray -> intent.putExtra(extra.key, value)
                is Long -> intent.putExtra(extra.key, value)
                is LongArray -> intent.putExtra(extra.key, value)
                is Short -> intent.putExtra(extra.key, value)
                is ShortArray -> intent.putExtra(extra.key, value)
                is Serializable -> intent.putExtra(extra.key, value)
                is String -> intent.putExtra(extra.key, value)
                is Parcelable -> intent.putExtra(extra.key, value)
                else -> throw RuntimeException("Invalid extra type.")
            }
        }

        if (startNew) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        (context as Activity).startActivity(intent)
        if (startNew) context.finish()
        if (fade || startNew) context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    fun finishActivity(fade: Boolean = false) {
        (context as Activity).finish()
        if (fade) context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    fun sendSnack(message: String?) {
        if (message == null) return
        val windowLayout: View = (context as Activity).findViewById(R.id.window_layout)
        val snackBar: Snackbar = Snackbar.make(windowLayout, message, Snackbar.LENGTH_LONG)
        snackBar.show()
    }

    fun sendSnackIndefinite(message: String?) {
        if (message == null) return
        val windowLayout: View = (context as Activity).findViewById(R.id.window_layout)
        val snackBar: Snackbar = Snackbar.make(windowLayout, message, Snackbar.LENGTH_INDEFINITE)

        snackBar.setAction(context.getString(R.string.x)) {
            snackBar.dismiss()
        }

        if (DARK_MODE) snackBar.setActionTextColor(context.getColor(R.color.colorPrimaryDark))
        else snackBar.setActionTextColor(context.getColor(R.color.colorPrimary))

        val textView = snackBar.view.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
        textView.maxLines = 99
        textView.textSize = context.resources.getDimension(R.dimen.text_size_snack)
        snackBar.show()
    }

    fun setTitle(title: String) {
        val toolbar: Toolbar? = ((context as Activity).findViewById(R.id.toolbar) as Toolbar)
        toolbar?.title = title
    }

    fun sendDialog(requestCode: Int, message: String, label: String = context.getString(R.string.ok), title: String = "") {
        val intent = Intent(context, OkDialog::class.java)
        intent.putExtra("message", message)
        intent.putExtra("label", label)
        intent.putExtra("title", title)
        (context as Activity).startActivityForResult(intent, requestCode)
    }

    fun sendYesNoDialog(requestCode: Int, message: String, leftLabel: String = context.getString(R.string.yes), rightLabel: String = context.getString(R.string.no), title: String = "") {
        val intent = Intent(context, YesNoDialog::class.java)
        intent.putExtra("message", message)
        intent.putExtra("leftLabel", leftLabel)
        intent.putExtra("rightLabel", rightLabel)
        intent.putExtra("title", title)
        (context as Activity).startActivityForResult(intent, requestCode)
    }
}