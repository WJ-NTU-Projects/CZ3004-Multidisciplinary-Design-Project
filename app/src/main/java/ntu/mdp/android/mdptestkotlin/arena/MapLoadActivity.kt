package ntu.mdp.android.mdptestkotlin.arena

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import kotlinx.android.synthetic.main.activity_map_load.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.App.Companion.appTheme
import ntu.mdp.android.mdptestkotlin.App.Companion.dialogTheme
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.databinding.ActivityMapLoadBinding
import ntu.mdp.android.mdptestkotlin.room.AppDatabase
import ntu.mdp.android.mdptestkotlin.room.arena.Arena
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil
import ntu.mdp.android.mdptestkotlin.utils.AdapterSwipeController


class MapLoadActivity : AppCompatActivity() {
    private lateinit var binding        : ActivityMapLoadBinding
    private lateinit var activityUtil   : ActivityUtil
    private lateinit var database       : AppDatabase
    private lateinit var dataList       : ArrayList<Arena>
    private lateinit var deleteList     : ArrayList<Arena>
    private lateinit var recyclerAdapter: DeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(dialogTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMapLoadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activityUtil = ActivityUtil(this)
        database = AppDatabase.getDatabase(applicationContext)
        dataList = arrayListOf()
        deleteList = arrayListOf()
        noSaveDataLabel.visibility = View.GONE
        bottomMarginView.visibility = View.GONE

        mapLoadRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MapLoadActivity)
        }

        mapLoadRecycler.addItemDecoration(DividerItemDecoration(ContextThemeWrapper(this, appTheme), LinearLayoutManager.VERTICAL))


        CoroutineScope(Dispatchers.IO).launch {
            dataList = ArrayList(database.arenaDao().selectAll())
        }.invokeOnCompletion {
            CoroutineScope(Dispatchers.Main).launch {
                recyclerAdapter = DeviceAdapter()
                mapLoadRecycler.adapter = recyclerAdapter

                if (dataList.isEmpty()) {
                    mapLoadRecycler.visibility = View.GONE
                    noSaveDataLabel.visibility = View.VISIBLE
                    bottomMarginView.visibility = View.VISIBLE
                }
            }
        }

        val swipeToDeleteCallback = object : AdapterSwipeController(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
                val position = viewHolder.adapterPosition
                val item: Arena = dataList[position]
                val name: String = item.map_name
                recyclerAdapter.removeItem(position)
                deleteList.add(item)

                val snackBar = Snackbar.make(window_layout, getString(R.string.map_removed, name), Snackbar.LENGTH_LONG)

                snackBar.setAction("UNDO") {
                    recyclerAdapter.restoreItem(item, position)
                    mapLoadRecycler.scrollToPosition(position)
                    deleteList.remove(item)
                }

                snackBar.setActionTextColor(Color.YELLOW)
                snackBar.show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(mapLoadRecycler)
    }

    override fun onPause() {
        super.onPause()

        CoroutineScope(Dispatchers.IO).launch {
            for (arena in deleteList) {
                database.arenaDao().delete(arena)
            }
        }
    }

    @Suppress("unused")
    fun clickUiButton(view: View) {
        when (view.id) {
            R.id.closeButton -> activityUtil.finishActivity()
        }
    }

    private inner class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
            init {
                view.setOnClickListener(this)
            }

            val label: MaterialTextView = (view.findViewById(R.id.listitem_map_load_label) as MaterialTextView)
            var pos: Int = -1

            override fun onClick(v: View?) {
                val intent = Intent()
                intent.putExtra(getString(R.string.app_result_map_id), dataList[pos].arena_id)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val textView = LayoutInflater.from(parent.context).inflate(R.layout.listitem_map_load, parent, false)
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.label.text = dataList[position].map_name
            holder.pos = position
        }

        override fun getItemCount() = dataList.size

        fun removeItem(position: Int) {
            dataList.removeAt(position)
            notifyItemRemoved(position)
        }

        fun restoreItem(item: Arena, position: Int) {
            dataList.add(position, item)
            notifyItemInserted(position)
        }
    }
}
