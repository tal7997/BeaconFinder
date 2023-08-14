package il.ac.hit.beaconfinder.viewmodels

import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.zxing.client.android.BeepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.TagData
import il.ac.hit.beaconfinder.data.MainRepository
import javax.inject.Inject

/**
 * This represents the QRScanner fragment's ViewModel
 */
@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel(), LifecycleObserver {
    private lateinit var beepManager: BeepManager
    private val statusText: MutableLiveData<String> = MutableLiveData("")
    private var lastQrScanResult: String = ""

    /**
     * Gets LiveData representing the scan status for QrScanner status text
     */
    fun getStatusText(): LiveData<String> = statusText

    /**
     * Gets LiveData representing beacons registered in local database
     */
    private fun getRegisteredBeacons() = repository.getRegisteredBeacons()

    fun setBeepManager(beepManager: BeepManager) {
        this.beepManager = beepManager
    }

    /**
     * Starts a coroutine that fetches beacons registered in local database
     */
    suspend fun handleScanResult(text: String): TagData? {
        if (text == lastQrScanResult) {
            // Prevent duplicate scans
            return null
        }

        lastQrScanResult = text

        val tag = repository.tryParseShortLinkAsync(text) ?: return null
        // if execution reaches here we detected a valid tag link
        beepManager.playBeepSoundAndVibrate()

        if (getRegisteredBeacons().value?.any { it.macAddress == tag.macAddress } == true) {
            statusText.postValue("Tag with MAC ${tag.macAddress} already exists")
            return null
        }

        Log.i("QrScannerFragment", "Detected QR: $text")
        repository.addTag(tag)
        return tag
    }

    /**
     * Navigates to TagView
     */
    fun navigateToTagView(navController: NavController, tag: TagData) {
        navController.popBackStack()
        navController.popBackStack()
        navController.navigate(R.id.action_mainFragment_to_tagViewFragment, bundleOf("tag" to tag))
    }
}
