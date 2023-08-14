package il.ac.hit.beaconfinder.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * This class is a broadcast receiver for intents
 * Currently the main intent is receiving the "turn off" button from notification
 * to shut down the scanning.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action")
        if (action == "turn off") {
            val serviceIntent = Intent(context, BeaconScannerService::class.java)
            val binder = peekService(context, serviceIntent) as BeaconScannerService.BeaconBinder?
            val service = binder?.getService()
            if (service != null) {
                service.stopScan()
            } else {
                Log.e(
                    "NotificationActionReceiver",
                    "Couldn't fetch BeaconScannerService via IBinder"
                )
            }
        } else {
            Log.e("NotificationActionReceiver", "Received unknown broadcast")
        }
    }
}