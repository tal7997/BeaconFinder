package il.ac.hit.beaconfinder.viewmodels

import androidx.core.os.bundleOf
import androidx.lifecycle.*
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.TagData
import il.ac.hit.beaconfinder.data.MainRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This represents the ViewModel
 * The viewModel allows to grab MutableLiveData representations for a few of the items needed
 * for UI, and interface into local database to add/remove/change local "owned" tags.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {
    /**
     * Gets LiveData representing beacons registered in local database
     */
    fun getRegisteredBeacons() = repository.getRegisteredBeacons()

    /**
     * Starts a coroutine that fetches beacons registered in local database
     */
    fun fetchRegisteredBeacons() {
        viewModelScope.launch { repository.fetchRegisteredBeacons() }
    }

    /**
     * Returns a LiveData representing the tags currently searched for
     */
    fun getSearchedForTags(): LiveData<List<String>> = repository.getSearchedForTags()

    /**
     * Navigates to TagView fragment
     */
    fun viewTag(navController: NavController, tag: TagData) {
        navController.navigate(
            R.id.action_mainFragment_to_tagViewFragment,
            bundleOf("tag" to tag)
        )
    }

    /**
     * Navigates to add new tag fragment
     */
    fun addNewTag(navController: NavController) {
        navController.navigate(R.id.action_mainFragment_to_tagConnectFragment)
    }
}
