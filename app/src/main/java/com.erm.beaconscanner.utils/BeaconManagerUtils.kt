package com.erm.beaconscanner.utils

import android.content.Context
import org.altbeacon.beacon.*

object BeaconManagerUtils {
    private const val ermBeaconLayout =
        "m:0-1=4C00,m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"

    private fun initBeaconParsers(beaconManager: BeaconManager) {
        // By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
        // find a different type of beacon, you must specify the byte layout for that beacon's
        // advertisement with a line like below.  The example shows how to find a beacon with the
        // same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb.  To find the proper
        // layout expression for other beacon types, do a web search for "setBeaconLayout"
        // including the quotes.
        beaconManager.beaconParsers.clear()
        beaconManager.beaconParsers.addAll(
            listOf(
                BeaconParser().setBeaconLayout(ermBeaconLayout),
                BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT),
                BeaconParser().setBeaconLayout(BeaconParser.URI_BEACON_LAYOUT),
                BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT),
                BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT),
                BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT),

            )
        )
    }

    fun initBeaconManager(applicationContext: Context): BeaconManager {
        val beaconManager = BeaconManager.getInstanceForApplication(applicationContext)
        beaconManager.isRegionStatePersistenceEnabled = false
        initBeaconParsers(beaconManager)

        // beaconManager.setDebug(true);
        beaconManager.setEnableScheduledScanJobs(false)
        beaconManager.foregroundBetweenScanPeriod = 0L
        beaconManager.foregroundScanPeriod = 1100L
        beaconManager.backgroundBetweenScanPeriod = 0L
        beaconManager.backgroundScanPeriod = 1100L
        // Configures whether a the bluetoothAddress (mac address) must be the same for two Beacons to be configured equal.
        Beacon.setHardwareEqualityEnforced(true)
        // BeaconManager.setAndroidLScanningDisabled(false);
        return beaconManager
    }
}
