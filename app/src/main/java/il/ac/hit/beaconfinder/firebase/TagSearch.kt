package il.ac.hit.beaconfinder.firebase

/**
 * Class that represents a tag that is searched for in firebase, has optional notification
 */
//data class TagSearch(val macAddress: String, val notification: String)
data class TagSearch(val macAddress: String, val notification: String,val uid:String)