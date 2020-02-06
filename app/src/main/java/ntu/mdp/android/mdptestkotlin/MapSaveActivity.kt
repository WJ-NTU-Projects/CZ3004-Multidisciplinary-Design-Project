package ntu.mdp.android.mdptestkotlin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_map_save.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ntu.mdp.android.mdptestkotlin.App.Companion.dialogTheme
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMapSaveBinding
import ntu.mdp.android.mdptestkotlin.room.AppDatabase
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil


class MapSaveActivity : AppCompatActivity() {
    private lateinit var binding        : ActivityMapSaveBinding
    private lateinit var activityUtil   : ActivityUtil
    private lateinit var database       : AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(dialogTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMapSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activityUtil = ActivityUtil(this)
        database = AppDatabase.getDatabase(applicationContext)
    }

    @Suppress("unused")
    fun clickUiButton(view: View) {
        when (view.id) {
            R.id.saveButton -> {
                CoroutineScope(Dispatchers.Main).launch {
                    val mapName: String = mapNameEditText.text.toString().trim()

                    if (mapName.isBlank()) {
                        activityUtil.sendSnack(getString(R.string.error_name_blank))
                        return@launch
                    }

                    var exists = true

                    withContext(Dispatchers.IO) {
                        exists = (database.arenaDao().selectByName(mapName) != null)
                    }

                    if (exists) {
                        activityUtil.sendSnack(getString(R.string.error_name_not_available))
                        return@launch
                    }

                    val intent = Intent()
                    intent.putExtra(getString(R.string.app_result_map_name), mapName)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }

            R.id.cancelButton -> {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }

            R.id.clearButton -> mapNameEditText.text.clear()
        }
    }
}
