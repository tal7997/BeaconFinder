package il.ac.hit.beaconfinder.features_group.group_member

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import il.ac.hit.beaconfinder.R

class ContactAdapter(private val contactList: List<InviteGroupMemberFragment.Contact>, val  onClick : (String) -> Unit) :
    RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "ContactAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contactList[position]
        holder.nameTextView.text = contact.name
        holder.numberTextView.text = contact.phoneNumber
        holder.inviteButton.setOnClickListener {
            sendSMS(contact.phoneNumber,"Hello, this is your invite message!",holder.itemView.context)
            onClick(contact.phoneNumber,)
            Log.d("ContactAdapter", "Invite button clicked for contact: ${contact.name}")
        }
        holder.itemView.setOnClickListener {
            Log.d("ContactAdapter", "Contact item clicked: ${contact.name}")
            // Handle itemView click event
        }
    }


    fun sendSMS(phoneNo: String, msg: String, context : Context){
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNo, null, msg, null, null)
            Toast.makeText(context, "Message Sent",
                Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
            Toast.makeText(context,"Message failed",
                Toast.LENGTH_LONG).show()
            ex.printStackTrace()
        }
    }



    override fun getItemCount(): Int {
        return contactList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.contact_name)
        val numberTextView: TextView = itemView.findViewById(R.id.contact_number)
        val inviteButton: Button = itemView.findViewById(R.id.invite_button)
    }

}