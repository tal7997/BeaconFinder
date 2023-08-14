package il.ac.hit.beaconfinder.viewmodels

import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.TagData
import il.ac.hit.beaconfinder.data.MainRepository
import org.altbeacon.beacon.Beacon
import java.util.*
import javax.inject.Inject

/**
 * This represents the TagConnect fragment's ViewModel
 */
@HiltViewModel
class TagConnectViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel(), LifecycleObserver {
    /**
     * Navigates to TagViewFragment
     */
    fun viewTag(navController: NavController, tag: TagData) {
        navController.popBackStack(R.id.mainFragment, false)
        navController.navigate(R.id.action_mainFragment_to_tagViewFragment, bundleOf("tag" to tag))
    }

    /**
     * Gets nearby beacons from most recent bluetooth scan
     */
    fun getNearbyBeacons(): LiveData<List<Beacon>> {
        return repository.getNearbyBeacons()
    }

    /**
     * Adds tag to local database
     */
    suspend fun addTag(tag: TagData) {
        repository.addTag(tag)
    }

    /**
     * This method fetches the list of "owned" tags from local database
     */
    suspend fun getTags(): List<TagData> = Collections.unmodifiableList(repository.getTags())
}
