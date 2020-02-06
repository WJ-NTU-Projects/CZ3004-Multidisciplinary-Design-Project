package ntu.mdp.android.mdptestkotlin.room.arena

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arena")
data class Arena(
    @PrimaryKey(autoGenerate = true)
    val arena_id: Int,
    val map_name: String,
    val map_descriptor: String,
    val obstacle_descriptor: String,
    val startX: Int,
    val startY: Int,
    val waypointX: Int,
    val waypointY: Int,
    val goalX: Int,
    val goalY: Int
)