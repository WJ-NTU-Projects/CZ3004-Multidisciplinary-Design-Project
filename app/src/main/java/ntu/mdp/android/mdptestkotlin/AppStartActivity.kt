package ntu.mdp.android.mdptestkotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ntu.mdp.android.mdptestkotlin.App.Companion.isSimple
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil

class AppStartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isSimple)   ActivityUtil(this).startActivity(MainSimpleActivity::class.java, startNew = true)
        else            ActivityUtil(this).startActivity(MainActivity::class.java, startNew = true)
    }
}
