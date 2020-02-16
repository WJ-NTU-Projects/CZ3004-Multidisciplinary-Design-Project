package ntu.mdp.android.mdptestkotlin.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.APP_LANGUAGE
import ntu.mdp.android.mdptestkotlin.App.Companion.darkMode
import ntu.mdp.android.mdptestkotlin.R
import java.io.Serializable
import java.util.*


class ActivityUtil(private val context: Context) {
    /**
     * Starts an activity.
     * @param c Activity class for the activity to be started.
     * @param extras Extras to be appended to the intent. Supports almost every supported type except Parcelable and Serializable array lists. Use traditional way for these two types.
     * @param fade Flag to use fade animation.
     * @param startNew Flag to start the activity as a new task at the top of the stack.
     */
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

    /**
     * Finishes an activity.
     * @param fade Flag to use fade animation.
     */
    fun finishActivity(fade: Boolean = false) {
        (context as Activity).finish()
        if (fade) context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    /**
     * Sends a snackbar message.
     * @param message Message to be displayed.
     */
    fun sendSnack(message: String?) {
        if (message == null) return
        val windowLayout: View = (context as Activity).findViewById(R.id.window_layout)
        val snackBar: Snackbar = Snackbar.make(windowLayout, message, Snackbar.LENGTH_LONG)
        snackBar.show()
        //Log.e(this::class.simpleName ?: "-", "Broadcasted from ${context::class.simpleName}")
    }

    fun sendSnackIndefinite(message: String?) {
        if (message == null) return
        val windowLayout: View = (context as Activity).findViewById(R.id.window_layout)
        val snackBar: Snackbar = Snackbar.make(windowLayout, message, Snackbar.LENGTH_INDEFINITE)

        snackBar.setAction(context.getString(R.string.x)) {
            snackBar.dismiss()
        }

        if (darkMode) snackBar.setActionTextColor(context.getColor(R.color.colorPrimaryDark))
        else snackBar.setActionTextColor(context.getColor(R.color.colorPrimary))

        val textView = snackBar.view.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
        textView.maxLines = 99
        textView.textSize = context.resources.getDimension(R.dimen.text_size_snack)
        snackBar.show()
        //Log.e(this::class.simpleName ?: "-", "Broadcasted from ${context::class.simpleName}")
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

        /*
        val builder: AlertDialog.Builder? = AlertDialog.Builder(context)

        builder?.setMessage(message)
        builder?.setPositiveButton(rightLabel) { dialog, _ ->
            dialog.dismiss()
            callback(false)
        }

        builder?.setNegativeButton(leftLabel) { dialog, _ ->
            dialog.dismiss()
            callback(true)
        }

        val dialog: AlertDialog? = builder?.create()
        dialog?.show()

         */
    }

    /**
     * Toggles the view state of the progress bar and tint to the visibility specified.
     * @param visibility Visibility state to toggle to.
     * @param opaque If visibility is View.VISIBLE, flag whether to set the alpha value to 1.0f or 0.5f (opaque or translucent).
     */
    fun toggleProgressBar(visibility: Int, opaque: Boolean = true, instant: Boolean = false, endCallback: () -> Unit = {}) {
        val windowTint: View = (context as Activity).findViewById(R.id.window_tint)

        val animatorListener: AnimatorListenerAdapter = object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                windowTint.visibility = visibility
                endCallback()
            }
        }

        if (visibility == View.VISIBLE) {
            windowTint.visibility = View.VISIBLE
            if (instant) {
                windowTint.alpha = if (opaque) 1.0f else 0.5f
                return
            }
            val end = if (opaque) 1.0f else 0.5f
            windowTint.animate().alpha(end).setDuration(App.ANIMATOR_DURATION).setListener(animatorListener)
        } else {
            windowTint.animate().alpha(0.0f).setDuration(App.ANIMATOR_DURATION).setListener(animatorListener)
        }
    }
}