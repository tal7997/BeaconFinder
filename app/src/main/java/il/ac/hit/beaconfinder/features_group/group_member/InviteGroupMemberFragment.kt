package il.ac.hit.beaconfinder.features_group.group_member



import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import il.ac.hit.beaconfinder.MainActivity
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.features_group.groupInvite
import il.ac.hit.beaconfinder.features_group.group_chat.GroupChatFragment
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import kotlinx.coroutines.launch
import java.util.Date

class InviteGroupMemberFragment : Fragment() {

    companion object {
        private const val TAG = "GroupMember"
        private const val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    }

    private lateinit var contactsRecyclerView: RecyclerView

    var GroupId = ""

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group_member, container, false)
        contactsRecyclerView = view.findViewById(R.id.contacts_recycler_view)
        contactsRecyclerView.layoutManager = LinearLayoutManager(context)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadContacts()

       val isAdd = arguments?.getBoolean(GroupChatFragment.isAdd) ?: true
        GroupId = arguments?.getString(GroupChatFragment.GroupId) ?: throw Exception("Exception")

        if (isAdd) {
            // Set the fragment-specific title
            activity?.title = "Add Members"
            (activity as MainActivity).setFragmentTitle("Add Members")

        } else {
            (activity as MainActivity).setFragmentTitle("Remove Members")
            activity?.title = "Remove Members"
        }


    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadContacts()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            Manifest.permission.READ_CONTACTS
                        )
                    ) {
                        // User has selected "Don't ask again," show a dialog to guide the user to app settings
                        showPermissionSettingsDialog()
                    } else {
                        // Permission denied, show a toast or handle it accordingly
                        Toast.makeText(
                            requireContext(),
                            "Permission denied.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }



    private fun showPermissionSettingsDialog() {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle("Permission Required")
        alertDialogBuilder.setMessage("To access contacts, you need to grant the permission. Please enable it in the app settings.")
        alertDialogBuilder.setPositiveButton("App Settings") { dialog, which ->
            // Open app settings
            openAppSettings()
        }
        alertDialogBuilder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
    }



    private fun loadContacts() {
        val progressBar = view?.findViewById<ProgressBar>(R.id.loading_progress_bar)
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            progressBar?.visibility = View.VISIBLE
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
        } else {
            progressBar?.visibility = View.GONE
            getContacts()
        }
    }

   private fun getContacts() {
        val contactList = mutableListOf<Contact>()
        val contactsCursor = requireContext().contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
        contactsCursor?.apply {
            while (moveToNext()) {
                val idIndex = getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val hasPhoneNumberIndex = getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                if (idIndex > -1 && nameIndex > -1 && hasPhoneNumberIndex > -1) {
                    val id = getString(idIndex)
                    val name = getString(nameIndex)
                    val hasPhoneNumber = getInt(hasPhoneNumberIndex) > 0
                    if (hasPhoneNumber) {
                        val phoneCursor = requireContext().contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf(id), null)
                        phoneCursor?.apply {
                            while (moveToNext()) {
                                val phoneNumberIndex = getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (phoneNumberIndex > -1) {
                                    val phoneNumber = getString(phoneNumberIndex)
                                    contactList.add(Contact(name, phoneNumber))
                                }
                            }
                            close()
                        }
                    }
                }
            }
            close()
        }
        // Now, you can set the adapter and update the RecyclerView.
// Assuming you're in a coroutine scope
       contactsRecyclerView.adapter = ContactAdapter(contactList) {

           Log.i(TAG, "getContacts: invite")

           val groupInvite = groupInvite(createTime = Date().time, memberPhone = it, groupId = GroupId)
           lifecycleScope.launch {
               FirebaseUtils().invitePeople(groupInvite)
           }
       }

       // Then add the DividerItemDecoration
       val dividerItemDecoration = DividerItemDecoration(requireContext(), (contactsRecyclerView!!.layoutManager as LinearLayoutManager).orientation)
       contactsRecyclerView!!.addItemDecoration(dividerItemDecoration)


   }


/*    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContacts()
                } else {
                    Toast.makeText(requireContext(), "You have disabled a contacts permission", Toast.LENGTH_LONG).show()
                }
            }
        }
    }*/

    data class Contact(val name: String, val phoneNumber: String)
}
