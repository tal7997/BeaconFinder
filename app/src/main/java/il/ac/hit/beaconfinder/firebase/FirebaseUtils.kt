package il.ac.hit.beaconfinder.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import il.ac.hit.beaconfinder.MainActivity
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.TagData
import il.ac.hit.beaconfinder.data.GroupEntity
import il.ac.hit.beaconfinder.feature_socialNetwork.FirebaseUserInfo
import il.ac.hit.beaconfinder.features_group.groupInvite
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This class represents the access point into firebase database
 */
class FirebaseUtils @Inject constructor() {

    companion object {
        private const val TAG = "FirebaseUtils"
        val macFeild: String = "mac"
    }



    private val db = Firebase.firestore
    private val searchPath: String = "TagsSearch"
    private val userPath: String = "TagsUser"
    private val pingPath: String = "TagsPing"
    private val groupInfoPath: String = "GroupInfo"
    private val groupInvitePath: String = "GroupInvite"


    /**
     * This method lets us do basic anonymous login to get past firebase access rules
     */
    fun signin(user: String, password: String, onCompleteListener: OnCompleteListener<AuthResult>) {
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(user, password)
            .addOnCompleteListener(onCompleteListener)
    }

    fun signup(email: String, password: String, userName: String,token : String,
               phoneNumber: String, onSuccessListener: OnSuccessListener<Void>,
                onFailureListener: OnFailureListener) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if(it.isSuccessful) {
                    val instance = FirebaseAuth.getInstance()
                    val uid = instance.uid!!
                    val fields = hashMapOf(
                            UserUtils.UID to uid,
                    UserUtils.EMAIL to email,
                    UserUtils.USERNAME to userName,
                    UserUtils.PHONE_NUMBER to phoneNumber,
                    UserUtils.FCM_TOKEN to token,
                    UserUtils.GROUP_INFO to "",
                    UserUtils.CREATED_DATE to Date().time.toString()
                    )
                    db.collection(userPath)
                        .document(uid)
                        .set(fields, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.i("FirebaseUtils", "User with uid $uid was added")
                            onSuccessListener.onSuccess(null)
                        }
                        .addOnFailureListener { e ->
                            Log.w("FirebaseUtils", "Error adding user with uid $uid", e)
                            FirebaseAuth.getInstance().currentUser?.delete()
                            onFailureListener.onFailure(e)
                        }

                } else {
                    onFailureListener.onFailure(it.exception?: Exception("Unspecified exception"))
                }
            }

    }

    /**
     * This method retrieves a list of searched tags from firebase
     */
    suspend fun getSearchedTags(): List<TagSearch> {
        val oneWeekAgo =
            Date.from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant())
        val tagMacList = ArrayList<TagSearch>()
        db.collection(searchPath)
            .whereGreaterThan("date", Timestamp(oneWeekAgo))
            .get()
            .addOnSuccessListener { result ->
                tagMacList.addAll(result.map {
                    TagSearch(
                        it.reference.id,

//                        (it["notification"] as String?).orEmpty()
                        (it["notification"] as String?).orEmpty(),
                        (it["uid"] as String?).orEmpty()

                    )
                })

                Log.d("FirebaseUtils", "Received tagMacList with ${tagMacList.size} elements")
            }
            .addOnFailureListener { exception ->
                Log.w("FirebaseUtils", "Error getting a tag mac list.", exception)
            }
            .await()
        return tagMacList
    }

    /**
     * This method gets a list of pings for specific mac address
     * each ping represents a geopoint and a time it was located at
     */
    suspend fun getPingsForMac(macAddress: String): List<TagPing> {
        Log.i("FirebaseUtils", "getPingsForMac($macAddress) called")
        val pingList = ArrayList<TagPing>()

        // limit to samples from 2 days
        val timeLimit =
            Date.from(LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant())

        db.collection(pingPath)
            .whereEqualTo(macFeild, macAddress)
            .whereGreaterThan("date", Timestamp(timeLimit))
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { result ->
                pingList.addAll(result.map { it.toObject(TagPing::class.java) })
                Log.i("FirebaseUtils", "Received ping list with ${pingList.size} elements")
            }
            .addOnFailureListener { exception ->
                Log.w("FirebaseUtils", "Error getting ping list.", exception)
            }
            .await()

        pingList.sortByDescending { it.date } // sort descending for reverse chronological order

        var deletedItems = 0
        // now we filter out all points which are too close to previous point, most likely
        // this is 2 devices detecting same beacon, or multiple detections by same device and
        // GPS location jitter
        for (i in 0 until pingList.count()) {
            if (i >= pingList.count() - 1)
                break

            val curr = pingList[i]
            val next = pingList[i + 1]

            val distanceResults = FloatArray(1)
            Location.distanceBetween(
                curr.location.latitude, curr.location.longitude,
                next.location.latitude, next.location.longitude,
                distanceResults
            )
            val distance = distanceResults[0]
            val minDistance = 40f
            if (distance < minDistance) {
                deletedItems++
                pingList.removeAt(i + 1)
            }
        }

        pingList.sortBy { it.date } // sort chronologically
        if (deletedItems > 0) {
            Log.d(
                "FirebaseUtils",
                "getPingsForMac() returning ${pingList.count()} (removed $deletedItems)"
            )
        }
        return pingList
    }

    /**
     * This method adds a list of tag pings into firebase
     * This filters out tags without specified location
     * and also filters out all but the last detection,
     * so that we don't spam the database in case we're near the tag for a long time
     */
    suspend fun addPings(tags: List<TagPing>): Boolean {
        Log.i("FirebaseUtils", "addPings(${tags.count()}) called")
        val tagsFiltered =
            tags.asSequence()
                .filter { it.location.latitude != 0.0 || it.location.longitude != 0.0 }
                .sortedByDescending { it.date }
                .groupBy { it.macAddress }
                .map { it.value.first() } // upload only the last sample
                .toList()
        val countDiff = tags.count() - tagsFiltered.count()
        if (countDiff != 0) {
            Log.i("FirebaseUtils", "addPings(${tags.count()}) filtered out $countDiff")
        }
        if (tagsFiltered.none()) {
            return true
        }

        val batch = db.batch()
        var count = 0
        tagsFiltered.forEach {
            val docRef = db.collection(pingPath).document() // automatically generate unique id
            batch.set(docRef, it)
            count++
        }
        var result = false
        batch.commit()
            .addOnSuccessListener {
                Log.i("FirebaseUtils", "$count tags inserted into firebase")
                result = true
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseUtils", "Error adding tag pings", e)
                result = false
            }
            .await()
        return result
    }

    /**
     * This method adds a snapshot listener into firebase context
     */
    fun addSnapshotListener(listener: EventListener<QuerySnapshot>): ListenerRegistration {
        return db.collection(pingPath)
            .addSnapshotListener(listener)
    }

    /**
     * This method removes a snapshot listener from firebase context
     */
    fun removeSnapshotListener(listenerRegistration: ListenerRegistration) {
        listenerRegistration.remove()
    }

    /**
     * Method that adds a tag to searched tags, optionally it adds notification
     * such that a device that detects the tag will also display this notification
     */
    suspend fun addTagToSearch(macAddress: String, notification: String) {
        Log.i("FirebaseUtils", "addTagToSearch($macAddress) called")
        val instance = FirebaseAuth.getInstance()
        val uid = instance.uid!!

        val fields = hashMapOf(
            "date" to FieldValue.serverTimestamp(),
            "macAddress" to macAddress,
            "notification" to notification,
            "uid" to uid,
        )
        if (notification.isBlank()) {
            fields.remove("notification")
        }
        db.collection(searchPath)
            .document(macAddress)
            .set(fields, SetOptions.merge())
            .addOnSuccessListener {
                Log.i("FirebaseUtils", "Tag with MAC address: $macAddress was added to search")
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseUtils", "Error adding tag with mac $macAddress", e)
            }
            .await()
        Log.i("FirebaseUtils", "Finished adding tag with mac $macAddress")
    }


    suspend fun removeTagToSearch(macAddress: String) {
        val instance = FirebaseAuth.getInstance()
        val uid = instance.uid!!
        Log.i("FirebaseUtils", "removeTagToSearch($macAddress, $uid) called")

        db.collection(searchPath)
            .whereEqualTo("uid", uid)
            .whereEqualTo("macAddress", macAddress)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val documentUid = document.getString("uid")
                    if (documentUid == uid) {
                        document.reference.delete()
                        Log.i("FirebaseUtils", "Tag with MAC address: $macAddress and uid: $uid was removed from search")
                    } else {
                        Log.i("FirebaseUtils", "Tag with MAC address: $macAddress is not owned by uid: $uid. Removal denied.")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseUtils", "Error removing tag with MAC address: $macAddress and uid: $uid", e)
            }
            .await()
        Log.i("FirebaseUtils", "Finished removing tag with MAC address: $macAddress and uid: $uid")
    }


    suspend fun isTagPresent(macAddress: String): Boolean {
        val instance = FirebaseAuth.getInstance()
        val uid = instance.uid!!
        Log.i("FirebaseUtils", "isTagPresent($macAddress, $uid) called")

        val querySnapshot = db.collection(searchPath)
            .whereEqualTo("uid", uid)
            .whereEqualTo("macAddress", macAddress)
            .get()
            .await()

        val isPresent = !querySnapshot.isEmpty

        Log.i("FirebaseUtils", "Tag with MAC address: $macAddress and uid: $uid is present: $isPresent")

        return isPresent
    }
    suspend fun isOtherTagPresent(context: Context,macAddress: String): Boolean {
        val instance = FirebaseAuth.getInstance()
        val uid = instance.uid!!
        Log.i("FirebaseUtils", "isTagPresent($macAddress, $uid) called")

        val querySnapshot = db.collection(searchPath)
           .whereEqualTo("notification", "Missing")
            .whereEqualTo("macAddress", macAddress)
            .get()
            .await()

        val isPresent = !querySnapshot.isEmpty
        querySnapshot.query.get()
        if(isPresent) {
            for (document in querySnapshot.documents) {
//                val id = document.getString("uid")
//                notifyOwnedTagHasBeenFound(context,macAddress,id.toString())
                val id = document.getString("uid")
                val documentRef = document.reference
                val data = hashMapOf(
                    "notification" to "Found"
                )

                documentRef.update(data as Map<String, Any>)
                    .addOnSuccessListener {
                        Log.i("FirebaseUtils", "Notification updated successfully")
                        notifyOwnedTagHasBeenFound(context, macAddress, id.toString())
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseUtils", "Failed to update notification: ${e.message}")
                    }
                    .await()
                // Process the retrieved value as needed
            }
            Log.e(TAG, "isTagPresent: "+macAddress)

        }else{
            Log.e(TAG, "isTagNotPresent: "+macAddress)

        }
        Log.i("FirebaseUtils", "Tag with MAC address: $macAddress and uid: $uid is present: $isPresent")

        return isPresent
    }

    private fun notifyOwnedTagHasBeenFound(context: Context,macAddress: String,id:String) {
        val mBuilder = NotificationCompat.Builder(context, "Main")

            val notifyMacs =macAddress
            val notifyNotifications ="Tag has Found"
            Log.i("MainRepository", "Notifying about nearby tags $notifyMacs")

            mBuilder
                .setContentTitle(notifyNotifications)
                .setContentText("Missing tag  "+macAddress + "  of   "+id+"  has found")


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


    /*    fun getCurrentUserdata(callback: (FirebaseUserInfo?) -> Unit) {
            Log.d(TAG, "getCurrentUserdata() called")

            val instance = FirebaseAuth.getInstance()
            val uid = instance.uid!!

            db.collection(userPath).document(uid)
                .get()
                .addOnSuccessListener { result: DocumentSnapshot ->

                    val uid = result.getString(UserUtils.UID) ?: ""
                    val email = result.getString(UserUtils.EMAIL) ?: ""
                    val userName = result.getString(UserUtils.USERNAME) ?: ""
                    val phoneNumber = result.getString(UserUtils.PHONE_NUMBER) ?: ""
                    val fcmToken = result.getString(UserUtils.FCM_TOKEN) ?: ""
                    val groupinfo = result.getString(UserUtils.GROUP_INFO) ?: ""
                    val createdDate = result.getString(UserUtils.CREATED_DATE) ?: ""


                    val firebaseUserInfo = FirebaseUserInfo(
                        uid,
                        email,
                        userName,
                        phoneNumber,
                        fcmToken,
                        groupinfo ,
                                createdDate.toLongOrNull() ?: 0
                    )

                    callback(firebaseUserInfo)
                }
                .addOnFailureListener { e ->
                    Log.w("FirebaseUtils", "Error adding user with uid $uid", e)
                    callback(null)
                }
        }*/




    suspend fun getGroupFromId(groupEntityIdList: String): GroupEntity? {
        return suspendCoroutine { continuation ->
            db.collection(groupInfoPath)
                .document(groupEntityIdList)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {

                        val group: GroupEntity? = documentSnapshot.toObject(GroupEntity::class.java)

                        if (group != null) {
                            continuation.resume(group.copy(groupFirebaseId = documentSnapshot.id))
                        } else {
                            continuation.resume(null)
                        }


                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resume(null)
                }
        }
    }


    suspend fun updateGroup(groupEntityId: String, updatedGroup: GroupEntity): Boolean {
        return suspendCoroutine { continuation ->
            db.collection(groupInfoPath)
                .document(groupEntityId)
                .set(updatedGroup)
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener { exception ->
                    continuation.resume(false)
                }
        }
    }



    suspend fun getCurrentUserdata(): FirebaseUserInfo? {
        Log.d(TAG, "getCurrentUserdata() called")
        val instance = FirebaseAuth.getInstance()
        val uid = instance.uid!!
        return try {


            val result = db.collection(userPath).document(uid)
                .get()
                .await() // Wait for the document retrieval to complete

            val uid_value = result.getString(UserUtils.UID) ?: ""
            val email = result.getString(UserUtils.EMAIL) ?: ""
            val userName = result.getString(UserUtils.USERNAME) ?: ""
            val phoneNumber = result.getString(UserUtils.PHONE_NUMBER) ?: ""
            val fcmToken = result.getString(UserUtils.FCM_TOKEN) ?: ""
            val groupinfo = result.getString(UserUtils.GROUP_INFO) ?: ""
            val createdDate = result.getString(UserUtils.CREATED_DATE) ?: ""

            val firebaseUserInfo = FirebaseUserInfo(
                uid_value,
                email,
                userName,
                phoneNumber,
                fcmToken,
                groupinfo,
                createdDate.toLongOrNull() ?: 0
            )

            firebaseUserInfo // Return the FirebaseUserInfo object
        } catch (e: Exception) {
            Log.w("FirebaseUtils", "Error adding user with uid $uid", e)
            null // Return null in case of failure
        }
    }


    suspend fun createGroup(group: GroupEntity): GroupEntity? = suspendCoroutine { continuation ->
        db.collection(groupInfoPath)
            .add(group)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val groupId = task.result?.id
                    val groupWithFirebaseId = groupId?.let {
                        group.copy(groupFirebaseId = it)
                    }
                    continuation.resume(groupWithFirebaseId)
                } else {
                    continuation.resume(null)
                }
            }
    }


    suspend fun addGroupIdToUserInfo(newGroupId: String): Boolean {
        val currentUser: FirebaseUserInfo? = getCurrentUserdata()
        val gson = Gson()

        return if (currentUser != null) {
            val updatedGroup: Array<String> = if (currentUser.groupinfo.isNotEmpty()) {
                val arrayTutorialType = object : TypeToken<Array<String>>() {}.type

                val groupEntityIdList: Array<String> =
                    gson.fromJson(currentUser.groupinfo, arrayTutorialType)

                groupEntityIdList.toMutableList().apply {
                    add(newGroupId)
                }.toTypedArray()
            } else {
                arrayOf(newGroupId)
            }

            // Update the groupinfo field in the user document
            val instance = FirebaseAuth.getInstance()
            val uid = instance.uid!!
            val userDocumentRef = db.collection(userPath).document(uid)
            userDocumentRef.update(UserUtils.GROUP_INFO, gson.toJson(updatedGroup))
                .await() // Wait for the update to complete

            true // Return true after updating
        } else {
            false // Return false if currentUser is null
        }
    }


