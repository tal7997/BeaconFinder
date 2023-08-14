package il.ac.hit.beaconfinder.features_group.group_chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import il.ac.hit.beaconfinder.features_group.group_chat.GroupChatFragment.Companion.messages
import il.ac.hit.beaconfinder.features_group.showGroupMemeber.GroupMember

class GroupChatViewModel : ViewModel() {

     companion object {
         private const val TAG = "ChatViewModel"
     }


    private val _chatMessages = MutableLiveData<MutableList<GroupChatMessage>>()
    val chatMessages: LiveData<MutableList<GroupChatMessage>> get() = _chatMessages


    private val _groupMember = MutableLiveData<MutableList<GroupMember>>()
    val groupMember: LiveData<MutableList<GroupMember>> get() = _groupMember



    init {

    }

    fun listenForMessages(messageid : String) {
        Log.d(TAG, "listenForMessages() called")

        val databaseReference = FirebaseDatabase.getInstance().getReference("groups").child(messageid).child(GroupChatFragment.messages)

        val chatMessages = mutableListOf<GroupChatMessage>()

        databaseReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(GroupChatMessage::class.java)
                if (chatMessage != null) {
                    chatMessages.add(chatMessage)
                    _chatMessages.value = chatMessages
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenForMemeberstatus(messageid : String) {
        Log.d(TAG, "listenForMessages() called")

        val databaseReference = FirebaseDatabase.getInstance().getReference("groups").child(messageid).child(GroupChatFragment.MemberNode)

        val chatMessages = mutableListOf<GroupMember>()

        databaseReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(GroupMember::class.java)
                if (chatMessage != null) {
                    chatMessages.add(chatMessage)
                    _groupMember.value = chatMessages
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }






}