package ba.etf.chatapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import ba.etf.chatapp.adapters.UsersSelectionAdapter
import ba.etf.chatapp.databinding.ActivityGroupChatSettingsBinding
import ba.etf.chatapp.models.GroupChat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class GroupChatSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupChatSettingsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGroupChatSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        binding.nazivGrupe.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.groupName.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.saveButton.textSize = 14F + ApplicationSettingsActivity.textSizeIncrease * 4

        binding.saveButton.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        binding.saveButton.setOnClickListener {
            if(binding.groupName.text.toString() != "") {
                val groupName = binding.groupName.text.toString()
                val groupChat = GroupChat(groupName)

                database.reference.child("Groups").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        var max = 0
                        dataSnapshot.children.toList().forEach { c ->
                            if(c.key!!.toInt() > max) max = c.key!!.toInt()
                        }
                        //val  groupId = (dataSnapshot.children.toList().stream(). + 1).toString()
                        val groupId = (max + 1).toString()
                        if(imageUri != null) {
                            groupChat.image = imageUri.toString()
                            val storageReference = storage.reference.child("Profile Images").child(groupId)
                            storageReference.putFile(imageUri!!).addOnSuccessListener {
                                storageReference.downloadUrl.addOnSuccessListener {
                                    database.reference.child("Groups")
                                        .child(groupId).child("image")
                                        .setValue(imageUri.toString())
                                }
                            }
                        }
                        database.reference.child("Groups").child(groupId).setValue(groupChat).addOnSuccessListener {
                            val users = UsersSelectionAdapter.selectedUsers
                            for(user in users) {
                                database.reference.child("Group Participants").child(groupId).push().setValue(object { val userId = user.userId!! })
                            }
                            database.reference.child("Group Participants").child(groupId).push().setValue(object { val userId = FirebaseAuth.getInstance().uid!! }).addOnSuccessListener {
                                val intent = Intent(applicationContext, GroupChatActivity::class.java)
                                intent.putExtra("groupId", groupId)
                                intent.putExtra("groupName", groupName)
                                //intent.putExtra("image", imageUri.toString())
                                startActivity(intent)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
                Toast.makeText(this, "Group created", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Please enter group name", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backArrow.setOnClickListener {
            /*val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)*/
            finish()
        }

        binding.plus.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 25)
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