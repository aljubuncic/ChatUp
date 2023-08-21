package ba.etf.chatapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import ba.etf.chatapp.ChatDetailsActivity
import ba.etf.chatapp.MainActivity
import ba.etf.chatapp.R
import ba.etf.chatapp.databinding.FragmentChatsBinding
import ba.etf.chatapp.databinding.FragmentFeelingsBinding
import ba.etf.chatapp.models.Message
import ba.etf.chatapp.models.User
import ba.etf.chatapp.notifications.APIService
import ba.etf.chatapp.notifications.Client
import ba.etf.chatapp.notifications.NotificationData
import ba.etf.chatapp.notifications.Response
import ba.etf.chatapp.notifications.Sender
import ba.etf.chatapp.notifications.Token
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import java.util.Date

class FeelingsFragment : Fragment() {
    private lateinit var binding: FragmentFeelingsBinding
    private lateinit var users: ArrayList<User>
    private lateinit var database: FirebaseDatabase
    private lateinit var apiService: APIService
    private lateinit var auth: FirebaseAuth

    private lateinit var senderId: String
    private lateinit var receiverId: String
    private lateinit var senderRoom: String
    private lateinit var receiverRoom: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeelingsBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance()
        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)
        auth = Firebase.auth
        users = ArrayList()

        binding.text.setBackgroundColor(Color.parseColor(MainActivity.appTheme))

        database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if(!user!!.parent && !user.teacher) {
                    database.reference.child("Users").orderByChild("mail")
                        .equalTo(user.parentMail).addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (snapshot in dataSnapshot.children) {
                                senderId = auth.uid!!
                                receiverId = snapshot.key!!
                                senderRoom = senderId + receiverId
                                receiverRoom = receiverId + senderId
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        binding.happy.setOnClickListener {
            val message = Message(senderId, "SRETAN SAM!")
            message.timestamp = Date().time.toString()

            database.reference.child("Chats").child(senderRoom).push().setValue(message).addOnSuccessListener {
                database.reference.child("Chats").child(receiverRoom).push().setValue(message).addOnSuccessListener {
                    sendNotification(receiverId, senderId, "SRETAN SAM!")
                }
            }
        }

        binding.sad.setOnClickListener {
            val message = Message(senderId, "TUŽAN SAM!")
            message.timestamp = Date().time.toString()

            database.reference.child("Chats").child(senderRoom).push().setValue(message).addOnSuccessListener {
                database.reference.child("Chats").child(receiverRoom).push().setValue(message).addOnSuccessListener {
                    sendNotification(receiverId, senderId, "TUŽAN SAM!")
                }
            }
        }

        binding.crying.setOnClickListener {
            val message = Message(senderId, "UPLAKAN SAM!")
            message.timestamp = Date().time.toString()

            database.reference.child("Chats").child(senderRoom).push().setValue(message).addOnSuccessListener {
                database.reference.child("Chats").child(receiverRoom).push().setValue(message).addOnSuccessListener {
                    sendNotification(receiverId, senderId, "UPLAKAN SAM!")
                }
            }
        }

        binding.tired.setOnClickListener {
            val message = Message(senderId, "UMORAN SAM!")
            message.timestamp = Date().time.toString()

            database.reference.child("Chats").child(senderRoom).push().setValue(message).addOnSuccessListener {
                database.reference.child("Chats").child(receiverRoom).push().setValue(message).addOnSuccessListener {
                    sendNotification(receiverId, senderId, "UMORAN SAM!")
                }
            }
        }

        binding.angry.setOnClickListener {
            val message = Message(senderId, "LJUT SAM!")
            message.timestamp = Date().time.toString()

            database.reference.child("Chats").child(senderRoom).push().setValue(message).addOnSuccessListener {
                database.reference.child("Chats").child(receiverRoom).push().setValue(message).addOnSuccessListener {
                    sendNotification(receiverId, senderId, "LJUT SAM!")
                }
            }
        }

        binding.heart.setOnClickListener {
            val message = Message(senderId, "VOLIM VAS!")
            message.timestamp = Date().time.toString()

            database.reference.child("Chats").child(senderRoom).push().setValue(message).addOnSuccessListener {
                database.reference.child("Chats").child(receiverRoom).push().setValue(message).addOnSuccessListener {
                    sendNotification(receiverId, senderId, "VOLIM VAS!")
                }
            }
        }

        return binding.root
    }

    private fun sendNotification(receiverId: String, senderId: String, message: String) {
        val tokens = database.getReference("Tokens")
        val query = tokens.orderByKey().equalTo(receiverId)
        query.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for(snapshot in dataSnapshot.children) {
                    val token = snapshot.getValue(Token::class.java)
                    database.reference.child("Users").child(senderId).addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val user = dataSnapshot.getValue(User::class.java)
                            val username = user!!.userName.toString()
                            //?
                            val data = NotificationData(senderId, R.drawable.logo, message, username, receiverId)
                            val sender = Sender(data, token!!.token)

                            apiService.sendNotification(sender).enqueue(object :
                                Callback<Response> {
                                override fun onFailure(call: Call<Response>?, t: Throwable?) {
                                }

                                override fun onResponse(call: Call<Response>?, response: retrofit2.Response<Response>?) {
                                    if(response!!.code() == 200) {
                                        if(response.body()!!.success != 1) {
                                            Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                            })
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}