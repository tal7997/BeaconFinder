package il.ac.hit.beaconfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/**
 * This interface provides access to local tag pings table
 * The table contains pings that have not been sent to firebase
 */
@Dao
interface TagPingDao {
    @Query("SELECT * FROM tagPings")
    suspend fun getAll(): List<TagPingLocal>

    @Insert
    suspend fun insertAll(vararg tags: TagPingLocal)

    @Delete
    suspend fun delete(tag: TagPingLocal)
}