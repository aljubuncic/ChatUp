package ba.etf.chatapp

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ba.etf.chatapp.models.Message
import ba.etf.chatapp.adapters.ChatAdapter
import ba.etf.chatapp.databinding.ActivityChatDetailsBinding
import ba.etf.chatapp.models.User
import ba.etf.chatapp.notifications.APIService
import ba.etf.chatapp.notifications.Client
import ba.etf.chatapp.notifications.NotificationData
import ba.etf.chatapp.notifications.Response
import ba.etf.chatapp.notifications.Sender
import ba.etf.chatapp.notifications.Token
import com.devlomi.record_view.RecordButton
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
import java.util.*
import java.util.jar.Manifest
import java.util.stream.Collectors
import kotlin.collections.ArrayList

class ChatDetailsActivity : AppCompatActivity() {
    companion object {
        lateinit var binding: ActivityChatDetailsBinding
        lateinit var chatAdapter: ChatAdapter
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
    private lateinit var receiverId: String
    private lateinit var senderRoom: String
    private lateinit var receiverRoom: String

    private lateinit var messages: ArrayList<Message>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()

        supportActionBar?.hide()

        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        senderId = auth.uid!!
        receiverId = intent.getStringExtra("userId")!!
        val userName = intent.getStringExtra("userName")
        ///val profileImage = intent.getStringExtra("profileImage")

        binding.toolbar.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        binding.chatLinearLayout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        binding.userName.text = userName
        //Picasso.get().load(profileImage).placeholder(R.drawable.avatar).into(binding.profileImage)
        storage = FirebaseStorage.getInstance()
        storage.reference.child("Profile Images").child(receiverId).downloadUrl.addOnSuccessListener {
            Picasso.get().load(it).placeholder(R.drawable.avatar).into(binding.profileImage)
        }

        binding.backArrow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            //finish()
        }

        binding.sendVoice.isListenForRecord = false
        binding.sendVoice.setOnClickListener {
            binding.sendVoice.isListenForRecord = true
        }


        messages = ArrayList()
        chatAdapter = ChatAdapter(messages, this)

        binding.chatRecyclerView.adapter = chatAdapter

        val linearLayoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.layoutManager = linearLayoutManager

        senderRoom = senderId + receiverId
        receiverRoom = receiverId + senderId

        binding.sendMessage.setOnClickListener {
            notify = true
            val msg = binding.enterMessage.text.toString()
            val message = Message(senderId, msg)
            message.timestamp = Date().time.toString()
            binding.enterMessage.setText("")

            database.reference.child("Chats").child(senderRoom).push().setValue(message).addOnSuccessListener {
                database.reference.child("Chats").child(receiverRoom).push().setValue(message).addOnSuccessListener {
                    if(notify) {
                        sendNotification(receiverId, senderId, msg)
                    }
                    notify = false
                }
            }
        }

        binding.enterMessage.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 2
        binding.userName.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 3

        binding.plus.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/* video/*" //radi li video?
            startActivityForResult(intent, 25)
        }

        /*database.reference.child("Chats").child(senderRoom).orderByChild("timestamp").limitToLast(recordPerPage).addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                messages.clear()
                for(snapshot in dataSnapshot.children) {
                    val message = snapshot.getValue(Message::class.java)
                    message?.messageId = snapshot.key
                    messages.add(message!!)
                }
                chatAdapter.notifyDataSetChanged()
                Log.i("ddd", messages.stream().map { m -> m.message }.collect(Collectors.toList()).toString())
                if(chatAdapter.itemCount != 0) {
                    binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })*/
        loadData()

