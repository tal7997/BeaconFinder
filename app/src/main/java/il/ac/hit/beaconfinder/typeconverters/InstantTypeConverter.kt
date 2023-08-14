package il.ac.hit.beaconfinder.typeconverters

import android.util.Log
import androidx.room.TypeConverter
import java.time.Instant

/**
 * This class allows converting Instant type into string
 */
class InstantTypeConverter {
    /**
     * Converts from string to Instant
     *
     * @param data string to convert
     */
    @TypeConverter
    fun stringToInstant(data: String?): Instant? {
        return try {
            if (data == null || data == "null")
                null
            else
                Instant.parse(data)
        } catch (e: Exception) {
            Log.d("InstantTypeConverter", "Couldn't convert string to Instant")
            null
        }
    }

    /**
     * Converts from Instant to string
     *
     * @param instant Instant to convert
     */
    @TypeConverter
    fun instantToString(instant: Instant?): String? {
        return instant?.toString() ?: "null"
    }
}
