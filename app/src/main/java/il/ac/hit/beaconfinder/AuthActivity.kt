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
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint
import il.ac.hit.beaconfinder.data.MainRepository
import il.ac.hit.beaconfinder.firebase.FirebaseLinkUtils
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {


    @Inject
    lateinit var firebase: FirebaseUtils

    @Inject
    lateinit var repository: MainRepository



    @Inject
    lateinit var firebaseLinks: FirebaseLinkUtils
    private var permissionsRequired = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_activity)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        setupActionBarWithNavController(navController)
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



}

//import android.Manifest
//import android.content.Context
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.util.AttributeSet
//import android.view.View
//import androidx.annotation.RequiresApi
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.navigation.fragment.NavHostFragment
//import androidx.navigation.ui.setupActionBarWithNavController
//import dagger.hilt.android.AndroidEntryPoint
//import il.ac.hit.beaconfinder.data.MainRepository
//import il.ac.hit.beaconfinder.firebase.FirebaseLinkUtils
//import il.ac.hit.beaconfinder.firebase.FirebaseUtils
//import javax.inject.Inject

//@AndroidEntryPoint
//class AuthActivity : AppCompatActivity() {
//
//
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
//
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
//        Manifest.permission.BLUETOOTH_SCAN,
//        Manifest.permission.BLUETOOTH_CONNECT,
//        Manifest.permission.BLUETOOTH,
//        Manifest.permission.CAMERA,
//        Manifest.permission.SEND_SMS
//    )
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.auth_activity)
//
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        var  navController = navHostFragment.navController
//
//        setupActionBarWithNavController(navController)
//    }
//
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
//
//
//    /**
//     * Checks if this activity has the specified permissions
//     */
//    private fun hasPermissions(context: Context, vararg permissions: String): Boolean =
//        permissions.all {
//            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//        }
//    private fun checkAndRequestPermissions() {
//        val permissionsToRequest = mutableListOf<String>()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.BLUETOOTH_ADMIN
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.BLUETOOTH_CONNECT
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.BLUETOOTH_SCAN
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.READ_CONTACTS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.CAMERA
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.CAMERA)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.SEND_SMS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.SEND_SMS)
//            }
//        } else {
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.BLUETOOTH_ADMIN
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.READ_CONTACTS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.CAMERA
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.CAMERA)
//            }
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.SEND_SMS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                permissionsToRequest.add(Manifest.permission.SEND_SMS)
//            }
//
//            if (permissionsToRequest.isNotEmpty()) {
//                ActivityCompat.requestPermissions(
//                    this, permissionsToRequest.toTypedArray(), permissionsRequestCode
//                )
//            } else {
//                // All permissions are already granted
//            }
//        }
//
//    }
//
//
//
//
//
////    private fun checkAndRequestPermissions() {
////        val permissionsForCurrentApi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
////            permissionsForApi31 else permissions
////
////        val permissionsToRequest = mutableListOf<String>()
////        permissionsForCurrentApi.forEach { permission ->
////            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
////                permissionsToRequest.add(permission)
////            }
////        }
////
////        if (permissionsToRequest.isNotEmpty()) {
////            ActivityCompat.requestPermissions(
////                this, permissionsToRequest.toTypedArray(), permissionsRequestCode
////            )
////        } else {
////            // All permissions are already granted
////        }
////    }
//    /**
//     * Overrides the onRequestPermissionsResult to handle the permission request result.
//     * If any permissions are denied, it requests the missing ones again until all are granted.
//     */
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
//                checkAndRequestPermissions()
//            } else {
//                // All permissions are granted
//            }
//        }
//    }
//
//}