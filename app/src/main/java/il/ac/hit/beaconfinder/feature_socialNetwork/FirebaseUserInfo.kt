package il.ac.hit.beaconfinder.feature_socialNetwork

data class FirebaseUserInfo(
    val uid : String = "",
    val email : String = "",
    val userName : String = "",
    val phoneNumber : String = "",
    val fcmToken : String = "",
    val groupinfo : String = "" , // save the group as String format
  val createDate : Long
)
