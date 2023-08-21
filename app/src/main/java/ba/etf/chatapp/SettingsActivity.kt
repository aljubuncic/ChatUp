package ba.etf.chatapp

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import ba.etf.chatapp.databinding.ActivitySettingsBinding
import ba.etf.chatapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

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
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                //Picasso.get().load(user!!.profileImage).placeholder(R.drawable.avatar3).into(binding.profileImage)
                storage = FirebaseStorage.getInstance()
                storage.reference.child("Profile Images").child(FirebaseAuth.getInstance().uid!!).downloadUrl.addOnSuccessListener {
                    Picasso.get().load(it).placeholder(R.drawable.avatar).into(binding.profileImage)
                }
                binding.txtUsername.setText(user!!.userName, TextView.BufferType.EDITABLE)
                //binding.txtUsername.hint = user!!.userName
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

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
            val uri = data.data
            Log.i("uri", uri.toString())
            binding.profileImage.setImageURI(uri)

            val storageReference = storage.reference.child("Profile Images").child(FirebaseAuth.getInstance().uid!!)


            //msm da ne treba dok se ne pritisne save
            storageReference.putFile(uri!!).addOnSuccessListener {
                storageReference.downloadUrl.addOnSuccessListener {
                    database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!).child("profileImage").setValue(uri.toString())
                }
            }

        }
    }
}