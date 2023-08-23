package ba.etf.chatapp.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ba.etf.chatapp.ApplicationSettingsActivity
import ba.etf.chatapp.GroupChatActivity
import ba.etf.chatapp.ImageShowActivity
import ba.etf.chatapp.R
import ba.etf.chatapp.models.Message
import ba.etf.chatapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import me.jagar.chatvoiceplayerlibrary.VoicePlayerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class GroupChatAdapter(
    private var messages: ArrayList<Message>,
    private val context: Context,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase

    private val SENDER_TEXT_VIEW_TYPE = 1
    private val SENDER_PHOTO_VIEW_TYPE = 2
    private val RECEIVER_TEXT_VIEW_TYPE = 3
    private val RECEIVER_PHOTO_VIEW_TYPE = 4
    private val SENDER_RECORD_VIEW_TYPE = 5
    private val RECEIVER_RECORD_VIEW_TYPE = 6

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

    inner class SenderRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val voicePlayerView: VoicePlayerView = itemView.findViewById(R.id.voicePlayerView)
    }

    inner class ReceiverRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiverImage: CircleImageView = itemView.findViewById(R.id.receiverImage)
        val receiverName: TextView = itemView.findViewById(R.id.receiverName)
        val voicePlayerView: VoicePlayerView = itemView.findViewById(R.id.voicePlayerView)
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

            RECEIVER_PHOTO_VIEW_TYPE -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.photo_group_receiver, parent, false)
                ReceiverPhotoViewHolder(view)
            }
            SENDER_RECORD_VIEW_TYPE -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.sender_audio_item, parent, false)
                SenderRecordViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.group_receiver_audio_item, parent, false)
                ReceiverRecordViewHolder(view)
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
                val simpleDateFormat = SimpleDateFormat("dd.MM HH:mm")
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
                val simpleDateFormat = SimpleDateFormat("dd.MM HH:mm")
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
                val simpleDateFormat = SimpleDateFormat("dd.MM HH:mm")
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
                val simpleDateFormat = SimpleDateFormat("dd.MM HH:mm")
                (holder as ReceiverPhotoViewHolder).receiverTime.text = simpleDateFormat.format(date)
                holder.receiverName.textSize = 10F + ApplicationSettingsActivity.textSizeIncrease * 3
                holder.receiverTime.textSize = 9F + ApplicationSettingsActivity.textSizeIncrease * 2
            }
            SenderRecordViewHolder::class.java -> {
                (holder as SenderRecordViewHolder).voicePlayerView.setAudio(message.message)
            }
            ReceiverRecordViewHolder::class.java -> {
                database.reference.child("Users").child(message.uId!!).addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val user = dataSnapshot.getValue(User::class.java)
                        (holder as ReceiverRecordViewHolder).receiverName.text = user!!.userName.toString()
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
                (holder as ReceiverRecordViewHolder).receiverName.textSize = 10F + ApplicationSettingsActivity.textSizeIncrease * 3
                storage = FirebaseStorage.getInstance()
                storage.reference.child("Profile Images").child(message.uId!!).downloadUrl.addOnSuccessListener {
                    Picasso.get().load(it).placeholder(R.drawable.avatar).into(holder.receiverImage)
                }
                holder.voicePlayerView.setAudio(message.message)
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(messages[position].image) {
            if (messages[position].uId.equals(FirebaseAuth.getInstance().uid)) {
                SENDER_PHOTO_VIEW_TYPE
            } else {
                RECEIVER_PHOTO_VIEW_TYPE
            }
        } else if(messages[position].record){
            if (messages[position].uId.equals(FirebaseAuth.getInstance().uid)) {
                SENDER_RECORD_VIEW_TYPE
            } else {
                RECEIVER_RECORD_VIEW_TYPE
            }
        } else {
            if (messages[position].uId.equals(FirebaseAuth.getInstance().uid)) {
                SENDER_TEXT_VIEW_TYPE
            } else {
                RECEIVER_TEXT_VIEW_TYPE
            }
        }
    }
}