package il.ac.hit.beaconfinder.features_group.group_chat

import android.app.PendingIntent.getActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import il.ac.hit.beaconfinder.R
import org.jetbrains.anko.backgroundDrawable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroupChatAdapter(private var chatMessages: List<GroupChatMessage>) : RecyclerView.Adapter<GroupChatAdapter.ChatViewHolder>() {

    companion object {
        private const val TAG = "ChatAdapter"
    }

    var currentUserUUid : String? = null


    fun updateMessages(newMessages: List<GroupChatMessage>) {
        chatMessages = newMessages
        notifyDataSetChanged()
    }


    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val centerNameTextView: TextView = view.findViewById(R.id.centerNameTextView)
        val timeTextView: TextView = view.findViewById(R.id.timeTextView)
        val messageTextView: TextView = view.findViewById(R.id.messageTextView)
        val bubble: LinearLayout = view.findViewById(R.id.bubble)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatMessage = chatMessages[position]

        holder.centerNameTextView.text = chatMessage.centerName

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = sdf.format(Date(chatMessage.timestamp))
        holder.timeTextView.text = time

        holder.messageTextView.text = chatMessage.message

        if (currentUserUUid!=null) {

            // Assuming 'isUserSender' is a boolean that is true when the user is the sender of the message
            val bubble: LinearLayout = holder.bubble
            val params: RelativeLayout.LayoutParams = bubble.layoutParams as RelativeLayout.LayoutParams

            if (currentUserUUid == chatMessage.uuid) {
                params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
                params.addRule(RelativeLayout.ALIGN_PARENT_START, 0) // remove start alignment
                bubble.backgroundDrawable = ContextCompat.getDrawable(holder.bubble.context, R.drawable.me_chat_bubble_background)!!;
            } else {
                params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
                params.addRule(RelativeLayout.ALIGN_PARENT_END, 0) // remove end alignment
                bubble.backgroundDrawable = ContextCompat.getDrawable(holder.bubble.context, R.drawable.chat_bubble_background)!!;

            }

            bubble.layoutParams = params


        }

    }

    override fun getItemCount() = chatMessages.size
}

