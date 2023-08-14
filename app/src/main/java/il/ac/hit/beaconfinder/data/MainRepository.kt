package il.ac.hit.beaconfinder.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import il.ac.hit.beaconfinder.MainActivity
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.TagData
import il.ac.hit.beaconfinder.firebase.FirebaseLinkUtils
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import il.ac.hit.beaconfinder.firebase.TagPing
import il.ac.hit.beaconfinder.firebase.TagSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.altbeacon.beacon.Beacon
import java.time.Instant
import java.util.*
import javax.inject.Inject
import kotlin.math.log

/**
 * This class represents the main repository of the project, allowing access to local database
 * (room/sqlite) and cloud database (firebase) as well as providing means to raise notifications
 */
class MainRepository @Inject constructor(
    val tagsDb: AppDatabase,
     val firebase: FirebaseUtils,
    private val firebaseLinks: FirebaseLinkUtils
) {
    private val nearbyBeacons: MutableLiveData<List<Beacon>> = MutableLiveData<List<Beacon>>()
    private val registeredBeacons: MutableLiveData<List<TagData>> = MutableLiveData<List<TagData>>()
    private val tagsSearchedFor = MutableLiveData<List<String>>()
    private val recentlyNotifiedTags = ArrayList<String>()
    private val tagSearchInterval = 5 * 60 * 1000 // recheck every 5 minutes
    private var nextTagsSearchCheck = System.currentTimeMillis()
    private var snapshotListener: ListenerRegistration? = null

    init {
        tagsSearchedFor.postValue(Collections.unmodifiableList(ArrayList()))
    }

    /**
     * Returns a MutableLiveData representing the tags currently searched for
     */
    fun getSearchedForTags(): LiveData<List<String>> {
        return tagsSearchedFor
    }

    /**
     * Returns a MutableLiveData representing a list of nearby beacons
     * detected by bluetooth module
     */
    fun getNearbyBeacons(): LiveData<List<Beacon>> {
        return nearbyBeacons
    }

    /**
     * Sets the list of nearby detected beacons
     */
    fun setNearbyBeacons(beacons: List<Beacon>) {
        nearbyBeacons.postValue(beacons)
    }

    /**
     * Returns a MutableLiveData representing a list of tags currently
     * registered as "owned" by this device
     */
    fun getRegisteredBeacons(): LiveData<List<TagData>> {
        return registeredBeacons
    }

    /**
     * This method starts a fetch from the local database and posts
     * the fetched value into registeredBeacons MutableLiveData
     */
    suspend fun fetchRegisteredBeacons() {
        val tags = getTags()
        registeredBeacons.postValue(tags)
    }

    /**
     * This method fetches the list of "owned" tags from local database
     */
    suspend fun getTags(): List<TagData> {
        val tags = tagsDb.tagDao().getAll()
            .map {
                val td = TagData(it.macAddress)
                td.description = it.description
                td.distance = it.distance
                td.icon = it.icon
                td.lastSeen = it.lastSeen
                td.lastSeenAt = it.lastSeenAt
                td.batteryAlertNext = it.batteryAlertNext
                td.batteryAlertStart = it.batteryAlertStart
                td.expiresAt = it.expiresAt
                td
            }

        return Collections.unmodifiableList(tags)
    }

    /**
     * Adds a tag into local database, this makes it "owned" by this device
     */
    suspend fun addTag(tag: TagData) {
        val entity = TagEntity(tag.macAddress)

        entity.description = tag.description
        entity.distance = tag.distance
        entity.icon = tag.icon
        entity.lastSeen = tag.lastSeen
        entity.lastSeenAt = tag.lastSeenAt
        entity.batteryAlertNext = tag.batteryAlertNext
        entity.batteryAlertStart = tag.batteryAlertStart
        entity.expiresAt = tag.expiresAt

        tagsDb.tagDao().insertAll(entity)
    }

    /**
     * Removes a tag from local database, this makes it "disowned", so
     * the user will no longer receive updates about it
     */
    suspend fun removeTag(tag: TagData) {
        val tagEntity = tagsDb.tagDao().getAll()
            .firstOrNull { x -> x.macAddress == tag.macAddress }
        tagEntity?.let { tagsDb.tagDao().delete(it) }
    }

    /**
     * Adds a tag ping into local database, later on this will be sent
     * to firebase cloud db, but only if it's being looked for
     */
    suspend fun addTagPingLocal(bluetoothAddress: String, geoPoint: GeoPoint) {
        val localTagPing = TagPingLocal(bluetoothAddress)
        localTagPing.location = geoPoint
        tagsDb.tagPingsDao().insertAll(localTagPing)
    }

    /**
     * This method registers firebase snapshot listener into the searched tags
     * database, allowing us to notify the user about new notification
     *
     * @param context Android context, it's needed to raise notifications
     */
    fun registerForFirebaseUpdates(context: Context) {
        if (snapshotListener != null) {
            return
        }

        snapshotListener = firebase.addSnapshotListener(EventListener { value, error ->
            if (error != null) {
                Log.e("MainRepository", "Couldn't register firebase snapshot listener")
                return@EventListener
            }
            if (value == null) {
                Log.e("MainRepository", "Firebase snapshot listener value was null")
                return@EventListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                val tags = getTags()
                val ownedMacs = tags.map { it.macAddress }.toHashSet()

                value.documentChanges.forEach {
                    Log.v("MainRepository", "${it.document.data[FirebaseUtils.macFeild]} ${it.type}")
                }
                val ownedTagsFound = value.documentChanges
                    // filter only owned macs
                    .filter { ownedMacs.contains(it.document.data[FirebaseUtils.macFeild]) }
                    // map to local TagData data classes
                    .map { doc -> tags.first { doc.document.data[FirebaseUtils.macFeild] == it.macAddress } }
                    .distinctBy { it.macAddress }
                    .toList()

                if (ownedTagsFound.isNotEmpty()) {
                    // in case we reached here this means someone else has found a tag this local
                    // device "owns" and is looking after
                    notifyOwnedTagHasBeenFound(context, ownedTagsFound)
                }
            }
        })
    }

    /**
     * This removes the snapshot listener from firebase, triggered at service shutdown
     */
    fun unregisterForFirebaseUpdates() {
        snapshotListener.let {
            firebase.removeSnapshotListener(it ?: return)
        }
        snapshotListener = null
    }

    /**
     * This method is invoked periodically
     * It checks whether pings stored in local database are searched for by someone who "owns" them
     *
     * @param context Android context, needed to raise notifications
     */
    suspend fun checkIfLocalPingsNeedSending(context: Context) {
        if (System.currentTimeMillis() < nextTagsSearchCheck) {
            return // only check every tagSearchInterval milliseconds
        }

        nextTagsSearchCheck = System.currentTimeMillis() + tagSearchInterval

        Log.i("MainRepository", "Tag check interval expired, rechecking")

        val searchedForTags = firebase.getSearchedTags()

        val newTagsSearchedFor = ArrayList<String>(searchedForTags.size)
        newTagsSearchedFor.addAll(searchedForTags.map { it.macAddress })
        tagsSearchedFor.postValue(Collections.unmodifiableList(newTagsSearchedFor))

        handleExpiredTags()

        val pings = tagsDb.tagPingsDao().getAll()
        val reportTags = pings
            .filter { tagsSearchedFor.value?.contains(it.macAddress) == true }
            .map {
                val tagPing = TagPing()
                tagPing.location = it.location
                tagPing.macAddress = it.macAddress
                tagPing
            }
            .toList()

        // notify beacons that have notification, add them to recently notified list
        val reportedTagMacs = reportTags.map { it.macAddress }.toHashSet()
        val notifyTags = searchedForTags
            .filter { reportedTagMacs.contains(it.macAddress) }
            .filter { it.notification.isNotBlank() }
            .filter { !recentlyNotifiedTags.contains(it.macAddress) }
        notifyTagsAreNearby(context, notifyTags)

        val notifyTagss = searchedForTags

            .filter { it.notification.trim().equals("Found", ignoreCase = true) }
        val notifyDe = notifyTagss.joinToString { it.notification }
        val notifyId = notifyTagss.joinToString {it.uid }
        val instance = FirebaseAuth.getInstance()
        val uid = instance.uid!!
        Toast.makeText(context, ""+uid, Toast.LENGTH_SHORT).show()
        if(notifyDe.equals("Found", ignoreCase = true) && notifyId == uid) {
            notifyMissingTag(context, notifyTagss)
        }
        recentlyNotifiedTags.addAll(notifyTags.map { it.macAddress })

        if (firebase.addPings(reportTags)) {

            pings.forEach { tagsDb.tagPingsDao().delete(it) }
            Log.i("MainRepository", "Uploaded ${reportTags.size} pings")

        } else {
            Log.e("MainRepository", "Failed to uploaded ${reportTags.size} pings")

        }

    }

    /**
     * This function looks for all expired tags and deletes them from local db.
     * It also re-fetches tags from db to update UI
     */
    private suspend fun handleExpiredTags() {
        val expiredTags = tagsDb.tagDao().getAll()
            .filter { it.expiresAt != null && it.expiresAt!! < Instant.now() }
            .toList()
        if (expiredTags.any()) { // remove expired tags and refresh UI
            for (tag in expiredTags) {
                tagsDb.tagDao().delete(tag)
                Log.i("MainRepository", "Deleting tag ${tag.macAddress} expiry date has passed")
            }
            fetchRegisteredBeacons()
        }
    }

    /**
     * This method notifies that a tag is nearby (this device has detected a tag someone else is
     * searching for, and it has a notification attached to it )
     *
     * @param context Android context, needed to raise notifications
     * @param notifyTags Tags that have been detected.
     */
    private fun notifyTagsAreNearby(context: Context, notifyTags: List<TagSearch>) {
        val mBuilder = NotificationCompat.Builder(context, "Main")
        if (notifyTags.count() == 1) {
            val notify = notifyTags.first()
            Log.i("MainRepository", "Notifying about nearby tag ${notify.macAddress}")

            mBuilder
                .setContentTitle("A person is looking for a beacon near you")
                .setContentText(notify.notification)
        } else {
            val notifyMacs = notifyTags.joinToString { it.macAddress }
            val notifyNotifications = notifyTags.joinToString(separator = "\n") { it.notification }
            Log.i("MainRepository", "Notifying about nearby tags $notifyMacs")

            mBuilder
                .setContentTitle("Someone is looking for multiple beacons near you")
                .setContentText(notifyNotifications)
        }

        val restoreViewIntent = Intent(context, MainActivity::class.java)
        val restoreIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            restoreViewIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        mBuilder
            .setContentIntent(restoreIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "Nearby notification"
            val channel = NotificationChannel(
                channelId,
                "Beacon nearby notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
            mBuilder.setChannelId(channelId)
        }

        notificationManager.notify(10, mBuilder.build())
    }

    private fun notifyMissingTag(context: Context, notifyTags: List<TagSearch>) {
        val mBuilder = NotificationCompat.Builder(context, "Main")


            val notifyMacs = notifyTags.joinToString { it.macAddress }

            Log.i("MainRepository", "Notifying about nearby tags $notifyMacs")

            mBuilder
                .setContentTitle("Tag Found")
                .setContentText("Your Missing tag  "+notifyMacs + " has found")


        val restoreViewIntent = Intent(context, MainActivity::class.java)
        val restoreIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            restoreViewIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        mBuilder
            .setContentIntent(restoreIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "Missing notification"
            val channel = NotificationChannel(
                channelId,
                "Tag Missing Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
            mBuilder.setChannelId(channelId)
        }

        notificationManager.notify(10, mBuilder.build())
    }
    /**
     * This method triggers on device that "owns" a tag that has been spotted by another device
     *
     * @param context Android context, needed to raise notifications
     * @param notifyTags Tags that have been found
     */
    private fun notifyOwnedTagHasBeenFound(context: Context, notifyTags: List<TagData>) {
        val mBuilder = NotificationCompat.Builder(context, "Main")
        if (notifyTags.count() == 1) {
            val tag = notifyTags.first()
            Log.i("MainRepository", "Notifying about found owned tag ${tag.macAddress}")
            mBuilder
                .setContentTitle("${tag.description} has been spotted!")
                .setContentText("${tag.description} was spotted, open locate dialog to see")
        } else {
            val tagMacs = notifyTags.joinToString { it.macAddress }
            val tagIds = notifyTags.joinToString { it.description }
            Log.i("MainRepository", "Notifying about found owned tags $tagMacs")
            mBuilder
                .setContentTitle("$tagIds have been spotted!")
                .setContentText("Multiple tags were spotted, open locate dialog to see")
        }
        val restoreViewIntent = Intent(context, MainActivity::class.java)
        val restoreIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            restoreViewIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "Tag spotted notification"
            val channel = NotificationChannel(
                channelId,
                "Tag spotted notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
            mBuilder.setChannelId(channelId)
        }

        mBuilder
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(restoreIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        notificationManager.notify(11, mBuilder.build())
    }

    /**
     * Given a text detects if it's a valid beacon short url
     * It fetches the long link from firebase and returns the
     * parsed url
     */
    suspend fun tryParseShortLinkAsync(text: String): TagData? =
        firebaseLinks.tryParseShortLinkAsync(text)

    /**
     * Generates a short link from for the given TagData
     */
    suspend fun generateLinkFromTagAsync(tag: TagData): String =
        firebaseLinks.generateLinkFromTagAsync(tag)






}
