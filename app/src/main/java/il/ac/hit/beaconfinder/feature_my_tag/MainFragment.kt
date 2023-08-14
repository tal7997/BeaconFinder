package il.ac.hit.beaconfinder.feature_my_tag

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.TagRecyclerViewAdapter
import il.ac.hit.beaconfinder.databinding.FragmentMainBinding
import il.ac.hit.beaconfinder.services.BeaconScannerService
import il.ac.hit.beaconfinder.viewmodels.MainViewModel


@AndroidEntryPoint
class MainFragment : Fragment(), TagRecyclerViewAdapter.ItemClickListener , ActivityCompat.OnRequestPermissionsResultCallback {
    private val viewModel by viewModels<MainViewModel>()
    private var beaconScannerService: BeaconScannerService? = null

    private lateinit var binding: FragmentMainBinding

    private val TAG = "MainFragment"
    private val PERMISSION_BLUETOOTH = 1


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val intent = Intent(context, BeaconScannerService::class.java)
        requireContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)

        val adapter = TagRecyclerViewAdapter(requireContext(), ArrayList())
        adapter.setClickListener(this)

        binding.rvTagList.layoutManager = LinearLayoutManager(context, VERTICAL, false)
        binding.rvTagList.adapter = adapter
        binding.btnAddTag.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                // Bluetooth permission is already granted, navigate to the desired destination
                viewModel.addNewTag(findNavController())
            } else {
                // Bluetooth permission is not granted, request the permission
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH), PERMISSION_BLUETOOTH)
            }
        }
        binding.switchBackgroundTracking.setOnClickListener {
            if ((it as SwitchMaterial).isChecked) {
                beaconScannerService?.startScan()
            } else {
                beaconScannerService?.stopScan()
            }
        }

        viewModel.getSearchedForTags().observe(viewLifecycleOwner) {
            binding.tvSummary.text =
                if (it.isNotEmpty()) getString(R.string.searchedForTagsSummary, it.count()) else ""
        }
        viewModel.getRegisteredBeacons().observe(viewLifecycleOwner) {
            adapter.setData(it)
            binding.tvNoBeaconsAdded.visibility = if (it.isEmpty()) View.VISIBLE else View.INVISIBLE
        }
        viewModel.fetchRegisteredBeacons()



    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_BLUETOOTH) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Bluetooth permission granted, navigate to the desired destination
                viewModel.addNewTag(findNavController())
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH)) {
                // Bluetooth permission denied, but the user has not selected "Don't ask again"
                // You can show a custom explanation dialog here if needed
                showPermissionDeniedDialog()
            } else {
                // Bluetooth permission denied, and the user has selected "Don't ask again"
                // You can show a dialog prompting the user to grant the permission manually through app settings
                showPermissionDeniedDialog()
            }
        }
    }

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            beaconScannerService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            (service as BeaconScannerService.BeaconBinder).let { scanner ->
                scanner.getService().getIsScanning().observe(this@MainFragment) { isScanning ->
                    binding.switchBackgroundTracking.isChecked = isScanning
                }
            }
        }
    }

    override fun onDestroyView() {
        if (binding.switchBackgroundTracking.isChecked) {
            Log.i(
                "MainFragment",
                "onDestroyView: Background tracking is enabled, keeping scanner service alive"
            )
            requireContext().unbindService(mServiceConnection)
        }

        super.onDestroyView()
    }

    override fun onItemClick(view: View?, position: Int) {
        val tag = (binding.rvTagList.adapter as TagRecyclerViewAdapter).getItem(position)

        viewModel.viewTag(findNavController(), tag)
    }

    private fun showPermissionDeniedDialog() {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Bluetooth Permission Alert")
        dialogBuilder.setMessage("Please open your setting to allow Bluetooth")
        dialogBuilder.setPositiveButton("Go to Setting") { dialog, which ->
            // Open app settings for the user to manually grant the Bluetooth permission
            openAppSettings()
        }
        dialogBuilder.setNegativeButton(R.string.cancel) { dialog, which ->
            // Handle cancellation if needed
        }
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun openAppSettings() {
        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        appSettingsIntent.data = uri
        bluetoothPermissionLauncher.launch(appSettingsIntent)
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Check if the Bluetooth permission has been granted
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Bluetooth permission granted, navigate to the desired destination
                viewModel.addNewTag(findNavController())
            } else {
                // Bluetooth permission is still not granted
                // Handle the situation accordingly
            }
        }
    }


}
