package il.ac.hit.beaconfinder.feature_socialNetwork

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import il.ac.hit.beaconfinder.R

class UserListAdapter : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    private var userList = listOf<FirebaseUserInfo>()

    fun setData(users: List<FirebaseUserInfo>) {
        userList = users
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_user, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(user: FirebaseUserInfo) {
            itemView.findViewById<TextView>(R.id.userNameTextView).text = user.userName
            itemView.findViewById<TextView>(R.id.emailTextView).text = user.email
            itemView.findViewById<TextView>(R.id.phoneTextView).text = user.phoneNumber

        }
    }
}
