package il.ac.hit.beaconfinder.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,  // changed from String to Int for autoGenerate

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "ownerInfo")
    val ownerInfo: String = "",

    @ColumnInfo(name = "members")
    val members: String = "",

    @ColumnInfo(name = "groupFirebaseId")
    val groupFirebaseId: String = "",

    @ColumnInfo(name = "groupCreatedDate")
    val groupCreatedDate: Long = 0,

    @ColumnInfo(name = "realtimeId")
    val realtimeId: String = "",

    @ColumnInfo(name = "displayMessage")
    val displayMessage: String = "",

    @ColumnInfo(name = "displayUsername")
    val displayUsername: String = "",

    @ColumnInfo(name = "lastMessageDate")
    val lastMessageDate : Long = 0
)

