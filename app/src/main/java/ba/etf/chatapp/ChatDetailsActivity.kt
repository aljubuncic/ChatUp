package ba.etf.chatapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ba.etf.chatapp.adapters.ChatAdapter
import ba.etf.chatapp.databinding.ActivityChatDetailsBinding
import ba.etf.chatapp.models.Message
import ba.etf.chatapp.models.User
import ba.etf.chatapp.notifications.APIService
import ba.etf.chatapp.notifications.Client
import ba.etf.chatapp.notifications.NotificationData
import ba.etf.chatapp.notifications.Response
import ba.etf.chatapp.notifications.Sender
import ba.etf.chatapp.notifications.Token
import com.devlomi.record_view.OnRecordListener
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
import java.io.File
import java.util.*

class ChatDetailsActivity : AppCompatActivity() {
    companion object {
        lateinit var binding: ActivityChatDetailsBinding
        lateinit var chatAdapter: ChatAdapter
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var isLoadMore = false
    private var firstMessage = false
    private val recordPerPage = 20
    private lateinit var storage: FirebaseStorage
    private var notify = false
    private lateinit var apiService: APIService

    private lateinit var senderId: String
    private lateinit var receiverId: String
    private lateinit var senderRoom: String
    private lateinit var receiverRoom: String

    private lateinit var messages: ArrayList<Message>

    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var audioPath: String

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

        binding.toolbar.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        binding.chatLinearLayout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        binding.userName.text = userName
        storage = FirebaseStorage.getInstance()
        storage.reference.child("Profile Images").child(receiverId).downloadUrl.addOnSuccessListener {
            Picasso.get().load(it).placeholder(R.drawable.avatar).into(binding.profileImage)
        }

        binding.backArrow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.enterMessage.addTextChangedListener {
            if(binding.enterMessage.text.toString().isNotEmpty() && binding.enterMessage.text.toString() != "") {
                binding.sendVoice.visibility = View.GONE
                binding.sendMessage.visibility = View.VISIBLE
            } else {
                binding.sendVoice.visibility = View.VISIBLE
                binding.sendMessage.visibility = View.GONE
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0);
        } else {
            binding.sendVoice.isListenForRecord = false
        }
        binding.sendVoice.setRecordView(binding.recordView)
        binding.sendVoice.isListenForRecord = false
        binding.sendVoice.setOnClickListener {
            binding.sendVoice.isListenForRecord = true
        }

        binding.recordView.setOnRecordListener(object : OnRecordListener {
            override fun onStart() {
                //Start Recording..
                Log.d("RecordView", "onStart")
                setupRecording()

                mediaRecorder.prepare()
                mediaRecorder.start()

                binding.recordView.visibility = View.VISIBLE
                binding.enterMessage.visibility = View.GONE
                binding.plus.visibility = View.GONE
            }

            override fun onCancel() {
                //On Swipe To Cancel
                Log.d("RecordView", "onCancel")

                mediaRecorder.reset()
                mediaRecorder.release()

                val file = File(audioPath)
                if(file.exists()) file.delete()

                binding.recordView.visibility = View.GONE
                binding.enterMessage.visibility = View.VISIBLE
                binding.plus.visibility = View.VISIBLE
            }

            override fun onFinish(recordTime: Long, limitReached: Boolean) {
                //Stop Recording..
                //limitReached to determine if the Record was finished when time limit reached.
                Log.d("RecordView", "onFinish")

                mediaRecorder.stop()
                mediaRecorder.release()

                binding.recordView.visibility = View.GONE
                binding.enterMessage.visibility = View.VISIBLE
                binding.plus.visibility = View.VISIBLE

                sendRecordingMessage(audioPath)
            }

            override fun onLessThanSecond() {
                //When the record time is less than One Second
                Log.d("RecordView", "onLessThanSecond")

                mediaRecorder.reset()
                mediaRecorder.release()

                val file = File(audioPath)
                if(file.exists()) file.delete()

                binding.recordView.visibility = View.GONE
                binding.enterMessage.visibility = View.VISIBLE
                binding.plus.visibility = View.VISIBLE
            }

            override fun onLock() {
                //When Lock gets activated
                Log.d("RecordView", "onLock")
            }
        })

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

            binding.sendVoice.visibility = View.VISIBLE
            binding.sendMessage.visibility = View.GONE
        }

        binding.enterMessage.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 2
        binding.userName.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 3

        binding.plus.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/* video/*"
            startActivityForResult(intent, 25)
        }

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

                    database.reference.child("Chats").child(senderRoom).orderByChild("timestamp").endBefore(timestamp).limitToLast(recordPerPage)
                        .addListenerForSingleValueEvent(object: ValueEventListener {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data != null && data.data != null) {
            val message = Message(senderId, data.data.toString())
            message.image = true
            message.timestamp = Date().time.toString()
            val snapshot = database.reference.child("Chats").child(senderRoom).push()
            snapshot.setValue(message).addOnSuccessListener {
                var storageReference = storage.reference.child("Chat Images").child(snapshot.key!!)
                storageReference.putFile(data.data!!).addOnSuccessListener {
                    storageReference.downloadUrl.addOnSuccessListener {
                        loadData()
                    }
                }
                val snapshot1 = database.reference.child("Chats").child(receiverRoom).push()
                snapshot1.setValue(message).addOnSuccessListener {
                    storageReference = storage.reference.child("Chat Images").child(snapshot1.key!!)
                    storageReference.putFile(data.data!!).addOnSuccessListener {
                        storageReference.downloadUrl.addOnSuccessListener {
                            loadData()
                        }
                    }

                    if(notify) {
                        sendNotification(receiverId, senderId, "Slika")
                    }
                    notify = false
                }
            }

            sendNotification(receiverId, senderId, "Slika")
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
                if(chatAdapter.itemCount != 0) {
                    binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun setupRecording() {
        mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

        val file = File(filesDir, "Records")
        if(!file.exists()) file.mkdirs()
        audioPath = file.absolutePath + File.separator + System.currentTimeMillis() + ".3gp"

        mediaRecorder.setOutputFile(audioPath)
    }

    private fun sendRecordingMessage(audioPath: String) {
        val snapshot = database.reference.child("Chats").child(senderRoom).push()
        val snapshot1 = database.reference.child("Chats").child(receiverRoom).push()
        val storageReference = storage.reference.child("Records").child(snapshot.key!!)
        val storageReference1 = storage.reference.child("Records").child(snapshot1.key!!)
        val audioFile = Uri.fromFile(File(audioPath))
        storageReference.putFile(audioFile).addOnSuccessListener { success ->
            success.storage.downloadUrl.addOnCompleteListener { path ->
                if(path.isSuccessful) {
                    val url = path.result.toString()
                    val message = Message(senderId, url)
                    message.record = true
                    message.timestamp = Date().time.toString()
                    snapshot.setValue(message)
                }
            }
        }
        storageReference1.putFile(audioFile).addOnSuccessListener { success ->
            success.storage.downloadUrl.addOnCompleteListener { path ->
                if(path.isSuccessful) {
                    val url = path.result.toString()
                    val message = Message(senderId, url)
                    message.record = true
                    message.timestamp = Date().time.toString()
                    snapshot1.setValue(message)
                }
            }
        }
        sendNotification(receiverId, senderId, "Glasovna poruka")
    }
}