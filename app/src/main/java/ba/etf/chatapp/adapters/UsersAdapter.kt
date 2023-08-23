package ba.etf.chatapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import ba.etf.chatapp.ApplicationSettingsActivity
import ba.etf.chatapp.ChatDetailsActivity
import ba.etf.chatapp.ContactActivity
import ba.etf.chatapp.GroupChatActivity
import ba.etf.chatapp.R
import ba.etf.chatapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlin.collections.ArrayList

class UsersAdapter(
    private var users: ArrayList<User>,
    private val context: Context,
    var chats: Boolean,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var storage: FirebaseStorage

    private val SINGLE_VIEW_TYPE = 1
    private val GROUP_VIEW_TYPE = 2

    companion object {
        var photoIncreased = false
        var textSizeIncreased = 0
    }

    inner class SingleChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userNameList)
        val image: ImageView = itemView.findViewById(R.id.profileImage)
        val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        val imageOnline: ImageView = itemView.findViewById(R.id.online)
        val imageOffline: ImageView = itemView.findViewById(R.id.offline)
    }

    inner class GroupChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userNameList)
        val image: ImageView = itemView.findViewById(R.id.profileImage)
        val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == SINGLE_VIEW_TYPE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.sample_show_user, parent, false)
            SingleChatViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.sample_show_user, parent, false)
            GroupChatViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val user = users[position]
        storage = FirebaseStorage.getInstance()

        val factor = holder.itemView.context.resources.displayMetrics.density
        photoIncreased = ApplicationSettingsActivity.photoIncrease
        textSizeIncreased = ApplicationSettingsActivity.textSizeIncrease

        if(holder.javaClass == SingleChatViewHolder::class.java) {
            storage.reference.child("Profile Images").child(user.userId!!).downloadUrl.addOnSuccessListener {
                Picasso.get().load(it).placeholder(R.drawable.avatar).into((holder as SingleChatViewHolder).image)
            }
            (holder as SingleChatViewHolder).userName.text = user.userName

            if (user.status.equals("online")) {
                holder.imageOnline.visibility = View.VISIBLE
                holder.imageOffline.visibility = View.GONE
            } else {
                holder.imageOnline.visibility = View.GONE
                holder.imageOffline.visibility = View.VISIBLE
            }

            if(chats) {
                FirebaseDatabase.getInstance().reference.child("Chats").child(FirebaseAuth.getInstance().uid + user.userId)
                    .orderByChild("timestamp").limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for(snapshot in dataSnapshot.children) {
                                val image = snapshot.child("image").value
                                val record = snapshot.child("record").value
                                if (image == true) holder.lastMessage.text = "Slika"
                                else if (record == true) holder.lastMessage.text = "Glasovna poruka"
                                else holder.lastMessage.text = snapshot.child("message").value.toString()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
            }

            holder.itemView.setOnClickListener {
                if(chats) {
                    val intent = Intent(context, ChatDetailsActivity::class.java)
                    intent.putExtra("userId", user.userId)
                    intent.putExtra("profileImage", user.profileImage)
                    intent.putExtra("userName", user.userName)
                    context.startActivity(intent)
                }
                else {
                    val intent = Intent(context, ContactActivity::class.java)
                    intent.putExtra("userId", user.userId)
                    intent.putExtra("profileImage", user.profileImage)
                    intent.putExtra("userName", user.userName)
                    intent.putExtra("email", user.mail)
                    if (user.parent) intent.putExtra("person", "parent")
                    if (user.teacher) intent.putExtra("person", "teacher")
                    if (!user.parent && !user.teacher) intent.putExtra("parentEmail", user.parentMail)
                    context.startActivity(intent)
                }
            }

            if(ApplicationSettingsActivity.photoIncrease) {
                holder.image.updateLayoutParams {
                    height = (105 * factor).toInt()
                    width = (105 * factor).toInt()
                }
                val params = LinearLayout.LayoutParams((15 * factor).toInt(), (15 * factor).toInt())
                params.setMargins((-25 * factor).toInt(), (78 * factor).toInt(), 0, 0)
                holder.imageOffline.layoutParams = params
                holder.imageOnline.layoutParams = params
            }
            else {
                holder.image.updateLayoutParams {
                    height = (85 * factor).toInt()
                    width = (85 * factor).toInt()
                }
                val params = LinearLayout.LayoutParams((15 * factor).toInt(), (15 * factor).toInt())
                params.setMargins((-25 * factor).toInt(), (62 * factor).toInt(), 0, 0)
                holder.imageOffline.layoutParams = params
                holder.imageOnline.layoutParams = params
            }

            holder.userName.textSize = 18F + textSizeIncreased * 5
            holder.lastMessage.textSize = 14F + textSizeIncreased * 5
        }
        else {
            storage.reference.child("Profile Images").child(user.userId!!).downloadUrl.addOnSuccessListener {
                Picasso.get().load(it).placeholder(R.drawable.avatar).into((holder as GroupChatViewHolder).image)
            }
            (holder as GroupChatViewHolder).userName.text = user.userName

            FirebaseDatabase.getInstance().reference.child("Group Chats").child(user.userId!!)
                .orderByChild("timestamp").limitToLast(1)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for(snapshot in dataSnapshot.children) {
                            val image = snapshot.child("image").value
                            val record = snapshot.child("record").value
                            if (image == true) holder.lastMessage.text = "Slika"
                            else if (record == true) holder.lastMessage.text = "Glasovna poruka"
                            else holder.lastMessage.text = snapshot.child("message").value.toString()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })

            holder.itemView.setOnClickListener {
                val intent = Intent(context, GroupChatActivity::class.java)
                intent.putExtra("groupId", user.userId)
                intent.putExtra("groupName", user.userName)
                context.startActivity(intent)
            }

            holder.userName.textSize = 18F + textSizeIncreased * 5
            holder.lastMessage.textSize = 14F + textSizeIncreased * 5

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

        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (users[position].mail != null) {
            SINGLE_VIEW_TYPE
        } else {
            GROUP_VIEW_TYPE
        }
    }
}