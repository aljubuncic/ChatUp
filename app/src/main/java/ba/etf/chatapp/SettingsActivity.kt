package ba.etf.chatapp

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import ba.etf.chatapp.MainActivity.Companion.context
import ba.etf.chatapp.adapters.EmergencyContactSelectionAdapter
import ba.etf.chatapp.adapters.UsersAdapter
import ba.etf.chatapp.adapters.UsersSelectionAdapter
import ba.etf.chatapp.databinding.ActivityAddParticipantsBinding
import ba.etf.chatapp.databinding.ActivitySettingsBinding
import ba.etf.chatapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.collections.ArrayList

class SettingsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var users: ArrayList<User>
    private lateinit var usersAdapter: EmergencyContactSelectionAdapter
    private lateinit var currentUser: User
    companion object {
        lateinit var binding: ActivitySettingsBinding
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        database.reference.child("Users").child(auth.uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    currentUser = dataSnapshot.getValue(User::class.java)!!
                    currentUser.userId = dataSnapshot.key!!
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            }
            )


        binding.saveButton.setOnClickListener {
            if(binding.txtUsername.text.toString() != "") {
                val username = binding.txtUsername.text.toString()

                val obj = HashMap<String, Any>()
                obj["userName"] = username

                database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!)
                    .updateChildren(obj)

                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show()
            }
        }

        binding.username.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.txtUsername.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.saveButton.textSize = 14F + ApplicationSettingsActivity.textSizeIncrease * 4

        binding.saveButton.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        binding.saveEmergencyContactButton.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                storage = FirebaseStorage.getInstance()
                storage.reference.child("Profile Images").child(FirebaseAuth.getInstance().uid!!).downloadUrl.addOnSuccessListener {
                    Picasso.get().load(it).placeholder(R.drawable.avatar).into(binding.profileImage)
                }
                binding.txtUsername.setText(user!!.userName, TextView.BufferType.EDITABLE)
                if(user != null && (user.parent || user.teacher)) {
                    binding.emergencyContactSelect.removeAllViews()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        binding.backArrow.setOnClickListener {
            finish()
        }

        binding.plus.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 25)
        }

        users = ArrayList()
        usersAdapter = EmergencyContactSelectionAdapter(users,null)
        binding.emergencyRecyclerView.adapter = usersAdapter
        binding.saveEmergencyContactButton.isEnabled = false

        val layoutManager = LinearLayoutManager(this)
        binding.emergencyRecyclerView.layoutManager = layoutManager

        getUsersWithRole("teacher") { teacherUsers ->
            // Add all teacher users to the users list
            users.addAll(teacherUsers)

            // After teacher users are added, get parent users
            getUsersWithRole("parent") { parentUsers ->
                // If there's a specific condition you want to check with parent users
                // (like filtering based on parentMail), you can do it here
                for (parent in parentUsers) {
                    if (parent.mail == currentUser.parentMail) {
                        users.add(parent)
                    }
                }
                //Log.d("Users:", users[0].mail.toString())
                // Notify adapter after both teacher and parent users are added
                usersAdapter.notifyDataSetChanged()

                displayAssignedEmergencyContact()
                usersAdapter.notifyDataSetChanged()

                addListenerToSaveEmergencyContactButton()
            }
        }
    }

    private fun getUsersWithRole(roleName: String,callback: (ArrayList<User>) -> Unit) {
        var userList = ArrayList<User>()
        database.reference.child("Users").orderByChild(roleName).equalTo(true)
            .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (childSnapshot in dataSnapshot.children) {
                    val user = childSnapshot.getValue(User::class.java)
                    if (user != null) {
                        user.userId = childSnapshot.key!!
                        userList.add(user)
                    }
                }
                //Log.d("User list", userList[0].mail.toString())
                callback(userList)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(userList)
            }
        })

    }

    private fun addListenerToSaveEmergencyContactButton() {
        binding.saveEmergencyContactButton.setOnClickListener {
            if(binding.saveEmergencyContactButton.isEnabled){
                assignEmergencyContactMailToCurrentUser(EmergencyContactSelectionAdapter.selectedUser!!)
            }
        }
    }

    private fun assignEmergencyContactMailToCurrentUser(emergencyContact: User) {
        val obj = HashMap<String, Any>()
        obj["emergencyContactMail"] = emergencyContact.mail!!
        Log.d("emergencyMail", emergencyContact.mail!!)
        database.reference.child("Users").child(currentUser.userId!!)
            .updateChildren(obj).addOnSuccessListener {
                Toast.makeText(this, "Emergency contact successfully added", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayAssignedEmergencyContact() {
        val emergencyContact = users.find {
            it.mail.equals(currentUser.emergencyContactMail)
        }
        usersAdapter = EmergencyContactSelectionAdapter(users,emergencyContact)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(data != null && data.data != null) {
            val uri = data.data
            binding.profileImage.setImageURI(uri)

            val storageReference = storage.reference.child("Profile Images").child(FirebaseAuth.getInstance().uid!!)

            storageReference.putFile(uri!!).addOnSuccessListener {
                storageReference.downloadUrl.addOnSuccessListener {
                    database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!).child("profileImage").setValue(uri.toString())
                }
            }

        }
    }
}