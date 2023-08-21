package ba.etf.chatapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import ba.etf.chatapp.adapters.UsersAdapter
import ba.etf.chatapp.databinding.ActivityGroupChatDetailsBinding
import ba.etf.chatapp.models.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.HashMap

class GroupChatDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupChatDetailsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null
    private lateinit var usersAdapter: UsersAdapter
    private lateinit var alertDialog: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGroupChatDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        val layoutManager = LinearLayoutManager(this)
        binding.participants.layoutManager = layoutManager
        val users = ArrayList<User>()
        usersAdapter = UsersAdapter(users, this, false)
        binding.participants.adapter = usersAdapter
        database.reference.child("Group Participants").child(intent.getStringExtra("groupId")!!).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.i("parrr", dataSnapshot.children.toList().size.toString())
                for (snapshot in dataSnapshot.children) {
                    //users.add(snapshot.getValue(User::class.java)!!)
                    val uid = snapshot.value.toString().substring(8).dropLast(1)
                    database.reference.child("Users").child(uid).addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val user = dataSnapshot.getValue(User::class.java)!!
                            user.userId = dataSnapshot.key
                            users.add(user)
                            usersAdapter.notifyDataSetChanged()
                            //usersAdapter = UsersAdapter(users, applicationContext, false)
                            //binding.participants.adapter = usersAdapter
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        val groupId = intent.getStringExtra("groupId")
        var groupName = intent.getStringExtra("groupName")

        binding.groupName.setText(intent.getStringExtra("groupName"), TextView.BufferType.EDITABLE)
        //binding.groupName.hint = intent.getStringExtra("groupName") //xxx

        storage.reference.child("Profile Images").child(intent.getStringExtra("groupId")!!).downloadUrl.addOnSuccessListener {
            Picasso.get().load(it).placeholder(R.drawable.avatar).into(binding.groupImage)
        }

        binding.backArrow.setOnClickListener {
            /*val intent = Intent(this, GroupChatActivity::class.java)
            intent.putExtra("groupId", groupId)
            intent.putExtra("groupName", groupName)
            startActivity(intent)*/
            finish()
        }

        binding.nazivGrupe.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.groupName.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.saveButton.textSize = 14F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.deleteButton.textSize = 16F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.ucesnici.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4

        binding.saveButton.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        alertDialog = AlertDialog.Builder(this)
        alertDialog.setMessage("Jeste li sigurni da želite obrisati grupu?")
            .setPositiveButton("Da") { _, _ ->
                database.reference.child("Groups").child(groupId!!).removeValue().addOnSuccessListener {
                    database.reference.child("Group Chats").child(groupId).removeValue().addOnSuccessListener {
                        database.reference.child("Group Participants").child(groupId).removeValue().addOnSuccessListener {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
            }
            .setNegativeButton("Ne") { dialog, _ ->
                dialog.cancel()
            }
        val alert = alertDialog.create()
        alert.setTitle("Obriši grupu")
        binding.deleteButton.setOnClickListener {
            alert.show()
        }

        binding.plus.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 25)
        }

        binding.saveButton.setOnClickListener {
            if(binding.groupName.text.toString() != "") {
                groupName = binding.groupName.text.toString()
                if(imageUri != null) {
                    val storageReference = storage.reference.child("Profile Images").child(groupId!!)
                    storageReference.putFile(imageUri!!).addOnSuccessListener {
                        storageReference.downloadUrl.addOnSuccessListener {
                            database.reference.child("Groups")
                                .child(groupId).child("image")
                                .setValue(imageUri.toString())
                        }
                    }
                }
                val obj = HashMap<String, Any>()
                obj["groupName"] = groupName!!
                database.reference.child("Groups").child(groupId!!).updateChildren(obj).addOnSuccessListener {
                    /*val users = UsersSelectionAdapter.selectedUsers
                    for(user in users) {
                        database.reference.child("Group Participants").child(groupId!!).push().setValue(object { val userId = user.userId!! })
                    }
                    database.reference.child("Group Participants").child(groupId!!).push().setValue(object { val userId = FirebaseAuth.getInstance().uid!! }).addOnSuccessListener {
                        val intent = Intent(applicationContext, GroupChatActivity::class.java)
                        intent.putExtra("groupId", groupId)
                        intent.putExtra("groupName", groupName)
                        //intent.putExtra("image", imageUri.toString())
                        startActivity(intent)
                    }*/
                }
                Toast.makeText(this, "Group updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(data != null && data.data != null) {
            imageUri = data.data
            binding.groupImage.setImageURI(imageUri)
        }
    }
}