package il.ac.hit.beaconfinder.features_group.group_chat

data class GroupChatMessage(
    val centerName: String = "",
    val timestamp: Long = 0L,
    val message: String = "",
    val uuid : String = ""
)
