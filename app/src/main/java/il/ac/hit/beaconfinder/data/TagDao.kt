package il.ac.hit.beaconfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/**
 * This interface provides access to local tags table
 */
@Dao
interface TagDao {
    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    @Insert
    suspend fun insertAll(vararg tags: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)
}