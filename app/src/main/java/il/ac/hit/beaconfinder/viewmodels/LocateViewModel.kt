package il.ac.hit.beaconfinder.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * This represents the ViewModel for LocateFragment
 */
@HiltViewModel
class LocateViewModel @Inject constructor(
) : ViewModel(), LifecycleObserver {
    fun navigateWaze(context: Context, pos: LatLng) {
        val wazeUrl = "https://waze.com/ul?ll=${pos.latitude}%2C${pos.longitude}&amp;navigate=yes"
        val uri = Uri.parse(wazeUrl)

        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    fun navigateGoogleMaps(context: Context, pos: LatLng) {
        val googleMapsUrl = "google.navigation:q=${pos.latitude},${pos.longitude}"
        val uri = Uri.parse(googleMapsUrl)

        val googleMapsPackage = "com.google.android.apps.maps"
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(googleMapsPackage)
        }

        context.startActivity(intent)
    }
}
