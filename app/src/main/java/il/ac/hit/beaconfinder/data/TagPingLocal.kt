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
 * This class represents an entry in local database in tag pings table
 * Entry contains sampleId as primary key, tag's mac address, timestamp and ping location.
 */
@Entity(tableName = "tagPings")
data class TagPingLocal(@ColumnInfo(name = "macAddress") val macAddress: String) {
    @PrimaryKey(autoGenerate = true)
    var sampleId: Int = 0

    @ColumnInfo(name = "timestamp")
    @TypeConverters(InstantTypeConverter::class)
    var timestamp: Instant = Instant.now()

    @ColumnInfo(name = "location")
    @TypeConverters(GeoPointTypeConverter::class)
    var location: GeoPoint = GeoPoint(0.0, 0.0)
}