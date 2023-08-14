package il.ac.hit.beaconfinder.features_group.showGroupMemeber

data class GroupMember(
    val name: String = "",
    val isOnline: Boolean = false,
    var lastSeen: String = "",
    var uuid: String = "",
    var fcm : String = ""
)
