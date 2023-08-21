package ba.etf.chatapp.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import ba.etf.chatapp.ApplicationSettingsActivity
import ba.etf.chatapp.ChatDetailsActivity
import ba.etf.chatapp.GroupChatActivity
import ba.etf.chatapp.ImageShowActivity
import ba.etf.chatapp.R
import ba.etf.chatapp.models.Message
import ba.etf.chatapp.models.User
import ba.etf.chatapp.notifications.NotificationData
import ba.etf.chatapp.notifications.Response
import ba.etf.chatapp.notifications.Sender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class GroupChatAdapter(
    private var messages: ArrayList<Message>,
    private val context: Context,  //?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase

    private val SENDER_TEXT_VIEW_TYPE = 1
    private val SENDER_PHOTO_VIEW_TYPE = 2
    private val RECEIVER_TEXT_VIEW_TYPE = 3
    private val RECEIVER_PHOTO_VIEW_TYPE = 4

    inner class ReceiverTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiverMsg: TextView = itemView.findViewById(R.id.receiverText)
        val receiverTime: TextView = itemView.findViewById(R.id.receiverTime)
        val receiverImage: CircleImageView = itemView.findViewById(R.id.receiverImage)
        val receiverName: TextView = itemView.findViewById(R.id.receiverName)
    }

    inner class SenderTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderMsg: TextView = itemView.findViewById(R.id.senderText)
        val senderTime: TextView = itemView.findViewById(R.id.senderTime)
    }

    inner class ReceiverPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiverMsg: ImageView = itemView.findViewById(R.id.receiverPhoto)
        val receiverTime: TextView = itemView.findViewById(R.id.receiverTime)
        val receiverImage: CircleImageView = itemView.findViewById(R.id.receiverImage)
        val receiverName: TextView = itemView.findViewById(R.id.receiverName)
    }

    inner class SenderPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderMsg: ImageView = itemView.findViewById(R.id.senderPhoto)
        val senderTime: TextView = itemView.findViewById(R.id.senderTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SENDER_TEXT_VIEW_TYPE -> {
                val view =
                    LayoutInflater.from(context).inflate(R.layout.sample_sender, parent, false)
                SenderTextViewHolder(view)
            }

            RECEIVER_TEXT_VIEW_TYPE -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.sample_group_receiver, parent, false)
                ReceiverTextViewHolder(view)
            }

            SENDER_PHOTO_VIEW_TYPE -> {
                val view =
                    LayoutInflater.from(context).inflate(R.layout.photo_sender, parent, false)
                SenderPhotoViewHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.photo_group_receiver, parent, false)
                ReceiverPhotoViewHolder(view)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        storage = FirebaseStorage.getInstance()

        when (holder.javaClass) {
            SenderTextViewHolder::class.java -> {
                (holder as SenderTextViewHolder).senderMsg.text = message.message
                val date = Date(message.timestamp!!.toLong())
                val simpleDateFormat = SimpleDateFormat("dd.MM HH:mm") //"dd.MM.YYYY HH:mm
                holder.senderTime.text = simpleDateFormat.format(date)
                holder.senderMsg.textSize = 14F + ApplicationSettingsActivity.textSizeIncrease * 5
                holder.senderTime.textSize = 9F + ApplicationSettingsActivity.textSizeIncrease * 2
            }
            ReceiverTextViewHolder::class.java -> {
                (holder as ReceiverTextViewHolder).receiverMsg.text = message.message
                database = FirebaseDatabase.getInstance()
                database.reference.child("Users").child(message.uId!!).addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val user = dataSnapshot.getValue(User::class.java)
                        holder.receiverName.text = user!!.userName.toString()
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
                storage = FirebaseStorage.getInstance()
                storage.reference.child("Profile Images").child(message.uId!!).downloadUrl.addOnSuccessListener {
                    Picasso.get().load(it).placeholder(R.drawable.avatar).into(holder.receiverImage)
                }
                val date = Date(message.timestamp!!.toLong())
                val simpleDateFormat = SimpleDateFormat("dd.MM HH:mm") //"dd.MM.YYYY HH:mm
                holder.receiverTime.text = simpleDateFormat.format(date)
                holder.receiverName.textSize = 10F + ApplicationSettingsActivity.textSizeIncrease * 3
                holder.receiverMsg.textSize = 14F + ApplicationSettingsActivity.textSizeIncrease * 5
                holder.receiverTime.textSize = 9F + ApplicationSettingsActivity.textSizeIncrease * 2
            }
            SenderPhotoViewHolder::class.java -> {
                storage.reference.child("Chat Images").child(message.messageId!!).downloadUrl.addOnSuccessListener {
                    Picasso.get().load(it).placeholder(R.drawable.image_icon).into((holder as SenderPhotoViewHolder).senderMsg)
                    GroupChatActivity.binding.chatRecyclerView.smoothScrollToPosition(
                        GroupChatActivity.chatAdapter.itemCount - 1)
                    holder.senderMsg.setOnClickListener {
                        val intent = Intent(context, ImageShowActivity::class.java)
                        intent.putExtra("image", message.messageId)
                        intent.putExtra("userName", "You")
                        context.startActivity(intent)
                    }
                }
                val date = Date(message.timestamp!!.toLong())
                val simpleDateFormat = SimpleDateFormat("dd.MM HH:mm") //"dd.MM.YYYY HH:mm
                (holder as SenderPhotoViewHolder).senderTime.text = simpleDateFormat.format(date)
                holder.senderTime.textSize = 9F + ApplicationSettingsActivity.textSizeIncrease * 2
            }
            ReceiverPhotoViewHolder::class.java -> {
                storage.reference.child("Chat Images").child(message.messageId!!).downloadUrl.addOnSuccessListener {
                    Picasso.get().load(it).placeholder(R.drawable.image_icon).into((holder as ReceiverPhotoViewHolder).receiverMsg)
                    GroupChatActivity.binding.chatRecyclerView.smoothScrollToPosition(
                        GroupChatActivity.chatAdapter.itemCount - 1)
                    holder.receiverMsg.setOnClickListener {
                        val intent = Intent(context, ImageShowActivity::class.java)
                        intent.putExtra("image", message.messageId)
                        intent.putExtra("userName", holder.receiverName.text)
                        context.startActivity(intent)
                    }
                }
                database = FirebaseDatabase.getInstance()
                database.reference.child("Users").child(message.uId!!).addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val user = dataSnapshot.getValue(User::class.java)
                        (holder as ReceiverPhotoViewHolder).receiverName.text = user!!.userName.toString()
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
                storage = FirebaseStorage.getInstance()
                storage.reference.child("Profile Images").child(message.uId!!).downloadUrl.addOnSuccessListener {
                    Picasso.get().load(it).placeholder(R.drawable.avatar).into((holder as ReceiverPhotoViewHolder).receiverImage)
                }
                val date = Date(message.timestamp!!.toLong())
                val simpleDateFormat = SimpleDateFormat("dd.MM HH:mm") //"dd.MM.YYYY HH:mm
                (holder as ReceiverPhotoViewHolder).receiverTime.text = simpleDateFormat.format(date)
                holder.receiverName.textSize = 10F + ApplicationSettingsActivity.textSizeIncrease * 3
                holder.receiverTime.textSize = 9F + ApplicationSettingsActivity.textSizeIncrease * 2
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(!messages[position].image) {
            if (messages[position].uId.equals(FirebaseAuth.getInstance().uid)) {
                SENDER_TEXT_VIEW_TYPE
            } else {
                RECEIVER_TEXT_VIEW_TYPE
            }
        } else {
            if (messages[position].uId.equals(FirebaseAuth.getInstance().uid)) {
                SENDER_PHOTO_VIEW_TYPE
            } else {
                RECEIVER_PHOTO_VIEW_TYPE
            }
        }
    }

    /*fun setData(newData: ArrayList<Message>) {
        messages.addAll(0, newData)
        this.notifyItemRangeInserted(0, newData.size)
    }*/
}