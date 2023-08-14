package il.ac.hit.beaconfinder.features_group.group_chat

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import il.ac.hit.beaconfinder.R

import il.ac.hit.beaconfinder.feature_socialNetwork.FirebaseUserInfo
import il.ac.hit.beaconfinder.features_group.group.GroupFragment
import il.ac.hit.beaconfinder.features_group.showGroupMemeber.GroupMember
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class GroupChatFragment : Fragment() {

    companion object {
        private const val TAG = "GroupChatFragment"
        const val isAdd = "isAdd"
        var GroupId = "GroupId"
        const val MemberNode = "members"
        const val messages = "messages"
    }


    private lateinit var chatAdapter: GroupChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var databaseReference: DatabaseReference

    private lateinit var viewModel: GroupChatViewModel
    private lateinit var groupId: String

    private var user: FirebaseUserInfo? = null
    private var lastChat: GroupChatMessage? = null

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val USER_STATUS_DELAY: Long =  10 * 1000 // 5 minutes

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val view = inflater.inflate(R.layout.fragment_group_chat, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        val chatInputEditText = view.findViewById<EditText>(R.id.chatInputEditText)
        val sendButton = view.findViewById<View>(R.id.sendButton)
        viewModel = ViewModelProvider(this)[GroupChatViewModel::class.java]


        chatAdapter = GroupChatAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = chatAdapter


        groupId = arguments?.getString(GroupFragment.GroupID) ?: throw Exception(" groupId is null ")
        // Use the groupId as needed

        viewModel.listenForMessages(groupId)

        databaseReference = FirebaseDatabase.getInstance().getReference("groups").child(groupId).child(messages)


        // Initialize handler and runnable
        handler = Handler()
        runnable = Runnable { updateUserStatus() }

        sendButton.setOnClickListener {
            val message = chatInputEditText.text.toString().trim()

            if (message.isNotEmpty()) {
                sendMessage(message,user?.uid)
                chatInputEditText.setText("")
            } else {
                Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }


        observeMessages()

        setHasOptionsMenu(true);

        lifecycleScope.launch(Dispatchers.IO) {
            user = FirebaseUtils().getCurrentUserdata()
            chatAdapter.currentUserUUid = user?.uid
            withContext(Dispatchers.Main) {
                // Update your UI here
                chatAdapter.notifyDataSetChanged()
            }

            // update the status
            if (user != null) {

                val tempt =  GroupMember(name= user!!.userName ,isOnline = true , lastSeen = Date().time.toString(),uuid =user!!.uid, fcm = user!!.fcmToken )
                updateStatus(tempt )
            }

        }


        return view
    }

    private fun startUserStatusUpdate() {
        handler.postDelayed(runnable, USER_STATUS_DELAY)
    }

    private fun stopUserStatusUpdate() {
        handler.removeCallbacks(runnable)
    }

    override fun onPause() {
        super.onPause()

        startUserStatusUpdate()

    }

    override fun onDestroyView() {

/*        lifecycleScope.launch(Dispatchers.IO) {
            if (user != null) {
                val tempt = GroupMember(name = user!!.userName, isOnline = false, lastSeen = Date().time.toString(), uuid = user!!.uid, fcm = user!!.fcmToken)
                updateStatus(tempt)
            }
        }*/

        stopUserStatusUpdate()

        super.onDestroyView()
    }
    fun updateStatus(groupMember : GroupMember) {


        val groupMembersRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId).child(MemberNode)
        groupMembersRef.orderByChild("uuid").equalTo(groupMember.uuid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // The group member with the specified 'uuid' already exists
                    // Let's update their status
                    for (snapshot in dataSnapshot.children) {
                        val existingMemberRef = snapshot.ref
                        existingMemberRef.child("isOnline").setValue(groupMember.isOnline)
                        existingMemberRef.child("lastSeen").setValue(groupMember.lastSeen)
                    }
                } else {
                    // The group member with the specified 'uuid' does not exist
                    // Let's create a new group member and add it to the database
                    groupMembersRef.push().setValue(groupMember)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })

    }





    private fun observeMessages() {
        viewModel.chatMessages.observe(viewLifecycleOwner) { chatMessages ->
            chatAdapter.updateMessages(chatMessages)
            recyclerView.scrollToPosition(chatMessages.size - 1)
        }
    }

    private fun sendMessage(message: String, uid: String?) {

        if (user != null) {
            val centerName = user!!.userName ?: "Anonymous"
            val timestamp = Calendar.getInstance().timeInMillis
            lastChat = GroupChatMessage(centerName, timestamp, message, uid ?: "")

            databaseReference.push().setValue(lastChat)

        } else {
            Toast.makeText(context, "You must be logged in to send messages", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.group_chat_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }


    private fun navigateToGroupMember(isAddValue : Boolean) {
        Log.d(TAG, "navigateToGroupMember() called with: isAddValue = $isAddValue")

        if ( view != null) {
            val bundle = Bundle()
            bundle.putBoolean(isAdd, isAddValue)
            bundle.putString(GroupId, groupId)
            Navigation.findNavController(view!!)
                .navigate(il.ac.hit.beaconfinder.R.id.GroupChatFragment_to_GroupMember, bundle)

        }

    }


    private fun navigateToShowGroupMember() {
        Log.d(TAG, "navigateToShowGroupMember() called")

        if ( view != null) {
            val bundle = Bundle()
            bundle.putBoolean(isAdd, false)
            bundle.putString(GroupId, groupId)
            Navigation.findNavController(view!!)
                .navigate(R.id.GroupChatFragment_to_ShowGroupMember, bundle)

        }

    }

    private fun updateUserStatus() {
        // Check if the user exists and update the status accordingly
        if (user != null) {
            val tempt = GroupMember(
                name = user!!.userName,
                isOnline = false,
                lastSeen = Date().time.toString(),
                uuid = user!!.uid,
                fcm = user!!.fcmToken
            )
            updateStatus(tempt)
        }
    }



    override fun onDestroy() {
        super.onDestroy()


        // update last message



    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_member -> {
                // Handle add member action
                navigateToGroupMember(true)
                true
            }

            R.id.Show_members -> {
                // Handle remove member action
                navigateToShowGroupMember()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}