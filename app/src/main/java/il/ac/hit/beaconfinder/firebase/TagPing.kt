package il.ac.hit.beaconfinder.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * Class that represents a firebase tag ping ( an incident where a tag has been spotted by bluetooth )
 */
class TagPing {
    var macAddress: String = ""
    var location: GeoPoint = GeoPoint(0.0, 0.0)
    var date: Timestamp = Timestamp.now()
}
