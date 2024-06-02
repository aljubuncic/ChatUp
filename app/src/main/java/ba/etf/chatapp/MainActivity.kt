package ba.etf.chatapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import ba.etf.chatapp.adapters.FragmentsAdapter
import ba.etf.chatapp.databinding.ActivityMainBinding
import ba.etf.chatapp.fragments.ChatsFragment
import ba.etf.chatapp.fragments.ContactsFragment
import ba.etf.chatapp.fragments.FeelingsFragment
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
import com.sinch.android.rtc.ClientRegistration
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.SinchError
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallController
import com.sinch.android.rtc.calling.CallControllerListener
import com.sinch.android.rtc.calling.CallListener
import com.sinch.android.rtc.calling.MediaConstraints
import retrofit2.Callback
import java.util.Date
import kotlin.collections.HashMap
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.facebook.FacebookEmojiProvider

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var apiService: APIService
    private lateinit var currentUser: User

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        EmojiManager.install(FacebookEmojiProvider())

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this

        supportActionBar?.hide()

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        appTheme = "#7bc1fa"

        binding.viewPager.adapter = FragmentsAdapter(supportFragmentManager)
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.setBackgroundColor(Color.parseColor(appTheme))
        database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                currentUser = dataSnapshot.getValue(User::class.java)!!
                currentUser.userId = dataSnapshot.key!!

                if(currentUser != null && (currentUser.parent || currentUser.teacher)) {
                    binding.tabLayout.removeTabAt(2)
                    binding.sendEmergencyMessageButton.visibility = View.GONE
                }

                binding.sendEmergencyMessageButton.setOnClickListener {
                    if(currentUser.emergencyContactMail != null)
                        sendMessageToEmergencyContact()
                    else
                        Toast.makeText(context, "Kontakt za hitne slučajeve nije postavljen u postavkama profila", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        binding.layout.setBackgroundColor(Color.parseColor(appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(appTheme)

        val popupMenu = PopupMenu(this, binding.options)
        popupMenu.menuInflater.inflate(R.menu.menu,popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.editProfile -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.settings -> {
                    val intent = Intent(this, ApplicationSettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.groupChat -> {
                    val intent = Intent(this, AddParticipantsActivity::class.java)
                    startActivity(intent)
                }
                R.id.logout -> {
                    status("offline")
                    auth.signOut()
                    val intent = Intent(this, SignInActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }

        binding.options.setOnClickListener {
            popupMenu.show()
        }

        database.reference.child("Themes").child(FirebaseAuth.getInstance().uid!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.getValue(String::class.java)
                if(value != null) {
                    appTheme = dataSnapshot.getValue(String::class.java)!!
                }
                binding.tabLayout.setBackgroundColor(Color.parseColor(appTheme))
                binding.layout.setBackgroundColor(Color.parseColor(appTheme))
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = Color.parseColor(appTheme)
                ChatsFragment.search.setBackgroundColor(Color.parseColor(appTheme))
                ContactsFragment.search.setBackgroundColor(Color.parseColor(appTheme))
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        sinchClient = SinchClient.builder()
            .context(this)
            .applicationKey("5719a36c-c690-4065-b7f3-8b4bf78eb159")
            .environmentHost("ocra.api.sinch.com")
            .userId(FirebaseAuth.getInstance().uid!!)
            .build()

        sinchClient.addSinchClientListener(SinchClientListener())


        sinchClient.callController.addCallControllerListener(SinchCallControllerListener())
        sinchClient.start()
    }

    private fun sendMessageToEmergencyContact() {
        val message = Message(currentUser.userId, "UPOZORENJE")
        message.timestamp = Date().time.toString()
        message.isEmergency = true

        getEmergencyContact(currentUser.emergencyContactMail!!) {
            val senderId = currentUser.userId
            val receiverId = it.userId
            val senderRoom = senderId + receiverId
            val receiverRoom = receiverId + senderId
            database.reference.child("Chats").child(senderRoom).push().setValue(message).addOnSuccessListener {
                database.reference.child("Chats").child(receiverRoom).push().setValue(message).addOnSuccessListener {
                    Log.d("recieverId", "$receiverId")
                    Log.d("senderId", "$senderId")
                    Log.d("message", "${message.message}")
                    sendNotification(receiverId!!, senderId!!, message.message!!)
                }
            }
        }

    }

    private fun getEmergencyContact(emergencyContactMail: String, callback: (User) -> Unit) {
        database.reference.child("Users").orderByChild("mail").equalTo(emergencyContactMail)
            .addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.children.first().getValue(User::class.java)!!
                Log.d("user", user.mail.toString())
                user.userId = dataSnapshot.children.first().key!!
                callback(user)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
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
                            val data = NotificationData(senderId, R.drawable.logo, message, username, receiverId)
                            val sender = Sender(data, token!!.token)

                            apiService.sendNotification(sender).enqueue(object :
                                Callback<Response> {
                                override fun onFailure(call: retrofit2.Call<Response>?, t: Throwable?) {
                                }

                                override fun onResponse(call: retrofit2.Call<Response>?, response: retrofit2.Response<Response>?) {
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

    private fun status(status: String) {
        if(FirebaseAuth.getInstance().uid != null) {
            val reference =
                database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!)

            val hashMap = HashMap<String, Any>()
            hashMap["status"] = status

            reference.updateChildren(hashMap)
        }
    }

    override fun onResume() {
        super.onResume()
        status("online")
    }

    override fun onPause() {
        super.onPause()
        status("offline")
    }

    companion object {
        var appTheme = "#7bc1fa"
        private lateinit var sinchClient: SinchClient
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        var call: Call? = null
        fun callUser(userId: String) {
            if (call == null) {
                sinchClient.start()
                call =
                    sinchClient.callController.callUser(userId, MediaConstraints(false))
                call?.addCallListener(SinchCallListener())
                val dialog = AlertDialog.Builder(context).create()
                dialog.setTitle("Calling")
                dialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL,
                    "Hang up",
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        dialogInterface.dismiss()
                        call?.hangup()
                    })
                dialog.show()
            }
        }

        fun setTheme(theme: String) {
            FirebaseDatabase.getInstance().reference.child("Themes").child(FirebaseAuth.getInstance().uid!!).setValue(theme)
        }
    }

    private class SinchCallControllerListener : CallControllerListener {

        override fun onIncomingCall(callController: CallController, incomingCall: Call) {
            val dialog = AlertDialog.Builder(context).create()
            dialog.setTitle("Incoming Call")
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Reject", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
                call?.hangup()
            })
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Answer", DialogInterface.OnClickListener { dialogInterface, i ->
                call = incomingCall
                call?.answer()
                call?.addCallListener(SinchCallListener())
                Toast.makeText(context, "Call started", Toast.LENGTH_LONG).show()
            })
            dialog.show()
        }
    }

    private class SinchCallListener : CallListener {

        override fun onCallEnded(endedCall: Call) {
            Toast.makeText(context, "Call ended", Toast.LENGTH_LONG).show()
            call = null
            endedCall.hangup()
        }

        override fun onCallEstablished(call: Call) {
            Toast.makeText(context, "Call established", Toast.LENGTH_LONG).show()
        }

        override fun onCallProgressing(call: Call) {
            Toast.makeText(context, "Ringing...", Toast.LENGTH_LONG).show()
        }
    }

    private class SinchClientListener : com.sinch.android.rtc.SinchClientListener {
        override fun onClientFailed(client: SinchClient, error: SinchError) {

        }

        override fun onClientStarted(client: SinchClient) {
        }

        override fun onCredentialsRequired(clientRegistration: ClientRegistration) {
        }

        override fun onPushTokenRegistered() {
        }

        override fun onPushTokenRegistrationFailed(error: SinchError) {
        }

        override fun onPushTokenUnregistered() {
        }

        override fun onPushTokenUnregistrationFailed(error: SinchError) {
        }

        override fun onUserRegistered() {
        }

        override fun onUserRegistrationFailed(error: SinchError) {
        }
    }
}