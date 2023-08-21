package ba.etf.chatapp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import ba.etf.chatapp.databinding.ActivityImageShowBinding
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class ImageShowActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageShowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageShowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        supportActionBar?.hide()

        val image = intent.getStringExtra("image")
        val storage = FirebaseStorage.getInstance()
        storage.reference.child("Chat Images").child(image!!).downloadUrl.addOnSuccessListener {
            Picasso.get().load(it).placeholder(R.drawable.image_icon).into(binding.image)
        }
        binding.userName.text = intent.getStringExtra("userName")

        binding.userName.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 4

        binding.layout.setOnClickListener {
            finish()
        }
    }
}