package il.ac.hit.beaconfinder.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.firebase.firestore.GeoPoint
import il.ac.hit.beaconfinder.typeconverters.GeoPointTypeConverter
import il.ac.hit.beaconfinder.typeconverters.InstantTypeConverter
import java.time.Instant

/**
 * This class represents an entry in local database in tags table
 * Entry contains macAddress as primary key.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "mac")
    val macAddress: String
) {
    @ColumnInfo(name = "description")
    var description: String = ""

    @ColumnInfo(name = "icon")
    var icon: String = ""

    @ColumnInfo(name = "distance")
    var distance: Double = 0.0

    @ColumnInfo(name = "last_seen")
    @TypeConverters(InstantTypeConverter::class)
    var lastSeen: Instant? = null

    @ColumnInfo(name = "last_seen_at")
    @TypeConverters(GeoPointTypeConverter::class)
    var lastSeenAt: GeoPoint? = null

    @ColumnInfo(name = "battery_alert_next")
    @TypeConverters(InstantTypeConverter::class)
    var batteryAlertNext: Instant? = null

    @ColumnInfo(name = "battery_alert_start")
    @TypeConverters(InstantTypeConverter::class)
    var batteryAlertStart: Instant? = null

    @ColumnInfo(name = "expiresAt")
    @TypeConverters(InstantTypeConverter::class)
    var expiresAt: Instant? = null
}
