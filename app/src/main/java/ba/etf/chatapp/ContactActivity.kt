package ba.etf.chatapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import ba.etf.chatapp.databinding.ActivityContactBinding
import ba.etf.chatapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class ContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val userId = intent.getStringExtra("userId")
        val userName = intent.getStringExtra("userName")
        val profileImage = intent.getStringExtra("profileImage")

        binding.userName.text = userName
        storage = FirebaseStorage.getInstance()
        storage.reference.child("Profile Images").child(userId!!).downloadUrl.addOnSuccessListener {
            Picasso.get().load(it).placeholder(R.drawable.avatar).into(binding.profileImage)
        }

        binding.userName.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 5
        binding.messageButton.textSize = 14F + ApplicationSettingsActivity.textSizeIncrease * 4

        binding.messageButton.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        val person = intent.getStringExtra("person")
        if (person != null && person.toString() == "teacher") {
            binding.description1.text = "Vaspitač"
            binding.description2.visibility = View.GONE
        }
        else if (person != null && person.toString() == "parent") {
            binding.description1.text = "Dijete: "
            var children = ""
            database.reference.child("Users").orderByChild("parentMail").equalTo(intent.getStringExtra("email").toString()).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for(snapshot in dataSnapshot.children) {
                        children += snapshot.getValue(User::class.java)!!.userName + " "
                    }
                    binding.description2.text = children
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
        else {
            binding.description1.text = "Roditelj: "
            database.reference.child("Users").orderByChild("mail").equalTo(intent.getStringExtra("parentEmail").toString()).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for(snapshot in dataSnapshot.children) {
                        binding.description2.text = snapshot.getValue(User::class.java)!!.userName
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }

        binding.messageButton.setOnClickListener {
            val intent = Intent(this, ChatDetailsActivity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("profileImage", profileImage)
            intent.putExtra("userName", userName)
            startActivity(intent)
        }

        binding.backArrow.setOnClickListener {
            finish()
        }
    }
}

