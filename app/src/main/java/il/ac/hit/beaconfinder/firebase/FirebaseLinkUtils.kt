package il.ac.hit.beaconfinder.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.ktx.Firebase
import il.ac.hit.beaconfinder.TagData
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject

/**
 * This class represents the access point into firebase database
 */
class FirebaseLinkUtils @Inject constructor() {
    /**
     * Static fields for base urls etc
     */
    companion object {
        const val baseUrl = "https://bf.karmaflux.com/#"
        const val shortLinkPrefix = "https://bfdr.page.link/"
    }

    /**
     * Returns true if link provided in text parameter is a valid beacon link
     */
    private fun isBeaconShortLink(text: String): Boolean {
        return text.startsWith(shortLinkPrefix)
    }

    /**
     * Returns true if link provided in text parameter is a valid beacon long link
     */
    private fun isBeaconLongLink(text: String): Boolean {
        return text.startsWith(baseUrl)
    }

    /**
     * Given a text detects if it's a valid beacon short url
     * It fetches the long link from firebase and returns the
     * parsed url
     */
    suspend fun tryParseShortLinkAsync(text: String): TagData? {
        if (!isBeaconShortLink(text)) {
            return null
        }

        val dynamicLink = Firebase.dynamicLinks.getDynamicLink(Uri.parse(text))
            .await()

        return tryParseLongLinkAsync(dynamicLink.link.toString())
    }

    /**
     * Given a text detects if it's a valid beacon long url
     * It returns the TagData from parsed url
     */
    fun tryParseLongLinkAsync(text: String): TagData? {
        if (!isBeaconLongLink(text)) {
            return null
        }

        val (url, fragment) = text
            .split('#', ignoreCase = false, limit = 2)

        if (url != baseUrl.substring(0, baseUrl.length - 1)) {
            Log.i(
                "FirebaseLinkUtils",
                "No base url '$baseUrl' contained in scanned QR text"
            )
            return null
        }

        try {
            val decodedLink = URLDecoder.decode(fragment, "utf-8")
            val tag = TagData.fromString(decodedLink)
            if (tag.macAddress.isNotBlank()) {
                return tag
            }
        } catch (e: Exception) {
            Log.w(
                "FirebaseLinkUtils",
                "Couldn't parse valid TagData from link string."
            )
        }
        return null
    }

    /**
     * Generates a short link for the given TagData
     */
    suspend fun generateLinkFromTagAsync(tag: TagData): String {
        val data = baseUrl + URLEncoder.encode(tag.toString(), "utf-8")
        val shortLinkTask =
            Firebase.dynamicLinks.shortLinkAsync(ShortDynamicLink.Suffix.UNGUESSABLE)
            {
                link = Uri.parse(data)
                domainUriPrefix = shortLinkPrefix
                androidParameters { }
            }

        return shortLinkTask.await().shortLink.toString()
    }
}
