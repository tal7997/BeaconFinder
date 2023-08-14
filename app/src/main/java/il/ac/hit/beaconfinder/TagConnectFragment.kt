package il.ac.hit.beaconfinder

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.AndroidEntryPoint
import il.ac.hit.beaconfinder.databinding.FragmentTagConnectBinding
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import il.ac.hit.beaconfinder.services.BeaconScannerService
import il.ac.hit.beaconfinder.viewmodels.TagConnectViewModel
import kotlinx.coroutines.launch
import java.time.Instant

@AndroidEntryPoint
class TagConnectFragment : Fragment(), TagRecyclerViewAdapter.ItemClickListener {
    private val viewModel by viewModels<TagConnectViewModel>()

    private lateinit var binding: FragmentTagConnectBinding
    private lateinit var rvAdapter: TagRecyclerViewAdapter
    private var beaconScannerService: BeaconScannerService? = null

    val processedMacAddresses = HashSet<String>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTagConnectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val intent = Intent(context, BeaconScannerService::class.java)
        requireContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)

        rvAdapter = TagRecyclerViewAdapter(requireContext(), ArrayList())
        binding.rvTagConnect.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvTagConnect.adapter = rvAdapter

        lifecycleScope.launch {
            val existingBeacons = viewModel.getTags()
                .map { it.macAddress }
                .toHashSet()

            viewModel.getNearbyBeacons().observe(requireActivity()) { beaconsList ->
                val beaconsData = beaconsList
                    .filter { !existingBeacons.contains(it.bluetoothAddress) }
                    .map { beacon ->
                        val macAddress = beacon.bluetoothAddress
                        if (!processedMacAddresses.contains(macAddress)) {
                            processedMacAddresses.add(macAddress)
                            checkMacAddressPresence(requireContext(), macAddress)
                        }
                        val td = TagData(beacon.bluetoothAddress)
//                      checkMacAddressPresence(requireActivity(),td.macAddress)
                        td.distance = beacon.distance
                        beaconScannerService?.getCurrentLocation()?.let { loc ->
                            td.lastSeen = Instant.now()
                            td.lastSeenAt = GeoPoint(loc.latitude, loc.longitude)
                        }

                        td.description = "Tag " + beacon.bluetoothAddress
                        td
                    }
                    .toList()
                binding.tvTagConnectNoBeacons.visibility =
                    if (beaconsData.isEmpty()) View.VISIBLE else View.INVISIBLE
                rvAdapter.setData(beaconsData)
            }
        }

        rvAdapter.setClickListener(this)

        binding.btnQrScanner.setOnClickListener {
            NavHostFragment.findNavController(this@TagConnectFragment)
                .navigate(R.id.action_tagConnectFragment_to_qrScannerFragment)
        }
    }

    fun checkMacAddressPresence(context:Context, macAddress: String) {

        lifecycleScope.launch {
            val isPresent = FirebaseUtils().isOtherTagPresent(context,macAddress)

        }

    }


    /**
     * Handler for item click on a line in RecycleView of tags
     */
    override fun onItemClick(view: View?, position: Int) {
        val tag = rvAdapter.getItem(position)

        lifecycleScope.launch {
            viewModel.addTag(tag)
            viewModel.viewTag(findNavController(), tag)
        }
    }

    /**
     * Class representing service connection holder
     *
     * This represents
     */
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        /**
         * Called when a connection to the Service has been lost. This typically happens when the
         * process hosting the service has crashed or been killed. This does not remove the
         * ServiceConnection itself -- this binding to the service will remain active, and you will
         * receive a call to onServiceConnected when the Service is next running.
         */
        override fun onServiceDisconnected(name: ComponentName) {
            beaconScannerService = null
        }

        /**
         * Called when a connection to the Service has been established, with the IBinder of the communication channel to the Service.
         * Note: If the system has started to bind your client app to a service, it's possible that your app will never receive this callback.
         * Your app won't receive a callback if there's an issue with the service, such as the service crashing while being created.
         */
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            beaconScannerService = (service as BeaconScannerService.BeaconBinder).getService()
            beaconScannerService?.startScan()
        }
    }
}
