package il.ac.hit.beaconfinder.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.erm.beaconscanner.utils.BeaconManagerUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.AndroidEntryPoint
import il.ac.hit.beaconfinder.BuildConfig
import il.ac.hit.beaconfinder.MainActivity
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.data.MainRepository
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import kotlinx.coroutines.launch
import org.altbeacon.beacon.*
import javax.inject.Inject

/**
 * This is the main service
 * It provides the context for the bluetooth scanning, in case where it's needed it writes to
 * repository and provides the context for notifications
 */
@AndroidEntryPoint
class BeaconScannerService : LifecycleService(), RangeNotifier {
    @Inject
    lateinit var repository: MainRepository

    @Inject
    lateinit var firebase: FirebaseUtils

    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var beaconManager: BeaconManager

    private val channelId = "beacon_scanner_channel"
    private val binder = BeaconBinder()
    private var currentLocation: Location? = null
    private var isScanning = MutableLiveData(false)
    private var beaconRegion = Region("all-beacons-region", null, null, null)

    /**
     * Provide a copy of current location or null if location is not yet available
     */
    fun getCurrentLocation() = if (currentLocation != null) Location(currentLocation) else null

    fun getIsScanning(): LiveData<Boolean> {
        return isScanning
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        beaconManager = BeaconManagerUtils.initBeaconManager(this@BeaconScannerService)
        repository.registerForFirebaseUpdates(this)
        repository.setNearbyBeacons(mutableListOf())
        locationProvider =
            LocationServices.getFusedLocationProviderClient(this@BeaconScannerService)
        locationProvider.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                return@addOnSuccessListener
            }

            currentLocation = location
        }

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId, "BeaconScanner channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(channel)
        }

        val scanOnStart = applicationContext
            .getSharedPreferences("default", Context.MODE_PRIVATE)
            .getBoolean("scan", false)

        if (scanOnStart) {
            startScan()
        } else {
            setupNotification(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i("BeaconScannerService", "onStartCommand was called")

        // we could receive intent.extras in intent here to stop ourselves
        // currently this is implemented via NotificationActionReceiver broadcast instead

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    /**
     * Returns a service IBinder that can be used to access this service
     */
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("BeaconScannerService", "onDestroy was called")
        repository.unregisterForFirebaseUpdates()
    }

    /**
     * Method to start the scanner, this also raises notification
     * The notification is mandatory to display, otherwise android system
     * will show "application not responding" screen
     */
    fun startScan() {
        Log.i("BeaconScannerService", "startScan was called")

        if (isScanning.value == true) {
            Log.w("BeaconScannerService", "startScan was called but a scan was already running")
            return
        }

        beaconManager.addRangeNotifier(this)
        beaconManager.startRangingBeacons(beaconRegion)
        beaconManager.startMonitoring(beaconRegion)
        isScanning.value = true

        setupNotification(true)

        applicationContext.getSharedPreferences("default", Context.MODE_PRIVATE)
            .edit().putBoolean("scan", isScanning.value == true)
            .apply()

        Log.i("BeaconScannerService", "isScanning.value = ${(isScanning.value == true)}")
    }

    /**
     * Method to set up notification
     * When clicked this raises the activity to foreground
     * Puts an extra button to turn off scanning
     */
    private fun setupNotification(showNotification: Boolean) {
        val intent = Intent(applicationContext, NotificationActionReceiver::class.java)
        val restoreViewIntent = Intent(applicationContext, MainActivity::class.java)
        val restoreIntent = PendingIntent.getActivity(
            applicationContext,
            System.currentTimeMillis().toInt(),
            restoreViewIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        intent.putExtra("action", "turn off")
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val requestID = System.currentTimeMillis().toInt()
        val actionIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentText(applicationContext.getString(R.string.serviceNotificationText))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(restoreIntent)
            .addAction(R.mipmap.ic_launcher, "Turn Off", actionIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        if (!showNotification)
            stopForeground(true)
    }

    /**
     * Stops the bluetooth scan and clears monitoring
     */
    fun stopScan() {
        Log.i("BeaconScannerService", "stopScan was called")

        if (isScanning.value != true) {
            Log.w("BeaconScannerService", "stopScan was called but no scan was running")
            return
        }

        isScanning.value = false
        applicationContext.getSharedPreferences("default", Context.MODE_PRIVATE)
            .edit().putBoolean("scan", isScanning.value == true)
            .apply()
        Log.i("BeaconScannerService", "isScanning.value = ${(isScanning.value == true)}")

        beaconManager.stopMonitoring(beaconRegion)
        beaconManager.stopRangingBeacons(beaconRegion)
        beaconManager.removeRangeNotifier(this)
        stopForeground(true)
    }

    /**
     * This method implements RangeNotifier interface, it's invoked when bluetooth scan is
     * completed and returns the beacon collection.
     */
    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>?, region: Region?) {
        lifecycleScope.launch {
            if (beacons == null) {
                Log.w("BeaconScannerService", "didRangeBeaconsInRegion received null beacons")
                return@launch
            }

            // in debug mode provide a dummy debug beacon with constant MAC address
            if (BuildConfig.DEBUG) {
                val b: Beacon = Beacon.Builder()
                    .setBluetoothAddress("DE:AD:BE:EF:00:00")
                    .setRssi(50)
                    .build()
                beacons.add(b)
            }

            val newBeacons = mutableListOf<Beacon>()

            for (beacon in beacons) {
                newBeacons.add(beacon)
                val location =
                    GeoPoint(currentLocation?.latitude ?: 0.0, currentLocation?.longitude ?: 0.0)
                repository.addTagPingLocal(beacon.bluetoothAddress, location)
                Log.v(
                    "BeaconScannerService",
                    "Found tag: ${beacon.bluetoothAddress} distance:${beacon.distance} rssi:${beacon.rssi} @ $location"
                )
            }

            repository.setNearbyBeacons(newBeacons)
            repository.checkIfLocalPingsNeedSending(this@BeaconScannerService)
        }
    }

    /**
     * Service binder so that other components can find this service
     */
    inner class BeaconBinder : Binder() {
        fun getService() = this@BeaconScannerService
    }
}
