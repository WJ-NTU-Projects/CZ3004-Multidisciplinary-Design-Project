package ntu.mdp.android.mdptestkotlin.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import ntu.mdp.android.mdptestkotlin.arena.ArenaController
import kotlin.math.abs


class GestureImageView(c: Context): AppCompatImageView(c) {
    private lateinit var callback: (view: GestureImageView, gesture: ArenaController.Gesture) -> Unit
    private val gestureDetector: GestureDetector

    init {
        gestureDetector = GestureDetector(c, GestureListener())
    }

    fun setCallback(callback: (view: GestureImageView, gesture: ArenaController.Gesture) -> Unit) {
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
            callback(this@GestureImageView, ArenaController.Gesture.SINGLE_TAP)
            return true
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            callback(this@GestureImageView, ArenaController.Gesture.DOUBLE_TAP)
            return true
        }

        override fun onLongPress(e: MotionEvent?) {
            callback(this@GestureImageView, ArenaController.Gesture.LONG_PRESS)
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {

            Log.e("FLING", "$velocityX, $velocityY")

            if (abs(velocityX) > abs(velocityY)) {
                if (velocityX >= 500) {
                    callback(this@GestureImageView, ArenaController.Gesture.FLING_RIGHT)
                } else {
                    callback(this@GestureImageView, ArenaController.Gesture.FLING_LEFT)
                }

                return true
            }

            if (abs(velocityY) > abs(velocityX) && velocityY >= 2000) {
                callback(this@GestureImageView, ArenaController.Gesture.FLING_DOWN)
                return true
            }

            return true
        }
    }
}