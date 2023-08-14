package il.ac.hit.beaconfinder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import il.ac.hit.beaconfinder.databinding.FragmentShareBinding
import il.ac.hit.beaconfinder.firebase.FirebaseLinkUtils
import il.ac.hit.beaconfinder.viewmodels.ShareViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class ShareFragment : Fragment() {
    private val viewModel by viewModels<ShareViewModel>()
    private lateinit var tag: TagData
    private lateinit var binding: FragmentShareBinding
    private var hoursToKeep: Int = 0

    @Inject
    lateinit var firebaseLinks: FirebaseLinkUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { x ->
            tag = x.get("tag") as TagData
            hoursToKeep = x.get("hoursToKeep") as Int
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val expiresAt = Instant.now().plusSeconds((hoursToKeep * 60 * 60).toLong())
        val tagWithExpiry = tag.copy(expiresAt = expiresAt)

        lifecycleScope.launch {
            val data = viewModel.generateLinkFromTagAsync(tagWithExpiry)

            binding.shareQRImageView.setImageBitmap(viewModel.generateQR(data))
            binding.progressBarSpinner.visibility = View.INVISIBLE
            binding.shareQRImageView.visibility = View.VISIBLE
            binding.btnShareLink.visibility = View.VISIBLE
            binding.btnShareLink.setOnClickListener {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, data)
                    type = "text/plain"
                }

                val shareIntent = Intent.createChooser(sendIntent, "Share link for tag")
                startActivity(shareIntent)
            }
        }
    }
}