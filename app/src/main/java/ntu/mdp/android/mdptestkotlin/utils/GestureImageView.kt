package ntu.mdp.android.mdptestkotlin.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs


class GestureImageView(c: Context): AppCompatImageView(c) {
    enum class Gesture {
        SINGLE_TAP,
        DOUBLE_TAP,
        LONG_PRESS,
        FLING_LEFT,
        FLING_RIGHT,
        FLING_DOWN
    }

    private lateinit var callback: (view: GestureImageView, gesture: Gesture) -> Unit
    private val gestureDetector: GestureDetector

    init {
        gestureDetector = GestureDetector(c, GestureListener())
    }

    fun setCallback(callback: (view: GestureImageView, gesture: Gesture) -> Unit) {
        this.callback = callback
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action != MotionEvent.ACTION_MOVE) {
            Log.e("EVENT", event.toString())
        }

        gestureDetector.onTouchEvent(event)
        return true
    }

    inner class GestureListener: GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            Log.e("DOWN", "DOWN")
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            callback(this@GestureImageView, Gesture.SINGLE_TAP)
            return true
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            callback(this@GestureImageView, Gesture.DOUBLE_TAP)
            return true
        }

        override fun onLongPress(e: MotionEvent?) {
            callback(this@GestureImageView, Gesture.LONG_PRESS)
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {

            Log.e("FLING", "$velocityX, $velocityY")

            if (abs(velocityX) > abs(velocityY)) {
                if (velocityX >= 300) {
                    callback(this@GestureImageView, Gesture.FLING_RIGHT)
                } else {
                    callback(this@GestureImageView, Gesture.FLING_LEFT)
                }

                return true
            }

            if (abs(velocityY) > abs(velocityX) && velocityY >= 1500) {
                callback(this@GestureImageView, Gesture.FLING_DOWN)
                return true
            }

            return true
        }
    }
}