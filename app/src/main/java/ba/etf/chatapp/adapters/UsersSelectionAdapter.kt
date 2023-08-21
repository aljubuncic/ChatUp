package ba.etf.chatapp.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import ba.etf.chatapp.AddParticipantsActivity
import ba.etf.chatapp.ApplicationSettingsActivity
import ba.etf.chatapp.R
import ba.etf.chatapp.models.User
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlin.collections.ArrayList

class UsersSelectionAdapter(
    private var users: ArrayList<User>
) : RecyclerView.Adapter<UsersSelectionAdapter.ViewHolder>() {

    private lateinit var storage: FirebaseStorage

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userName)
        val image: ImageView = itemView.findViewById(R.id.image)
        val radio: CheckBox = itemView.findViewById(R.id.select)
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

        holder.radio.setOnClickListener {
            Log.i("clikc", holder.radio.isChecked.toString())
            //holder.radio.isChecked = !holder.radio.isChecked
            if(holder.radio.isChecked) {
                selectedUsers.add(user)
            }
            else {
                selectedUsers.removeIf { u -> u.mail.equals(user.mail) } //radi li? //equals
            }
            if(selectedUsers.isNotEmpty()) {
                AddParticipantsActivity.binding.next.setTextColor(Color.WHITE)
                AddParticipantsActivity.binding.next.isEnabled = true
            }
            else {
                AddParticipantsActivity.binding.next.setTextColor(Color.GRAY)
                AddParticipantsActivity.binding.next.isEnabled = false
            }
        }

        val factor = holder.itemView.context.resources.displayMetrics.density
        if(ApplicationSettingsActivity.photoIncrease) {
            holder.image.updateLayoutParams {
                height = (105 * factor).toInt()
                width = (105 * factor).toInt()
            }
        }
        else {
            holder.image.updateLayoutParams {
                height = (85 * factor).toInt()
                width = (85 * factor).toInt()
            }
        }

        holder.userName.textSize = 18F + UsersAdapter.textSizeIncreased * 4
    }

    override fun getItemCount(): Int {
        return users.size
    }

    companion object {
        val selectedUsers = ArrayList<User>()
    }
}