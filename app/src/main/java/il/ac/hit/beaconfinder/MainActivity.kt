package il.ac.hit.beaconfinder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.SmsManager
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration

import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.tasks.Tasks.call
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import il.ac.hit.beaconfinder.data.MainRepository
import il.ac.hit.beaconfinder.firebase.FirebaseLinkUtils
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import il.ac.hit.beaconfinder.services.BeaconScannerService
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main application activity
 * This sets up permissions, logs into firebase and starts the foreground service
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var firebase: FirebaseUtils

    @Inject
    lateinit var repository: MainRepository

    @Inject
    lateinit var firebaseLinks: FirebaseLinkUtils

    //    private val permissionsRequestCode = 1
//    private val permissions = arrayOf(
//        Manifest.permission.ACCESS_FINE_LOCATION,
//        Manifest.permission.ACCESS_COARSE_LOCATION,
//        Manifest.permission.BLUETOOTH_ADMIN,
//        Manifest.permission.READ_CONTACTS,
//        Manifest.permission.BLUETOOTH,
//        Manifest.permission.CAMERA,
//        Manifest.permission.SEND_SMS
//    )
//
//    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//    private val permissionsForApi33 = arrayOf(
//        Manifest.permission.ACCESS_FINE_LOCATION,
//        Manifest.permission.ACCESS_COARSE_LOCATION,
//        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
//        Manifest.permission.BLUETOOTH_ADMIN,
//        Manifest.permission.BLUETOOTH_CONNECT,
//        Manifest.permission.BLUETOOTH_SCAN,
//        Manifest.permission.READ_CONTACTS,
//        Manifest.permission.BLUETOOTH,
//        Manifest.permission.CAMERA,
//        Manifest.permission.SEND_SMS
//    )
    private var permissionsRequired = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.RECEIVE_BOOT_COMPLETED,

        )

    private val PERMISSION_CALLBACK_CONSTANT = 100
    private val REQUEST_PERMISSION_SETTING = 101
    private var permissionStatus: SharedPreferences? = null
    private var sentToSettings = false

    /**
     * Overrides onCreate to set up the views, login to firebase
     * startup the foreground services and handle intent if one
     * was provided
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val serviceIntent = Intent(this, BeaconScannerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        handleIntent(intent)


        val navView: BottomNavigationView = findViewById(R.id.nav_view)



        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.mainFragment, R.id.socialNetworkFragment
            )
        )
        navView.setupWithNavController(navController)
        setupActionBarWithNavController(navController, appBarConfiguration)
        permissionStatus = getSharedPreferences("permissionStatus", Context.MODE_PRIVATE)
        requestPermission()


    }

    private fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, permissionsRequired[0]) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, permissionsRequired[1]) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, permissionsRequired[2]) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, permissionsRequired[3]) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, permissionsRequired[7]) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[0])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[1])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[2])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[3])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[7])) {
                //Show Information about why you need the permission
                getAlertDialog()
            } else if (permissionStatus!!.getBoolean(permissionsRequired[0], false)) {
                //Previously Permission Request was cancelled with 'Dont Ask Again',
                // Redirect to Settings after showing Information about why you need the permission
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Need Multiple Permissions")
                builder.setMessage("This app needs permissions.")
                builder.setPositiveButton("Grant") { dialog, which ->
                    dialog.cancel()
                    sentToSettings = true
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivityForResult(intent, REQUEST_PERMISSION_SETTING)
                    Toast.makeText(applicationContext, "Go to Permissions to Grant ", Toast.LENGTH_LONG).show()
                }
                builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
                builder.show()
            } else {
                //just request the permission
                ActivityCompat.requestPermissions(this, permissionsRequired, PERMISSION_CALLBACK_CONSTANT)
            }

            //   txtPermissions.setText("Permissions Required")

            val editor = permissionStatus!!.edit()
            editor.putBoolean(permissionsRequired[0], true)
            editor.commit()
        } else {

            //You already have the permission, just go ahead.
            Toast.makeText(applicationContext, "Allowed All Permissions", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CALLBACK_CONSTANT) {
            //check if all permissions are granted
            var allgranted = false
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    allgranted = true
                } else {
                    allgranted = false
                    break
                }
            }

            if (allgranted) {
                Toast.makeText(applicationContext, "Allowed All Permissions", Toast.LENGTH_LONG).show()

            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[0])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[1])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[2])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[3])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[7])) {

                getAlertDialog()
            } else {
                Toast.makeText(applicationContext, "Unable to get Permission", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Need Multiple Permissions")
        builder.setMessage("This app needs permissions.")
        builder.setPositiveButton("Grant") { dialog, which ->
            dialog.cancel()
            ActivityCompat.requestPermissions(this, permissionsRequired, PERMISSION_CALLBACK_CONSTANT)
        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        builder.show()
    }

    override fun onPostResume() {
        super.onPostResume()
        if (sentToSettings) {
            if (ActivityCompat.checkSelfPermission(this, permissionsRequired[0]) == PackageManager.PERMISSION_GRANTED) {
                //Got Permission
                Toast.makeText(applicationContext, "Allowed All Permissions", Toast.LENGTH_LONG).show()
            }
        }
    }




    fun setFragmentTitle(title: String) {
        supportActionBar?.title = title
    }

    /**
     * Overrides on new intent to check valid tag links
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * This function is triggered on creating or receiving new intent
     * The function checks if the intent was triggered with valid tag
     * link, this means someone pressed on tag link and we need to add
     * it to the repository
     */
    private fun handleIntent(intent: Intent) {
        val appLinkData: Uri = intent.data ?: return

        lifecycleScope.launch {
            var tagData = firebaseLinks.tryParseShortLinkAsync(appLinkData.toString())
            if (tagData == null) {
                Log.e("MainActivity", "Link provided is not valid short beacon link $appLinkData")
                tagData = firebaseLinks.tryParseLongLinkAsync(appLinkData.toString())
                if (tagData == null) {
                    Log.e(
                        "MainActivity",
                        "Link provided is not valid long beacon link $appLinkData"
                    )
                    return@launch
                }
            }

            Log.i("MainActivity", "Link provided is valid link for TagData $appLinkData $tagData")

            if (repository.getTags().any { it.macAddress == tagData.macAddress }) {
                Log.w("MainActivity", "Tag with mac ${tagData.macAddress} already registered")
                return@launch
            }
            repository.addTag(tagData)
            val tag = repository.getTags().find { it.macAddress == tagData.macAddress }!!
            val fragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val nc = fragment.navController
            nc.popBackStack(R.id.mainFragment, inclusive = false) // back up to main fragment
            nc.navigate(R.id.action_mainFragment_to_tagViewFragment, bundleOf("tag" to tag))
        }
    }


}
//@AndroidEntryPoint
//class MainActivity : AppCompatActivity() {
//    @Inject
//    lateinit var firebase: FirebaseUtils
//
//    @Inject
//    lateinit var repository: MainRepository
//
//    @Inject
//    lateinit var firebaseLinks: FirebaseLinkUtils
//
//    private val permissionsRequestCode = 1
//    private val permissions = arrayOf(
//        Manifest.permission.ACCESS_FINE_LOCATION,
//        Manifest.permission.ACCESS_COARSE_LOCATION,
//        Manifest.permission.BLUETOOTH_ADMIN,
//        Manifest.permission.READ_CONTACTS,
//        Manifest.permission.BLUETOOTH,
//        Manifest.permission.CAMERA,
//        Manifest.permission.SEND_SMS
//    )
//
//    @RequiresApi(Build.VERSION_CODES.S)
//    private val permissionsForApi31 = arrayOf(
//        Manifest.permission.ACCESS_FINE_LOCATION,
//        Manifest.permission.ACCESS_COARSE_LOCATION,
//        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
//        Manifest.permission.BLUETOOTH_ADMIN,
//        Manifest.permission.BLUETOOTH_CONNECT,
//        Manifest.permission.BLUETOOTH_SCAN,
//        Manifest.permission.READ_CONTACTS,
//        Manifest.permission.BLUETOOTH,
//        Manifest.permission.CAMERA,
//        Manifest.permission.SEND_SMS
//    )
//
//    /**
//     * Overrides onCreate to set up the views, login to firebase
//     * startup the foreground services and handle intent if one
//     * was provided
//     */
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.main_activity)
//
//        val serviceIntent = Intent(this, BeaconScannerService::class.java)
//        ContextCompat.startForegroundService(this, serviceIntent)
//        handleIntent(intent)
//
//
//        val navView: BottomNavigationView = findViewById(R.id.nav_view)
//
//
//
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
//        val navController = navHostFragment.navController
//
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.mainFragment, R.id.socialNetworkFragment
//            )
//        )
//        navView.setupWithNavController(navController)
//        setupActionBarWithNavController(navController, appBarConfiguration)
//
//
//
//    }
//
//
//
//
//
//
//    fun setFragmentTitle(title: String) {
//        supportActionBar?.title = title
//    }
//
//    /**
//     * Overrides on new intent to check valid tag links
//     */
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        handleIntent(intent)
//    }
//
//    /**
//     * This function is triggered on creating or receiving new intent
//     * The function checks if the intent was triggered with valid tag
//     * link, this means someone pressed on tag link and we need to add
//     * it to the repository
//     */
//    private fun handleIntent(intent: Intent) {
//        val appLinkData: Uri = intent.data ?: return
//
//        lifecycleScope.launch {
//            var tagData = firebaseLinks.tryParseShortLinkAsync(appLinkData.toString())
//            if (tagData == null) {
//                Log.e("MainActivity", "Link provided is not valid short beacon link $appLinkData")
//                tagData = firebaseLinks.tryParseLongLinkAsync(appLinkData.toString())
//                if (tagData == null) {
//                    Log.e(
//                        "MainActivity",
//                        "Link provided is not valid long beacon link $appLinkData"
//                    )
//                    return@launch
//                }
//            }
//
//            Log.i("MainActivity", "Link provided is valid link for TagData $appLinkData $tagData")
//
//            if (repository.getTags().any { it.macAddress == tagData.macAddress }) {
//                Log.w("MainActivity", "Tag with mac ${tagData.macAddress} already registered")
//                return@launch
//            }
//            repository.addTag(tagData)
//            val tag = repository.getTags().find { it.macAddress == tagData.macAddress }!!
//            val fragment =
//                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//            val nc = fragment.navController
//            nc.popBackStack(R.id.mainFragment, inclusive = false) // back up to main fragment
//            nc.navigate(R.id.action_mainFragment_to_tagViewFragment, bundleOf("tag" to tag))
//        }
//    }
//
//    /**
//     * Overrides onCreateView to check if we have all required permissions, if not
//     * we request the missing ones from the user.
//     */
//    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
//        val permissionsForCurrentApi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
//            permissionsForApi31 else permissions
//
//        if (!hasPermissions(this, *permissionsForCurrentApi)) {
//            ActivityCompat.requestPermissions(
//                this, permissionsForCurrentApi, permissionsRequestCode
//            )
//        }
//
//        return super.onCreateView(name, context, attrs)
//    }
//
//    /**
//     * Checks if this activity has the specified permissions
//     */
//    private fun hasPermissions(context: Context, vararg permissions: String): Boolean =
//        permissions.all {
//            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//        }
//
//    private fun requestPermissionsUntilGranted(permissions: Array<String>) {
//        val permissionsToRequest = mutableListOf<String>()
//        permissions.forEach { permission ->
//            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//                permissionsToRequest.add(permission)
//            }
//        }
//
//        if (permissionsToRequest.isNotEmpty()) {
//            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), permissionsRequestCode)
//        }
//    }
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == permissionsRequestCode) {
//            val permissionsDenied = mutableListOf<String>()
//
//            grantResults.forEachIndexed { index, result ->
//                if (result != PackageManager.PERMISSION_GRANTED) {
//                    permissionsDenied.add(permissions[index])
//                }
//            }
//
//            if (permissionsDenied.isNotEmpty()) {
//                requestPermissionsUntilGranted(permissionsDenied.toTypedArray())
//            }
//        }
//    }
//}
