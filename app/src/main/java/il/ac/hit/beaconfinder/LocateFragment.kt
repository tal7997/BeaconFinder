package il.ac.hit.beaconfinder

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.maps.android.SphericalUtil
import dagger.hilt.android.AndroidEntryPoint
import il.ac.hit.beaconfinder.databinding.FragmentLocateBinding
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import il.ac.hit.beaconfinder.firebase.TagPing
import il.ac.hit.beaconfinder.viewmodels.LocateViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class LocateFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnCameraIdleListener, GoogleMap.InfoWindowAdapter {
    private val viewModel by viewModels<LocateViewModel>()

    @Inject
    lateinit var firebase: FirebaseUtils

    private lateinit var binding: FragmentLocateBinding
    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var tag: TagData
    private lateinit var pings: List<TagPing>

    private var currentLocation: Location? = null
    private var arrowMarkers: MutableList<Marker> = arrayListOf()
    private var lastCamZoom = -1f
    private val viewsToggles: ArrayList<View> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { x -> tag = x.get("tag") as TagData }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLocateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewsToggles.addAll(
            listOf(
                binding.fabMyLoc,
                binding.fabFirstLoc,
                binding.fabLastLoc,
                binding.fabEditNotification,
                binding.fabDefaultZoom,

                binding.tvMyLoc,
                binding.tvFirstLoc,
                binding.tvLastLoc,
                binding.tvEditNotification,
                binding.tvDefaultZoom
            )
        )

        viewsToggles.forEach { it.visibility = View.GONE }

        binding.fabActions.shrink()
        binding.fabActions.setOnClickListener {
            if (viewsToggles.first().visibility == View.GONE) {
                binding.fabActions.extend()
                viewsToggles.forEach { it.visibility = View.VISIBLE }
            } else {
                binding.fabActions.shrink()
                viewsToggles.forEach { it.visibility = View.GONE }
            }
        }

        MapsInitializer.initialize(requireContext())
        locationProvider = LocationServices.getFusedLocationProviderClient(requireContext())

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.onResume() // needed to get the map to display immediately
        binding.mapView.getMapAsync(this)

        binding.fabEditNotification.setOnClickListener {
            val input = EditText(requireContext())
            input.inputType = InputType.TYPE_CLASS_TEXT

            MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.tag_nearby_notification_edit_msg))
                .setTitle(getString(R.string.enter_notification))
                .setView(input)
                .setPositiveButton(getString(R.string.okButton)) { _, _ ->
                    setSearchNotification(
                        input.text.toString()
                    )
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
                .show()
        }
    }

    private fun setSearchNotification(notification: String) {
        lifecycleScope.launch {
            firebase.addTagToSearch(tag.macAddress, notification)
        }
    }

    @SuppressLint("PotentialBehaviorOverride", "MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val polylineOptions = PolylineOptions()
        val markerOptions = MarkerOptions()
        // Collect markers in a list to calculate the bounds
        val markers = mutableListOf<Marker>()

        // Camera builder for marker bounds calculation
        val builder = LatLngBounds.builder()
        val padding = 120
        var bounds: LatLngBounds
        var cameraUpdate: CameraUpdate

        lifecycleScope.launch {
            firebase.addTagToSearch(tag.macAddress, "")
            pings = firebase.getPingsForMac(tag.macAddress)

            for (ping in pings) {
                val loc = LatLng(ping.location.latitude, ping.location.longitude)
                polylineOptions
                    .add(loc)
                    .width(6f)
                markerOptions
                    .position(loc)
                    .snippet(timestampFormat(ping) + "\n" + getString(R.string.nav_to_marker))
                val marker = mMap.addMarker(markerOptions)
                if (marker != null) {
                    markers.add(marker)
                }
                mMap.addPolyline(polylineOptions)
            }

            // Center all markers under one view and change last marker color
            for (marker in markers) {
                builder.include(marker.position)
                markers[markers.lastIndex].setIcon(
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
            }
            if (markers.isEmpty()) {
                builder.include(LatLng(0.0, 0.0))
            }
            bounds = builder.build()
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            mMap.animateCamera(cameraUpdate)

            // Marker info window event for waze or google maps navigation
            mMap.setOnInfoWindowClickListener(this@LocateFragment)
            mMap.setInfoWindowAdapter(this@LocateFragment)
            // Arrow count change based on camera zoom
            mMap.setOnCameraIdleListener(this@LocateFragment)

            // Floating buttons on click events
            binding.fabDefaultZoom.setOnClickListener {
                mMap.animateCamera(cameraUpdate)
            }

            binding.fabFirstLoc.setOnClickListener {
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            pings[0].location.latitude,
                            pings[0].location.longitude
                        ),
                        16f
                    )
                )
            }
            binding.fabLastLoc.setOnClickListener {
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            pings[pings.lastIndex].location.latitude,
                            pings[pings.lastIndex].location.longitude
                        ),
                        16f
                    )
                )
            }

            //User location on success event and floating button actions
            locationProvider.lastLocation.addOnSuccessListener { location ->
                if (location == null) {
                    return@addOnSuccessListener
                }
                currentLocation = location
                val currentPos = LatLng(location.latitude, location.longitude)

                for (marker in markers) {
                    val distance = SphericalUtil.computeDistanceBetween(currentPos, marker.position)
                    marker.title = if (distance >= 1000)
                        getString(R.string.km_away, distance / 1000)
                    else
                        getString(R.string.meters_away, distance)
                }

                binding.fabMyLoc.setOnClickListener {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos, 16f))
                }
            }

            // Google Maps ui settings
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = false
            mMap.uiSettings.isMapToolbarEnabled = false
            mMap.uiSettings.isCompassEnabled = true
        }
    }

    //MapView methods//////////
    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
    //MapView methods//////////

    //Google Maps on click listeners events//////////
    override fun onInfoWindowClick(marker: Marker) {
        val listItem = arrayOf(getString(R.string.waze), getString(R.string.google_maps))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_nav_app))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
            .setSingleChoiceItems(listItem, -1) { dialog, which ->
                val ctx = requireContext()
                when (which) {
                    0 -> viewModel.navigateWaze(ctx, marker.position)
                    1 -> viewModel.navigateGoogleMaps(ctx, marker.position)
                }
                dialog.dismiss()
            }
            .show()
    }

    override fun onCameraIdle() {
        val camZoom = mMap.cameraPosition.zoom
        if (lastCamZoom != camZoom) {
            lastCamZoom = camZoom
            if (camZoom >= 16) {
                removeArrowsOnMap()
                setArrowsOnMap(0.2)
            } else {
                removeArrowsOnMap()
                setArrowsOnMap(0.5)
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    override fun getInfoContents(marker: Marker): View? {
        if (marker.title == null || marker.snippet == null) {
            return null
        }
        val info = LinearLayout(requireContext())
        info.orientation = LinearLayout.VERTICAL

        val str = marker.snippet
        val strArray = str?.split("\n")

        val title = TextView(requireContext())
        title.setTextColor(Color.BLACK)
        title.gravity = Gravity.CENTER_VERTICAL
        title.setTypeface(null, Typeface.BOLD)
        title.text = marker.title

        val snippet1 = TextView(requireContext())
        snippet1.setTextColor(Color.BLACK)
        snippet1.gravity = Gravity.CENTER_VERTICAL
        if (strArray != null) {
            snippet1.text = strArray[0]
        }
        snippet1.setTypeface(null, Typeface.BOLD)

        val snippet2 = TextView(requireContext())
        snippet2.setTextColor(R.color.purple_500)
        snippet2.gravity = Gravity.CENTER_VERTICAL
        if (strArray != null) {
            snippet2.text = strArray[1]
        }
        snippet2.setTypeface(null, Typeface.BOLD)

        info.addView(title)
        info.addView(snippet1)
        info.addView(snippet2)

        return info
    }

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }
    //Google Maps on click listeners events//////////

    //Directional arrows on Google Map methods//////////
    private fun setDrawableToBitmapDescriptor(drawableId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(requireContext(), drawableId)!!
        val canvas = Canvas()
        val bitmap: Bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun addDirectionMarker(latLng: LatLng, angle: Float, drawableId: Int) {
        val markerIcon: BitmapDescriptor = setDrawableToBitmapDescriptor(drawableId)
        val markerOptions = MarkerOptions()
            .position(latLng)
            .anchor(0.5f, 0.5f)
            .rotation(angle)
            .flat(true)
            .icon(markerIcon)
        mMap.addMarker(
            markerOptions
        )?.let { arrowMarkers.add(it) }
    }

    private fun setArrowsOnMap(fractionAdd: Double) {
        for (i in 0 until pings.size - 1) {
            var fraction = 0.0
            val from = LatLng(pings[i].location.latitude, pings[i].location.longitude)
            val to = LatLng(pings[i + 1].location.latitude, pings[i + 1].location.longitude)

            while (fraction < 1.0) {
                fraction += fractionAdd
                val interpolated = SphericalUtil.interpolate(from, to, fraction)
                val segmentBearing = SphericalUtil.computeHeading(from, to).toFloat()
                if (SphericalUtil.computeDistanceBetween(from, to) >= 2
                    && (interpolated != from || interpolated != to)
                ) {
                    addDirectionMarker(interpolated, segmentBearing, R.drawable.ic_action_arrow_v2)
                }
            }
        }
    }

    private fun removeArrowsOnMap() {
        for (arrow in arrowMarkers) {
            arrow.remove()
        }
    }
    //Directional arrows on Google Map methods//////////

    private fun timestampFormat(ping: TagPing): String {
        val currentTime = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis())
        val pingTime = TimeUnit.SECONDS.toMinutes(ping.date.seconds)
        val timeDiff = (currentTime - pingTime).toDouble()
        val timeDiffHours = timeDiff / 60
        val remnantDiffToMinutes = (timeDiffHours - timeDiffHours.toInt()) * 60

        return if (timeDiff < 60) {
            getString(R.string.lastSeenMinutes, timeDiff)
        } else {
            getString(R.string.lastSeenHours, timeDiffHours, remnantDiffToMinutes)
        }
    }
}
