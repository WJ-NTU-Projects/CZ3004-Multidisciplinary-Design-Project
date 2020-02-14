package ntu.mdp.android.mdptestkotlin.utils

import android.annotation.SuppressLint
import android.content.Context
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
        FLING_DOWN,
        FLING_UP
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
        gestureDetector.onTouchEvent(event)
        return true
    }

    inner class GestureListener: GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
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
            if (abs(velocityX) > abs(velocityY)) {
                if (velocityX >= 150) {
                    callback(this@GestureImageView, Gesture.FLING_RIGHT)
                } else if (velocityX <= -150){
                    callback(this@GestureImageView, Gesture.FLING_LEFT)
                }

                return true
            }

            if (abs(velocityY) > abs(velocityX)) {
                if (velocityY >= 1500) {
                    callback(this@GestureImageView, Gesture.FLING_DOWN)
                } else if (velocityY <= -1500) {
                    callback(this@GestureImageView, Gesture.FLING_UP)
                }
                return true
            }

            return true
        }
    }
}