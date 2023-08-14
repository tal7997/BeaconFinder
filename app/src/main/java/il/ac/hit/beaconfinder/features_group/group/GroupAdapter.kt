package il.ac.hit.beaconfinder.features_group.group

import android.icu.text.SimpleDateFormat
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.data.GroupEntity
import java.util.Date
import java.util.Locale

class GroupAdapter(private val onItemClick: (GroupEntity) -> Unit) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textView_group_name)
        val date: TextView = view.findViewById(R.id.textView_date)
        val message: TextView = view.findViewById(R.id.textView_message)
    }

    var items: List<GroupEntity> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = items[position]
        holder.name.text = group.name
        holder.message.text = group.displayMessage
        holder.date.text = formatDate(group.groupCreatedDate)

        holder.itemView.setOnClickListener {
            onItemClick(group)
        }
    }

    fun updateItems(newItems: List<GroupEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun formatDate(date: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(Date(date))
    }
}