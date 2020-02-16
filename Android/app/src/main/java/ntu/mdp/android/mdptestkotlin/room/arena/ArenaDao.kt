package ntu.mdp.android.mdptestkotlin.room.arena

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ArenaDao {
    @Query("SELECT * FROM `arena`")
    fun selectAll(): List<Arena>

    @Query("SELECT * FROM `arena` WHERE `arena_id` = :id")
    fun selectById(id: Int): Arena?

    @Query("SELECT * FROM `arena` WHERE UPPER(`map_name`) = UPPER(:name)")
    fun selectByName(name: String): Arena?

    @Insert
    fun insert(vararg arenas: Arena)

    @Delete
    fun delete(arena: Arena)
}