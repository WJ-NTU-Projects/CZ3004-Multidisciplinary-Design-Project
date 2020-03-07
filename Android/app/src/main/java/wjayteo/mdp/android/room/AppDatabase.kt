package wjayteo.mdp.android.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import wjayteo.mdp.android.room.arena.Arena
import wjayteo.mdp.android.room.arena.ArenaDao

@Database(entities = [Arena::class], version = 2, exportSchema = false)

abstract class AppDatabase: RoomDatabase() {
    abstract fun arenaDao(): ArenaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE

            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}