package il.ac.hit.beaconfinder.viewmodels

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import il.ac.hit.beaconfinder.TagData
import il.ac.hit.beaconfinder.data.MainRepository
import javax.inject.Inject

/**
 * This represents the ViewModel
 * The viewModel allows to grab MutableLiveData representations for a few of the items needed
 * for UI, and interface into local database to add/remove/change local "owned" tags.
 */
@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel(), LifecycleObserver {
    /**
     * Generates a QR bitmap for provided string
     */
    fun generateQR(data: String): Bitmap {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            return bmp
        } catch (e: Exception) {
            Log.e("ShareFragment", e.toString())
            return Bitmap.createBitmap(16, 16, Bitmap.Config.RGB_565)
        }
    }

    /**
     * Generates a short link from for the given TagData
     */
    suspend fun generateLinkFromTagAsync(tag: TagData): String =
        repository.generateLinkFromTagAsync(tag)
}
