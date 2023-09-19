package ba.etf.chatapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ba.etf.chatapp.databinding.ActivitySignUpBinding
import ba.etf.chatapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var progressDialog: ProgressDialog

    private var parent = false
    private var teacher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()

        supportActionBar?.hide()

        val person = intent.getStringExtra("person").toString()
        if (person == "parent") parent = true
        if (person == "teacher") teacher = true
        if (!parent && !teacher) binding.parentEmail.visibility = View.VISIBLE

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Creating Account")
        progressDialog.setMessage("We are creating your account.")

        binding.txtUsername.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.txtEmail.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.txtPassword.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.txtAlreadyHaveAccount.textSize = 16F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.btnSignUp.textSize = 14F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.parentEmail.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4

        binding.btnSignUp.setOnClickListener {
            if(binding.txtUsername.text.toString().isNotEmpty() && binding.txtEmail.text.toString().isNotEmpty() && binding.txtPassword.text.toString().isNotEmpty()) {
                progressDialog.show()
                val user = User(binding.txtUsername.text.toString(), binding.txtEmail.text.toString(), binding.txtPassword.text.toString())
                if (parent) user.parent = true
                if (teacher) user.teacher = true
                if (parent || teacher) {
                    auth.createUserWithEmailAndPassword(binding.txtEmail.text.toString(), binding.txtPassword.text.toString())
                        .addOnCompleteListener { task ->
                            progressDialog.dismiss()
                            if (task.isSuccessful) {
                                val id = task.result?.user?.uid
                                if (id != null) {
                                    database.reference.child("Users").child(id).setValue(user)
                                    Toast.makeText(applicationContext, "Uspješno ste registrovani!", Toast.LENGTH_SHORT).show()
                                    if (teacher) {
                                        database.reference.child("Groups").orderByChild("groupName").equalTo("SVI").addListenerForSingleValueEvent(object :
                                            ValueEventListener {
                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                for(snapshot in dataSnapshot.children) {
                                                    database.reference.child("Group Participants").child(snapshot.key!!).push().setValue(object { val userId = id })
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                            }
                                        })
                                    }
                                    val intent = Intent(applicationContext, MainActivity::class.java)
                                    startActivity(intent)
                                }
                            }
                            else {
                                Toast.makeText(this, "Korisnik sa unesenim emailom već postoji!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                else {
                    val parentEmail = binding.parentEmail.text.toString()
                    if (parentEmail.isNotEmpty()) {
                        database.reference.child("Users").orderByChild("mail").equalTo(parentEmail).addListenerForSingleValueEvent(object :
                            ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    var exist = false
                                    for(snapshot in dataSnapshot.children) {
                                        exist = true
                                        if (!snapshot.getValue(User::class.java)!!.parent) {
                                            progressDialog.dismiss()
                                            exist = false
                                            break
                                        }
                                        auth.createUserWithEmailAndPassword(binding.txtEmail.text.toString(), binding.txtPassword.text.toString())
                                            .addOnCompleteListener { task ->
                                                progressDialog.dismiss()
                                                if (task.isSuccessful) {
                                                    val id = task.result?.user?.uid
                                                    if (id != null) {
                                                        user.parentMail = parentEmail
                                                        database.reference.child("Users").child(id).setValue(user)
                                                        database.reference.child("Groups").orderByChild("groupName").equalTo("SVI").addListenerForSingleValueEvent(object :
                                                            ValueEventListener {
                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                for(snapshot1 in dataSnapshot.children) {
                                                    database.reference.child("Group Participants").child(snapshot1.key!!).push().setValue(object { val userId = id })
                                                }
                                                Toast.makeText(applicationContext, "Uspješno ste registrovani!", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(applicationContext, MainActivity::class.java)
                                                startActivity(intent)
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                            }
                                        })
                                                    }
                                                    else {
                                                        Toast.makeText(applicationContext, "Korisnik sa unesenim emailom već postoji!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                        }
                                    }
                                    if (!exist) {
                                        progressDialog.dismiss()
                                        Toast.makeText(applicationContext, "Roditelj sa unesenim emailom ne postoji!", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                    }
                    else {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Unesite podatke", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Unesite podatke", Toast.LENGTH_SHORT).show()
            }
        }

        binding.txtAlreadyHaveAccount.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }
}