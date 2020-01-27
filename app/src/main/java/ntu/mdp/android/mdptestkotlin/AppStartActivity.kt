package ntu.mdp.android.mdptestkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.App.Companion.ANIMATOR_DURATION
import ntu.mdp.android.mdptestkotlin.App.Companion.appTheme
import ntu.mdp.android.mdptestkotlin.App.Companion.isSimple
import ntu.mdp.android.mdptestkotlin.App.Companion.sharedPreferences
import ntu.mdp.android.mdptestkotlin.MainActivityController.Companion.currentMode
import ntu.mdp.android.mdptestkotlin.MainActivityController.Companion.isPlotting
import ntu.mdp.android.mdptestkotlin.MainActivityController.Companion.robotAutonomous
import ntu.mdp.android.mdptestkotlin.arena.ArenaController
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMainBinding
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil
import ntu.mdp.android.mdptestkotlin.utils.TouchController
import ntu.mdp.android.mdptestkotlin.utils.TouchController.Companion.isSwipeMode

class AppStartActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isSimple)   ActivityUtil(this).startActivity(MainSimpleActivity::class.java, startNew = true)
        else            ActivityUtil(this).startActivity(MainActivity::class.java, startNew = true)
    }
}
