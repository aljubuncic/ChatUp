package ba.etf.chatapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ba.etf.chatapp.databinding.ActivitySignUpAsBinding

class SignUpAsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpAsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpAsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.parent.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            intent.putExtra("person", "parent")
            startActivity(intent)
        }

        binding.child.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            intent.putExtra("person", "child")
            startActivity(intent)
        }

        binding.teacher.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            intent.putExtra("person", "teacher")
            startActivity(intent)
        }
    }
}