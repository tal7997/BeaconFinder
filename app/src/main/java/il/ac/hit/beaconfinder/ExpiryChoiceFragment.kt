package il.ac.hit.beaconfinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import il.ac.hit.beaconfinder.databinding.FragmentExpiryChoiceBinding
import il.ac.hit.beaconfinder.viewmodels.ExpiryChoiceViewModel

/**
 * Fragment to choose the expiry time when sharing a tag
 */
class ExpiryChoiceFragment : Fragment() {
    private val viewModel by viewModels<ExpiryChoiceViewModel>()

    private lateinit var tag: TagData
    private lateinit var binding: FragmentExpiryChoiceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { x -> tag = x.get("tag") as TagData }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentExpiryChoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnInterval12Hours
            .setOnClickListener { viewModel.nextStep(findNavController(), tag, 12) }

        binding.btnInterval48Hours
            .setOnClickListener { viewModel.nextStep(findNavController(), tag, 48) }

        binding.btnIntervalWeek
            .setOnClickListener { viewModel.nextStep(findNavController(), tag, 24 * 7) }

        binding.btnIntervalPermanent
            .setOnClickListener { viewModel.nextStep(findNavController(), tag, 50 * 365 * 24) }
    }
}
