package il.ac.hit.beaconfinder

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import il.ac.hit.beaconfinder.databinding.FragmentTagViewBinding
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import il.ac.hit.beaconfinder.viewmodels.TagViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class TagViewFragment : Fragment() {
    private val viewModel by viewModels<TagViewViewModel>()

    private lateinit var binding: FragmentTagViewBinding
    private lateinit var tag: TagData
    private lateinit var startCameraPermissionForResult: ActivityResultLauncher<String>
    private lateinit var startCameraActivityForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { x -> tag = x.get("tag") as TagData }
        startCameraActivityForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    val camBitmap = it.data?.extras?.get("data") as Bitmap?
                    if (camBitmap == null) {
                        Log.e("TagViewFragment", "Camera intent returned null extras data")
                        return@registerForActivityResult
                    }

                    val croppedBitmap = viewModel.getCroppedBitmap(camBitmap)
                    val bitmap = Bitmap.createScaledBitmap(croppedBitmap, 256, 256, false)
                    binding.ivIcon.setImageBitmap(bitmap)
                    val filename = tag.macAddress.replace(':', '_') + ".jpg"
                    val iconFile = File(requireContext().filesDir, filename)
                    try {
                        FileOutputStream(iconFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    } catch (e: IOException) {
                        Log.e("TagViewFragment", "Failed writing to " + iconFile.absolutePath, e)
                    }

                    tag.icon = Uri.fromFile(iconFile).toString()
                    lifecycleScope.launch {
                        viewModel.updateTag(tag)
                    }
                }
            }
        startCameraPermissionForResult =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isPermissionAccepted ->
                if (isPermissionAccepted) {
                    startCameraCaptureActivity()
                } else {
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_LONG)
                        .show()
                }
            }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTagViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvMacAddress.text = context?.getString(R.string.macAddress, tag.macAddress)
        binding.editTextTagName.setText(tag.description, TextView.BufferType.EDITABLE)

        if (tag.expiresAt != null) {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
            binding.tvExpiresAt.text =
                context?.getString(R.string.expiryLabel, formatter.format(tag.expiresAt))
        } else {
            binding.tvExpiresAt.visibility = View.INVISIBLE
        }

        checkMacAddressPresence(binding.toggleButton)


        binding.toggleButton.isChecked

        binding.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                binding.textStatus.text = "Tag Found"

                lifecycleScope.launch(Dispatchers.IO) {
                    FirebaseUtils().removeTagToSearch(tag.macAddress)
                }
            } else {
                binding.textStatus.text = "Tag Missing"
                lifecycleScope.launch(Dispatchers.IO) {
//                    FirebaseUtils().addMissingMacAddress(tag.macAddress)
                    FirebaseUtils().addTagToSearch(tag.macAddress, "Missing")

                    //todo
                }

            }
        }


        updateBatteryText()

        if (tag.distance.isInfinite() || tag.distance.isNaN()) {
            binding.tvDistanceEstimate.text =
                context?.getString(R.string.distanceUnknown_prefixed)
        } else {
            binding.tvDistanceEstimate.text =
                context?.getString(R.string.distanceEstimate_prefixed, tag.distance)
        }

        binding.btnTagLocate.setOnClickListener {
            viewModel.navigateToLocateFragment(findNavController(), tag)
        }

        binding.btnTagShare.setOnClickListener {
            viewModel.navigateToExpiryChoiceFragment(findNavController(), tag)
        }

        binding.btnTagDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.deleteTagDialogMessage)
                .setTitle(getString(R.string.deleteTagDialogTitle, tag.description))
                .setIcon(R.drawable.ic_baseline_delete_forever_24)
                .setCancelable(false)
                .setPositiveButton(R.string.deleteButton) { _, _ ->
                    viewModel.removeTag(findNavController(), tag)
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
                .getButton(DialogInterface.BUTTON_POSITIVE)
                .requestFocus()
        }

        binding.editTagNameButton.setOnClickListener {
            val input = EditText(requireContext())
            input.inputType = InputType.TYPE_CLASS_TEXT
            input.filters = arrayOf<InputFilter>(LengthFilter(21))
            input.text = SpannableStringBuilder(tag.description)
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.editNameDialogMessage)
                .setTitle(R.string.editNameDialogMessage)
                .setView(input)
                .setIcon(R.drawable.ic_baseline_perm_identity_24)
                .setCancelable(false)
                .setPositiveButton(R.string.saveButton) { _, _ ->
                    lifecycleScope.launch {
                        binding.editTextTagName.setText(input.text.toString())
                        tag.description = input.text.toString()
                        viewModel.updateTag(tag)
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
                .getButton(DialogInterface.BUTTON_POSITIVE)
                .requestFocus()

        }

        viewModel.getResolvedBitmap().observe(viewLifecycleOwner) {
            if (it != null) {
                binding.ivIcon.setImageBitmap(it)
            } else {
                binding.ivIcon.setImageResource(R.drawable.ic_baseline_error_24)
            }
        }
        viewModel.resolveBitmap(requireContext(), tag.icon)

        binding.ivIcon.setOnClickListener {
            if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                startCameraPermissionForResult.launch(Manifest.permission.CAMERA)
            } else {
                startCameraCaptureActivity()
            }
        }

        binding.btnBatterySwap.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.batteryDialogMessage)
                .setTitle(R.string.batteryDialogTitle)
                .setIcon(R.drawable.ic_baseline_perm_identity_24)
                .setCancelable(false)
                .setPositiveButton(R.string.okButton) { _, _ ->
                    lifecycleScope.launch {
                        tag.resetBatteryTo100()
                        viewModel.updateTag(tag)
                        updateBatteryText()
                        Log.d(
                            "TagViewFragment",
                            "Reset ${tag.macAddress} battery to start: ${tag.batteryAlertStart} next: ${tag.batteryAlertNext}"
                        )
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()

        }
    }

    fun checkMacAddressPresence(toggleButton: SwitchMaterial) {
        lifecycleScope.launch {
            val isPresent = FirebaseUtils().isTagPresent(tag.macAddress)
            toggleButton.isChecked = isPresent
        }
    }

    private fun startCameraCaptureActivity() {
        startCameraActivityForResult.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }

    private fun updateBatteryText() {
        val batteryStatus = tag.getBatteryPercent()

        if (batteryStatus != null) {
            binding.tvBatteryEstimate.text =
                requireContext().getString(R.string.batteryEstimate_prefixed, batteryStatus * 100)
        } else {
            binding.tvBatteryEstimate.text =
                requireContext().getString(R.string.batteryUnknown_prefixed)
        }
    }
}