/*    suspend fun addMissingMacAddress(mac: String): Boolean {

        Log.d(TAG, "addMissingMacAddress() called with: mac = $mac")

        val instance = FirebaseAuth.getInstance()
        val currentUser = instance.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            val userDocumentRef = db.collection(userPath).document(uid)

            userDocumentRef.update("mac", mac)
                .await() // Wait for the update to complete

            return true // Return true after updating
        } else {
            return false // Return false if currentUser is null
        }
    }*/


    suspend fun isMacAddressPresent(): Boolean = suspendCoroutine { continuation ->
        val instance = FirebaseAuth.getInstance()
        val currentUser = instance.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            val userDocumentRef = db.collection(userPath).document(uid)

            userDocumentRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    val macAddress = documentSnapshot.getString("mac")
                    val isPresent = !macAddress.isNullOrEmpty()
                    continuation.resume(isPresent)
                }
                .addOnFailureListener { e ->
                    continuation.resume(false) // Return false if an error occurs
                }
        } else {
            continuation.resume(false) // Return false if currentUser is null
        }
    }

/*    suspend fun deleteMacAddress(): Boolean {
        val instance = FirebaseAuth.getInstance()
        val uid = instance.currentUser?.uid

        if (uid != null) {
            val userDocumentRef = db.collection(userPath).document(uid)

            val updateData = hashMapOf<String, Any>(
                "mac" to FieldValue.delete()
            )

            return try {
                userDocumentRef.update(updateData).await()
                true // Return true if the delete operation succeeds
            } catch (e: Exception) {
                false // Return false if the delete operation fails
            }
        }

        return false // Return false if currentUser is null
    }*/



    suspend fun invitePeople(groupInvite : groupInvite): groupInvite? = suspendCoroutine { continuation ->
        db.collection(groupInvitePath)
            .add(groupInvite)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val groupId = task.result?.id
                    val groupWithFirebaseId = groupId?.let {
                        groupInvite
                    }
                    continuation.resume(groupWithFirebaseId)
                } else {
                    continuation.resume(null)
                }
            }
    }



    suspend fun getRecordForPhone(memberPhone: String): Pair<String, groupInvite?> = suspendCoroutine { continuation ->
        db.collection(groupInvitePath)
            .whereEqualTo("memberPhone", memberPhone)
            .limit(1)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    if (!querySnapshot.isEmpty) {
                        val documentSnapshot = querySnapshot.documents[0]
                        val data = documentSnapshot.data
                        val createTime = data?.get("createTime") as? Long
                        val groupId = data?.get("groupId") as? String
                        if (createTime != null && groupId != null) {
                            val groupIdss = documentSnapshot.id
                            val groupInvite = groupInvite(createTime, memberPhone, groupId)
                            continuation.resume( groupIdss to groupInvite)
                        } else {
                            continuation.resume( "" to null)
                        }
                    } else {
                        continuation.resume("" to null)
                    }
                } else {
                    continuation.resume("" to null)
                }
            }
    }
    suspend fun deleteRecord(documentId: String): Boolean = suspendCoroutine { continuation ->
        val documentReference = db.collection(groupInvitePath).document(documentId)

        documentReference.delete()
            .addOnCompleteListener { deleteTask ->
                if (deleteTask.isSuccessful) {
                    Log.d("DeleteRecord", "Document deletion successful: $documentId")
                    continuation.resume(true)
                } else {
                    val exception = deleteTask.exception
                    if (exception != null) {
                        Log.e("DeleteRecord", "Error deleting document: $documentId", exception)
                    } else {
                        Log.e("DeleteRecord", "Error deleting document: $documentId")
                    }
                    continuation.resume(false)
                }
            }
    }




    suspend fun checkforthephonenumber()
    {
       var CurrentUserdata =  getCurrentUserdata()


    }


    suspend fun getGroupInviteByMemberPhone(memberPhone: String): Pair<groupInvite?, String?> = suspendCoroutine { continuation ->
        db.collection(groupInvitePath)
            .whereEqualTo("memberPhone", memberPhone)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val groupInvite = document.toObject(groupInvite::class.java)
                        val groupId = document.id
                        continuation.resume(Pair(groupInvite, groupId))
                    } else {
                        continuation.resume(Pair(null, null))
                    }
                } else {
                    continuation.resume(Pair(null, null))
                }
            }
    }

    


}
