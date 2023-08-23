package ba.etf.chatapp

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.SeekBar
import ba.etf.chatapp.adapters.UsersAdapter
import ba.etf.chatapp.databinding.ActivityApplicationSettingsBinding

class ApplicationSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityApplicationSettingsBinding

    companion object {
        var photoIncrease = false
        var textSizeIncrease = 0
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplicationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        if(UsersAdapter.photoIncreased) binding.increasePhotos.isChecked = true
        binding.increaseTextsize.progress = UsersAdapter.textSizeIncreased
        binding.text1.textSize = 20F + UsersAdapter.textSizeIncreased * 5
        binding.text2.textSize = 20F + UsersAdapter.textSizeIncreased * 5
        binding.text3.textSize = 20F + UsersAdapter.textSizeIncreased * 5

        binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        binding.increasePhotos.setOnCheckedChangeListener { _, _ ->
            photoIncrease = binding.increasePhotos.isChecked
        }

        binding.yellow.setOnClickListener {
            MainActivity.appTheme = "#ffd362"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.orange.setOnClickListener {
            MainActivity.appTheme = "#ff914d"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.red.setOnClickListener {
            MainActivity.appTheme = "#8c042b"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.lightred.setOnClickListener {
            MainActivity.appTheme = "#fb6767"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.darkpink.setOnClickListener {
            MainActivity.appTheme = "#7d3865"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.lightpink.setOnClickListener {
            MainActivity.appTheme = "#e58fac"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.purple.setOnClickListener {
            MainActivity.appTheme = "#af62ff"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.darkblue.setOnClickListener {
            MainActivity.appTheme = "#155790"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.lightblue.setOnClickListener {
            MainActivity.appTheme = "#7bc1fa"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.lightgreen.setOnClickListener {
            MainActivity.appTheme = "#77d59d"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.darkgreen.setOnClickListener {
            MainActivity.appTheme = "#0E8374"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }
        binding.gray.setOnClickListener {
            MainActivity.appTheme = "#7d8184"
            binding.layout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor(MainActivity.appTheme)
            MainActivity.setTheme(MainActivity.appTheme)
        }

        binding.increaseTextsize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                textSizeIncrease = progress
                binding.text1.textSize = 20F + progress * 5
                binding.text2.textSize = 20F + progress * 5
                binding.text3.textSize = 20F + progress * 5
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        binding.backArrow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}