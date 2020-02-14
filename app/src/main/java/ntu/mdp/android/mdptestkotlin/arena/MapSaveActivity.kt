package ntu.mdp.android.mdptestkotlin.arena

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.LocaleList
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_map_save.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ntu.mdp.android.mdptestkotlin.App
import ntu.mdp.android.mdptestkotlin.App.Companion.dialogTheme
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMapSaveBinding
import ntu.mdp.android.mdptestkotlin.room.AppDatabase
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil
import java.util.*


class MapSaveActivity : AppCompatActivity() {
    private lateinit var binding        : ActivityMapSaveBinding
    private lateinit var activityUtil   : ActivityUtil
    private lateinit var database       : AppDatabase

    override fun attachBaseContext(newBase: Context?) {
        val res: Resources? = newBase?.resources
        val configuration: Configuration? = res?.configuration
        val newLocale = Locale(App.APP_LANGUAGE)
        configuration?.setLocale(newLocale)
        val localeList = LocaleList(newLocale)
        LocaleList.setDefault(localeList)
        configuration?.setLocales(localeList)

        if (configuration != null) {
            val context = newBase.createConfigurationContext(configuration)
            super.attachBaseContext(ContextWrapper(context))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        super.applyOverrideConfiguration(baseContext.resources.configuration);
    }

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
