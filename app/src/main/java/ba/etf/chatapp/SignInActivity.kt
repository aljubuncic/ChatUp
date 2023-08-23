package ba.etf.chatapp

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import ba.etf.chatapp.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()

        supportActionBar?.hide()

        progressDialog  = ProgressDialog(this)
        progressDialog.setTitle("Login")
        progressDialog.setMessage("Please wait,\nValidation in progress.")

        binding.txtEmail.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.txtPassword.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.txtClickSignUp.textSize = 16F + ApplicationSettingsActivity.textSizeIncrease * 4
        binding.btnSignIn.textSize = 14F + ApplicationSettingsActivity.textSizeIncrease * 4

        binding.btnSignIn.setOnClickListener {
            if(binding.txtEmail.text.toString().isNotEmpty() && binding.txtPassword.text.toString().isNotEmpty()) {
                progressDialog.show()
                auth.signInWithEmailAndPassword(binding.txtEmail.text.toString(), binding.txtPassword.text.toString())
                    .addOnCompleteListener { task ->
                        progressDialog.dismiss()
                        if (task.isSuccessful) {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Neispravni podaci!", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Unesite podatke", Toast.LENGTH_SHORT).show()
            }
        }

        if(auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.txtClickSignUp.setOnClickListener {
            val intent = Intent(this, SignUpAsActivity::class.java)
            startActivity(intent)
        }

        binding.txtForgotPassword.setOnClickListener {
            val email = binding.txtEmail.text.toString()
            if(email.isEmpty()) {
                Toast.makeText(this, "Unesite email", Toast.LENGTH_SHORT).show()
            }
            else {
                auth.sendPasswordResetEmail(email).addOnSuccessListener {
                    Toast.makeText(this, "Link za resetovanje šifre je poslan na uneseni email", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}