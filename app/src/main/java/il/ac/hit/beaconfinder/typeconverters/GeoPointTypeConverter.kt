package il.ac.hit.beaconfinder.typeconverters

import androidx.room.TypeConverter
import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson

/**
 * This class provides a converter for Gson library so we are able
 * to serialize firebase's GeoPoint into JSON
 */
class GeoPointTypeConverter {
    /**
     * Converts from JSON string into GeoPoint
     * @param data JSON string to convert
     */
    @TypeConverter
    fun stringToGeoPoint(data: String?): GeoPoint? {
        return Gson().fromJson(data, GeoPoint::class.java)
    }

    /**
     * Converts from GeoPoint into JSON string
     *
     * @param geoPoint GeoPoint to convert
     */
    @TypeConverter
    fun geoPointToString(geoPoint: GeoPoint?): String? {
        return Gson().toJson(geoPoint)
    }
}
