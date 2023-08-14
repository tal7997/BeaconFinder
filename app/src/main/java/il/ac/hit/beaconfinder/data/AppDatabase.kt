package il.ac.hit.beaconfinder.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * This represents the class used by Room (sqlite wrapper) to provide access
 * to local database
 */
@Database(entities = [TagEntity::class, TagPingLocal::class, GroupEntity::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun tagPingsDao(): TagPingDao
    abstract fun GroupDao(): GroupDao


}