package il.ac.hit.beaconfinder.viewmodels

import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.TagData
import javax.inject.Inject

/**
 * This represents the ExpiryChoiceViewModel
 */
@HiltViewModel
class ExpiryChoiceViewModel @Inject constructor(
) : ViewModel(), LifecycleObserver {
    /**
     * Navigates to share fragment which expects tag and expiry variable
     */
    fun nextStep(navController: NavController, tag: TagData, hoursToKeep: Int) {
        navController.popBackStack() // remove own fragment from backstack
        navController.navigate(
            R.id.action_tagViewFragment_to_shareFragment,
            bundleOf(
                "tag" to tag,
                "hoursToKeep" to hoursToKeep
            )
        )
    }
}