        //val firstVisibleItemPosition: Int = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        binding.chatRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0/* && firstVisibleItemPosition == 0*/ && !isLoadMore && messages.isNotEmpty()) {
                    Log.i("dddd", messages[0].timestamp!!.toString())
                    ///loadMoreData((-1 * messages[messages.size - 1].timestamp!!).toString())
                    //loadMoreData((-1 * messages[0].timestamp!!).toString())
                    loadMoreData(messages[0].timestamp!!)
                }
            }

            private fun loadMoreData(timestamp: String) {
                if (!isLoadMore && !firstMessage) {
                    //binding.loadMoreProgressbar.visibility = View.VISIBLE
                    isLoadMore = true
                    //currentPage++
                    /*val list = ArrayList<Message>()
                    database.reference.child("Chats").child(senderRoom).orderByChild("timestamp").endBefore(timestamp).limitToLast(recordPerPage)
                        .addValueEventListener(object: ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for(snapshot in dataSnapshot.children.reversed()) {
                                    val message = snapshot.getValue(Message::class.java)
                                    message?.messageId = snapshot.key
                                    list.add(0, message!!)
                                }
                                Log.i("dddddddddd", list.stream().map { m -> m.message }.collect(Collectors.toList()).toString())
                                chatAdapter.notifyDataSetChanged()
                                isLoadMore = false
                            }
                            override fun onCancelled(error: DatabaseError) {
                            }
                        })*/

                    Log.i("dddtime", timestamp)
                    database.reference.child("Chats").child(senderRoom).orderByChild("timestamp").endBefore(timestamp).limitToLast(recordPerPage)
                        .addValueEventListener(object: ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for(snapshot in dataSnapshot.children.reversed()) {
                                val message = snapshot.getValue(Message::class.java)
                                message?.messageId = snapshot.key
                                messages.add(0, message!!)
                            }
                            Log.i("ddddd", messages.stream().map { m -> m.message }.collect(Collectors.toList()).toString())
                            chatAdapter.notifyDataSetChanged()
                            binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                            isLoadMore = false
                        }
                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                    /*for (i in messages.size - 1..currentPage * recordPerPage) {
                        messages.add(0, "chat message $i")
                    }*/
                    /*Handler(Looper.getMainLooper()).postDelayed({
                        binding.chatRecyclerView.post {
                            chatAdapter.setData(ArrayList(messages.subList(0, recordPerPage)))
                        }
                        isLoadMore = false
                        //dataBinding.loadMoreProgressbar.visibility = View.INVISIBLE
                    }, 1000)*/
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data != null && data.data != null) {
            Log.i("glupost", data.data.toString())
            val message = Message(senderId, data.data.toString())
            message.image = true
            message.timestamp = Date().time.toString()
            val snapshot = database.reference.child("Chats").child(senderRoom).push()
            snapshot.setValue(message).addOnSuccessListener {
                var storageReference = storage.reference.child("Chat Images").child(snapshot.key!!)
                storageReference.putFile(data.data!!).addOnSuccessListener {
                    storageReference.downloadUrl.addOnSuccessListener {
                        //database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!).child("profileImage").setValue(uri.toString())
                        //ponovo ucitat poruke
                        loadData()
                    }
                }
                val snapshot1 = database.reference.child("Chats").child(receiverRoom).push()
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
            }


            //msm da ne treba dok se ne pritisne save

            sendNotification(receiverId, senderId!!, "Image")
            //finish()
            //startActivity(intent)
        }

    }

    private fun sendNotification(receiverId: String, senderId: String, message: String) {
        val tokens = database.getReference("Tokens")
        val query = tokens.orderByKey().equalTo(receiverId)
        query.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for(snapshot in dataSnapshot.children) {
                    val token = snapshot.getValue(Token::class.java)
                    database.reference.child("Users").child(senderId).addListenerForSingleValueEvent(object :ValueEventListener {
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
                                            Toast.makeText(applicationContext, "Failed!", Toast.LENGTH_SHORT).show()
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

    private fun loadData() {
        database.reference.child("Chats").child(senderRoom).orderByChild("timestamp").limitToLast(recordPerPage).addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                messages.clear()
                for(snapshot in dataSnapshot.children) {
                    val message = snapshot.getValue(Message::class.java)
                    message?.messageId = snapshot.key
                    messages.add(message!!)
                }
                chatAdapter.notifyDataSetChanged()
                Log.i("ddd", messages.stream().map { m -> m.message }.collect(Collectors.toList()).toString())
                if(chatAdapter.itemCount != 0) {
                    binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    /*fun requestRecording() {
        ActivityCompat.requestPermissions(this, {android.Manifest.permission.RECORD_AUDIO}, REQUEST_)
    }*/
}

//mislim da ne treba notify atr
//sendnotif u sender ili receiver room