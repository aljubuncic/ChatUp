package ba.etf.chatapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import ba.etf.chatapp.adapters.FragmentsAdapter
import ba.etf.chatapp.databinding.ActivityMainBinding
import ba.etf.chatapp.fragments.ChatsFragment
import ba.etf.chatapp.fragments.ContactsFragment
import ba.etf.chatapp.models.User
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
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        binding.viewPager.adapter = FragmentsAdapter(supportFragmentManager)
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.setBackgroundColor(Color.parseColor(appTheme))
        database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if(user != null && (user.parent || user.teacher)) {
                    binding.tabLayout.removeTabAt(2)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        appTheme = "#7bc1fa"

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
            TODO("Not yet implemented")
        }

        override fun onClientStarted(client: SinchClient) {
            TODO("Not yet implemented")
        }

        override fun onCredentialsRequired(clientRegistration: ClientRegistration) {
        }

        override fun onLogMessage(level: Int, area: String, message: String) {
            TODO("Not yet implemented")
        }

        override fun onPushTokenRegistered() {
            TODO("Not yet implemented")
        }

        override fun onPushTokenRegistrationFailed(error: SinchError) {
            TODO("Not yet implemented")
        }

        override fun onPushTokenUnregistered() {
            TODO("Not yet implemented")
        }

        override fun onPushTokenUnregistrationFailed(error: SinchError) {
            TODO("Not yet implemented")
        }

        override fun onUserRegistered() {
            TODO("Not yet implemented")
        }

        override fun onUserRegistrationFailed(error: SinchError) {
            TODO("Not yet implemented")
        }
    }
}