package ba.etf.chatapp.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import ba.etf.chatapp.AddParticipantsActivity
import ba.etf.chatapp.ApplicationSettingsActivity
import ba.etf.chatapp.R
import ba.etf.chatapp.SettingsActivity
import ba.etf.chatapp.databinding.ActivitySettingsBinding
import ba.etf.chatapp.models.User
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class EmergencyContactSelectionAdapter(
    private var users: ArrayList<User>,
    private var existingEmergencyContact: User?
) : RecyclerView.Adapter<EmergencyContactSelectionAdapter.ViewHolder>() {

    private lateinit var storage: FirebaseStorage
    private var selectedItemPosition = -1
    private var lastItemSelectedPosition = -1

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userName)
        val image: ImageView = itemView.findViewById(R.id.image)
        val radio: CheckBox = itemView.findViewById(R.id.select)
        init {
            radio.isClickable = false
            itemView.setOnClickListener {
                selectedItemPosition = if (existingEmergencyContact != null && users.contains(existingEmergencyContact)) {
                    users.indexOf(existingEmergencyContact)
                } else {
                    bindingAdapterPosition
                }

                if(lastItemSelectedPosition == -1)
                    lastItemSelectedPosition = selectedItemPosition
                else {
                    notifyItemChanged(lastItemSelectedPosition)
                    lastItemSelectedPosition = selectedItemPosition
                }
                notifyItemChanged(selectedItemPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.sample_select_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]

        storage = FirebaseStorage.getInstance()
        storage.reference.child("Profile Images").child(user.userId!!).downloadUrl.addOnSuccessListener {
            Picasso.get().load(it).placeholder(R.drawable.avatar).into(holder.image)
        }
        holder.userName.text = user.userName

        if(position == selectedItemPosition) {
            selectedUser = user
            holder.radio.isChecked = true
            SettingsActivity.binding.saveEmergencyContactButton.isEnabled = true
        }
        else {
            holder.radio.isChecked = false
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

    companion object {
        var selectedUser: User? = null
    }
}