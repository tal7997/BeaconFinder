package il.ac.hit.beaconfinder

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint
import com.google.gson.GsonBuilder
import java.time.Duration
import java.time.Instant

/**
 * This class represents TagData used by ViewModels and the rest of the UI
 */
data class TagData constructor(
    var macAddress: String,
    var description: String = "",
    var icon: String = "",
    var distance: Double = 0.0,
    var lastSeen: Instant? = null, // when was tag last seen
    var lastSeenAt: GeoPoint? = null,
    var batteryAlertNext: Instant? = null, // when to alert the user
    var batteryAlertStart: Instant? = null, // when the user set up the alert
    var expiresAt: Instant? = null, // when tag has to expire
) : Parcelable {
    /**
     * Deserializer from Parcel constructor
     */
    private constructor(parcel: Parcel) : this(macAddress = parcel.readString()!!) {
        description = parcel.readString()!!
        icon = parcel.readString()!!
        distance = parcel.readDouble()
        if (parcel.readByte().toInt() == 1) {
            lastSeen = Instant.parse(parcel.readString()!!)
            lastSeenAt = GeoPoint(parcel.readDouble(), parcel.readDouble())
        }
        val batteryAlertNextStr = parcel.readString()
        val batteryAlertStartStr = parcel.readString()
        if (batteryAlertNextStr != null && batteryAlertStartStr != null) {
            batteryAlertNext = Instant.parse(batteryAlertNextStr)
            batteryAlertStart = Instant.parse(batteryAlertStartStr)
        }
        val expiresAtStr = parcel.readString()
        if (expiresAtStr != null) {
            expiresAt = Instant.parse(expiresAtStr)
        }
    }

    /**
     * toString() triggers serialization to JSON string
     *
     * @return JSON string representing TagData
     */
    override fun toString(): String {
        val gson = GsonBuilder().serializeSpecialFloatingPointValues().create()
        return gson.toJson(this)
    }

    /**
     * Writes TagData to a parcel
     * @param dest Parcel to write into
     * @param flags Flags aren't relevant to our case
     */
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        if (dest == null)
            return
        dest.writeString(macAddress)
        dest.writeString(description)
        dest.writeString(icon)
        dest.writeDouble(distance)
        if (lastSeen != null && lastSeenAt != null) {
            dest.writeByte(1)
            dest.writeString(lastSeen.toString())
            dest.writeString(lastSeenAt!!.latitude.toString())
            dest.writeString(lastSeenAt!!.longitude.toString())
        } else {
            dest.writeByte(0)
        }

        dest.writeString(batteryAlertNext?.toString())
        dest.writeString(batteryAlertStart?.toString())
        dest.writeString(expiresAt?.toString())
    }

    /**
     * Bitmask to describe Parcelable contents.
     * No special flags in this case
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * Returns a float value between 0f and 1f representing remaining battery charge
     */
    fun getBatteryPercent(): Float? {
        if (batteryAlertNext == null || batteryAlertStart == null)
            return null
        val secondsSinceBatteryChange =
            Duration.between(batteryAlertStart, Instant.now()).seconds
        return 1.0f - (secondsSinceBatteryChange.toFloat() / batteryLongevityInSeconds)
            .coerceIn(0f, 1f)
    }

    /**
     * Resets battery values to default batteryLongevityInSeconds
     */
    fun resetBatteryTo100() {
        batteryAlertStart = Instant.now()
        batteryAlertNext =
            Instant.now().plusSeconds(batteryLongevityInSeconds)
        expiresAt = Instant.now().plusMillis(1000 * 20)
    }

    companion object CREATOR : Parcelable.Creator<TagData> {
        // Battery longevity estimate in seconds, assume battery lasts a month
        private const val batteryLongevityInSeconds: Long = 4 * 7 * 24 * 60 * 60

        /**
         * This method builds a TagData object from a JSON string
         *
         * @param src JSON string to build TagData from
         */
        fun fromString(src: String): TagData {
            val gson = GsonBuilder().serializeSpecialFloatingPointValues().create()
            return gson.fromJson(src, TagData::class.java)
        }

        override fun createFromParcel(parcel: Parcel): TagData {
            return TagData(parcel)
        }

        override fun newArray(size: Int): Array<TagData?> {
            return arrayOfNulls(size)
        }
    }
}
