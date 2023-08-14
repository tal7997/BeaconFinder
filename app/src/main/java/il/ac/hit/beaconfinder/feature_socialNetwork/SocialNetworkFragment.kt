package il.ac.hit.beaconfinder.feature_socialNetwork

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.ac.hit.beaconfinder.databinding.FragmentSocialNetworkBinding
import il.ac.hit.beaconfinder.firebase.UserUtils

/**
 * A simple [Fragment] subclass.
 * Use the [SocialNetworkFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SocialNetworkFragment : Fragment() {

    private lateinit var binding: FragmentSocialNetworkBinding

    private lateinit var adapter: UserListAdapter

    companion object {
        private const val TAG = "SocialNetworkFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView() called with: inflater = $inflater, container = $container, savedInstanceState = $savedInstanceState")
        binding = FragmentSocialNetworkBinding.inflate(inflater, container, false)

        // Initialize RecyclerView adapter
        adapter = UserListAdapter()

        // Set RecyclerView layout manager and adapter
        binding.userListRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.userListRecyclerView.adapter = adapter

        // Load users from Firestore
        loadUsers()

        return binding.root
    }

    private fun loadUsers() {
        Log.d(TAG, "loadUsers() called")

        Firebase.firestore.collection("TagsUser")
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val userList = mutableListOf<FirebaseUserInfo>()
                result.documents.forEach {

                    val uid_value = it.getString(UserUtils.UID) ?: ""
                    val email = it.getString(UserUtils.EMAIL) ?: ""
                    val userName = it.getString(UserUtils.USERNAME) ?: ""
                    val phoneNumber = it.getString(UserUtils.PHONE_NUMBER) ?: ""
                    val fcmToken = it.getString(UserUtils.FCM_TOKEN) ?: ""
                    val groupinfo = it.getString(UserUtils.GROUP_INFO) ?: ""
                    val createdDate = it.getString(UserUtils.CREATED_DATE) ?: ""

                    val firebaseUserInfo = FirebaseUserInfo(
                        uid_value,
                        email,
                        userName,
                        phoneNumber,
                        fcmToken,
                        groupinfo,
                        createdDate.toLongOrNull() ?: 0
                    )

                    userList.add(firebaseUserInfo)

                }


                adapter.setData(userList)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting users", exception)
            }
    }
}