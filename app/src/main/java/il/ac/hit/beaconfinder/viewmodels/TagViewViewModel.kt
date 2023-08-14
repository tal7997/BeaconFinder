package il.ac.hit.beaconfinder.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
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
class TagViewViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel(), LifecycleObserver {
    private val resolvedBitmap = MutableLiveData<Bitmap?>()

    /**
     * Returns LiveData representing the Tag's icon bitmap
     */
    fun getResolvedBitmap(): LiveData<Bitmap?> = resolvedBitmap

    /**
     * Resolves a bitmap from a url
     */
    fun resolveBitmap(context: Context, uri: String) {
        try {
            // suppress deprecation, when using android < 10 (less than Q)
            // use the old getBitmap, otherwise use decodeBitmap
            @Suppress("DEPRECATION") val bitmap = when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(uri))
                }
                else -> {
                    val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))
                    ImageDecoder.decodeBitmap(source)
                }
            }
            resolvedBitmap.postValue(bitmap)
        } catch (e: Exception) {
            // not a critical error, it might happen if image is not yet set up for this tag
            // so just log it with "verbose" severity
            Log.v("TagViewViewModel", "Failed fetching tag image from $uri", e)
            resolvedBitmap.postValue(null)
        }
    }

    /**
     * Crop the provided bitmap in such a way that the result is centered rectangular image
     */
    fun getCroppedBitmap(src: Bitmap): Bitmap {
        return if (src.width >= src.height) {
            Bitmap.createBitmap(src, src.width / 2 - src.height / 2, 0, src.height, src.height)
        } else {
            Bitmap.createBitmap(src, 0, src.height / 2 - src.width / 2, src.width, src.width)
        }
    }

    /**
     * Removes and then adds the tag
     * This effectively updates it without complex logic
     */
    suspend fun updateTag(tag: TagData) {
        repository.removeTag(tag)
        repository.addTag(tag)
    }

    /**
     * Navigates to locate fragment
     */
    fun navigateToLocateFragment(navController: NavController, tag: TagData) {
        navController.navigate(
            R.id.action_tagViewFragment_to_locateFragment,
            bundleOf("tag" to tag)
        )
    }

    /**
     * Navigates to expiry choice fragment ( one step before sharing )
     */
    fun navigateToExpiryChoiceFragment(navController: NavController, tag: TagData) {
        navController.navigate(
            R.id.action_tagViewFragment_to_expiryChoiceFragment,
            bundleOf("tag" to tag)
        )
    }

    /**
     * Removes a tag from local database and navigates back up the NavGraph stack
     */
    fun removeTag(navController: NavController, tag: TagData) {
        viewModelScope.launch {
            repository.removeTag(tag)
            navController.popBackStack()
        }
    }
}
