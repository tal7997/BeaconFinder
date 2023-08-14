package il.ac.hit.beaconfinder

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import dagger.hilt.android.AndroidEntryPoint
import il.ac.hit.beaconfinder.databinding.FragmentQrScannerBinding
import il.ac.hit.beaconfinder.viewmodels.QrScannerViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QrScannerFragment : Fragment(), BarcodeCallback {
    private val viewModel by viewModels<QrScannerViewModel>()
    private val cameraPermissionsRequestCode = 101

    private lateinit var binding: FragmentQrScannerBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentQrScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPermissions()

        viewModel.setBeepManager(BeepManager(requireActivity()))
        viewModel.getStatusText().observe(viewLifecycleOwner) {
            binding.scannerView.statusView.text = it
        }

        binding.scannerView.setStatusText(getString(R.string.qrScannerInstruction))
        binding.scannerView.barcodeView.decoderFactory =
            DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        binding.scannerView.initializeFromIntent(requireActivity().intent)
        binding.scannerView.decodeContinuous(this)
    }

    override fun onResume() {
        super.onResume()
        binding.scannerView.resume()
        setupPermissions()
    }

    override fun onPause() {
        super.onPause()
        binding.scannerView.pause()
        setupPermissions()
    }

    private fun setupPermissions() {
        val permission =
            ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.CAMERA)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.CAMERA),
            cameraPermissionsRequestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            cameraPermissionsRequestCode -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.e("QrScannerFragment", "Camera permission isn't available")
                }

            }
        }
    }

    override fun barcodeResult(result: BarcodeResult) {
        lifecycleScope.launch {
            viewModel.handleScanResult(result.text)?.let {
                viewModel.navigateToTagView(findNavController(), it)
            }
        }
    }
}
