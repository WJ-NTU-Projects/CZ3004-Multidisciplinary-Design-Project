package ntu.mdp.android.mdptestkotlin.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class ViewPagerController(context: Context, attributes: AttributeSet): ViewPager(context, attributes) {
    @SuppressLint("ClickableViewAccessibility")
    /**
     * Overrides the default touch event to ignore all touches.
     */
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return false
    }

    /**
     * Overrides the default touch intercept event to ignore all touches.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return false
    }
}