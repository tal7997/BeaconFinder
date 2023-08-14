package il.ac.hit.beaconfinder.features_group.showGroupMemeber

import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import il.ac.hit.beaconfinder.R
import java.util.Date
import java.util.Locale

class GroupMemberAdapter(private var groupMembers: List<GroupMember>) :
    RecyclerView.Adapter<GroupMemberAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val statusDot: View = view.findViewById(R.id.status_dot)
        val lastSeen: TextView = view.findViewById(R.id.last_seen)
    }

    fun updateMembers(newMessages: List<GroupMember>) {
        groupMembers = newMessages
        notifyDataSetChanged()
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun formatDate(date: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(Date(date))
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = groupMembers[position]
        holder.name.text = member.name
        holder.statusDot.setBackgroundColor(if (member.isOnline) Color.BLUE else Color.LTGRAY)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.lastSeen.text =   "Last Seen : ${formatDate(member.lastSeen.toLong())} -  ${getTimeLabel(member.lastSeen.toLong())}"
        } else {
            holder.lastSeen.text = "Last Seen : ${member.lastSeen}  -  ${getTimeLabel(member.lastSeen.toLong())}"
        }

    }
    fun getTimeLabel(timeInMillis: Long): String {
        val currentTime = System.currentTimeMillis()
        return DateUtils.getRelativeTimeSpanString(timeInMillis, currentTime, DateUtils.MINUTE_IN_MILLIS).toString()
    }
    override fun getItemCount() = groupMembers.size
}