package ba.etf.chatapp

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ba.etf.chatapp.adapters.GroupChatAdapter
import ba.etf.chatapp.databinding.ActivityGroupChatBinding
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

class GroupChatActivity : AppCompatActivity() {
    companion object {
        lateinit var binding: ActivityGroupChatBinding
        lateinit var chatAdapter: GroupChatAdapter
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var isLoadMore = false
    private var firstMessage = false
    //private var currentPage = 1
    private val recordPerPage = 20
    //private var loadedMessages = ArrayList<Message>()
    private lateinit var storage: FirebaseStorage
    private var notify = false
    private lateinit var apiService: APIService

    private lateinit var senderId: String
    private lateinit var groupId: String
    private lateinit var groupName: String
    private lateinit var receiverRoom: String

    private lateinit var messages: ArrayList<Message>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()

        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        senderId = auth.uid!!
        groupId = intent.getStringExtra("groupId")!!
        groupName = intent.getStringExtra("groupName")!!

        binding.groupName.text = intent.getStringExtra("groupName")
        //Picasso.get().load(profileImage).placeholder(R.drawable.avatar).into(binding.profileImage)
        storage = FirebaseStorage.getInstance()
        storage.reference.child("Profile Images").child(intent.getStringExtra("groupId")!!).downloadUrl.addOnSuccessListener {
            Picasso.get().load(it).placeholder(R.drawable.avatar).into(binding.image)
        }

        binding.backArrow.setOnClickListener {
            /*val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)*/
            finish()
        }

        binding.details.setOnClickListener {
            val intent = Intent(this, GroupChatDetailsActivity::class.java)
            intent.putExtra("groupId", groupId)
            intent.putExtra("groupName", groupName)
            startActivity(intent)
        }

        messages = ArrayList<Message>()
        chatAdapter = GroupChatAdapter(messages, this)

        binding.chatRecyclerView.adapter = chatAdapter

        val linearLayoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.layoutManager = linearLayoutManager

        binding.sendMessage.setOnClickListener {
            notify = true
            val msg = binding.enterMessage.text.toString()
            val message = Message(senderId, msg)
            message.timestamp = Date().time.toString()
            binding.enterMessage.setText("")

            database.reference.child("Group Chats").child(groupId!!).push().setValue(message).addOnSuccessListener {
                if(notify) {
                    sendNotification(groupId, groupName!!, senderId!!, msg)
                }
                notify = false
            }
        }

        binding.plus.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/* video/*" //radi li video?
            startActivityForResult(intent, 25)
        }

        binding.enterMessage.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 2
        binding.groupName.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 3

        binding.toolbar.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        binding.chatLinearLayout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        /*database.reference.child("Group Chats").child(groupId!!).orderByChild("timestamp").limitToLast(recordPerPage).addValueEventListener(object:
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                messages.clear()
                for(snapshot in dataSnapshot.children) {
                    val message = snapshot.getValue(Message::class.java)
                    message?.messageId = snapshot.key
                    messages.add(message!!)
                }
                chatAdapter.notifyDataSetChanged()
                if(chatAdapter.itemCount != 0) {
                    binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })*/
        loadData()

        binding.chatRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0 && !isLoadMore && messages.isNotEmpty()) {
                    loadMoreData(messages[0].timestamp!!)
                }
            }

            private fun loadMoreData(timestamp: String) {
                if (!isLoadMore && !firstMessage) {
                    isLoadMore = true

                    database.reference.child("Group Chats").child(groupId).orderByChild("timestamp").endBefore(timestamp).limitToLast(recordPerPage)
                        .addValueEventListener(object: ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for(snapshot in dataSnapshot.children.reversed()) {
                                    val message = snapshot.getValue(Message::class.java)
                                    message?.messageId = snapshot.key
                                    messages.add(0, message!!)
                                }
                                chatAdapter.notifyDataSetChanged()
                                isLoadMore = false
                            }
                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                }
            }
        })
    }

    private fun sendNotification(groupId: String, groupName: String, senderId: String, message: String) {
        val tokens = database.getReference("Tokens")
        //val users = ArrayList<User>()
        database.reference.child("Group Participants").child(groupId).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    //users.add(snapshot.getValue(User::class.java)!!)
                    val receiverId = snapshot.value.toString().substring(8).dropLast(1)
                    if (receiverId != FirebaseAuth.getInstance().uid) {
                        val query = tokens.orderByKey().equalTo(receiverId)
                        query.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (shot in dataSnapshot.children) {
                                    val token = shot.getValue(Token::class.java)
                                    database.reference.child("Users").child(senderId)
                                        .addListenerForSingleValueEvent(object :
                                            ValueEventListener {
                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                val user = dataSnapshot.getValue(User::class.java)
                                                val username = user!!.userName.toString()
                                                val data = NotificationData(
                                                    senderId,
                                                    R.drawable.logo,
                                                    "$username: $message",
                                                    groupName,
                                                    receiverId
                                                )
                                                val sender = Sender(data, token!!.token)

                                                apiService.sendNotification(sender).enqueue(object :
                                                    Callback<Response> {
                                                    override fun onFailure(
                                                        call: Call<Response>,
                                                        t: Throwable
                                                    ) {
                                                    }

                                                    override fun onResponse(
                                                        call: Call<Response>,
                                                        response: retrofit2.Response<Response>
                                                    ) {
                                                        if (response.code() == 200) {
                                                            if (response.body()!!.success != 1) {
                                                                Toast.makeText(
                                                                    applicationContext,
                                                                    "Failed!",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
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
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && data.data != null) {
            Log.i("glupost", data.data.toString())
            val message = Message(senderId, data.data.toString())
            message.image = true
            message.timestamp = Date().time.toString()
            val snapshot = database.reference.child("Group Chats").child(groupId).push()
            snapshot.setValue(message).addOnSuccessListener {
                var storageReference = storage.reference.child("Chat Images").child(snapshot.key!!)
                storageReference.putFile(data.data!!).addOnSuccessListener {
                    storageReference.downloadUrl.addOnSuccessListener {
                        //database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!).child("profileImage").setValue(uri.toString())
                        //ponovo ucitat poruke
                        loadData()
                        if (notify) {
                            sendNotification(groupId, groupName, senderId!!, "Image")
                        }
                        notify = false
                    }
                }
                /*val snapshot1 = database.reference.child("Group Chats").child(receiverRoom).push()
                snapshot1.setValue(message).addOnSuccessListener {
                    storageReference = storage.reference.child("Chat Images").child(snapshot1.key!!)
                    storageReference.putFile(data.data!!).addOnSuccessListener {
                        storageReference.downloadUrl.addOnSuccessListener {
                            //database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!).child("profileImage").setValue(uri.toString())
                            //ponovo ucitat poruke
                            loadData()
                        }
                    }

                    if(notify) {
                        sendNotification(receiverId, senderId!!, "Image")
                    }
                    notify = false
                }
            }*/
            }
        }
    }

    private fun loadData() {
        database.reference.child("Group Chats").child(groupId).orderByChild("timestamp").limitToLast(recordPerPage).addValueEventListener(object:
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                messages.clear()
                for(snapshot in dataSnapshot.children) {
                    val message = snapshot.getValue(Message::class.java)
                    message?.messageId = snapshot.key
                    messages.add(message!!)
                }
                chatAdapter.notifyDataSetChanged()
                if(chatAdapter.itemCount != 0) {
                    binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}
