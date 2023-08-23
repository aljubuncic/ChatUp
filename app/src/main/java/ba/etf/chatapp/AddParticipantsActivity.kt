package ba.etf.chatapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ba.etf.chatapp.adapters.UsersSelectionAdapter
import ba.etf.chatapp.databinding.ActivityAddParticipantsBinding
import ba.etf.chatapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddParticipantsActivity : AppCompatActivity() {

    companion object {
        lateinit var binding: ActivityAddParticipantsBinding
    }
    private lateinit var users: ArrayList<User>
    private lateinit var database: FirebaseDatabase
    private lateinit var usersAdapter: UsersSelectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddParticipantsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseDatabase.getInstance()
        users = ArrayList()

        supportActionBar?.hide()

        binding.next.setOnClickListener {
            if(binding.next.isEnabled) {
                val intent = Intent(this, GroupChatSettingsActivity::class.java)
                startActivity(intent)
            }
        }

        binding.backArrow.setOnClickListener {
            finish()
        }

        usersAdapter = UsersSelectionAdapter(users)
        binding.contactsRecyclerView.adapter = usersAdapter

        val layoutManager = LinearLayoutManager(this)
        binding.contactsRecyclerView.layoutManager = layoutManager

        binding.next.textSize = 18F + ApplicationSettingsActivity.textSizeIncrease * 2
        binding.add.textSize = 20F + ApplicationSettingsActivity.textSizeIncrease * 2

        binding.next.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        binding.titleLayout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        binding.searchLinearLayout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.appTheme)

        binding.search.setOnClickListener {
            val username = binding.searchText.text.toString()
            if(username == "") {
                database.reference.child("Users").addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        setUsers(dataSnapshot)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }
            else {
                database.reference.child("Users").orderByChild("userName").equalTo(username)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            setUsers(dataSnapshot)
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
            }
        }

        database.reference.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                setUsers(dataSnapshot)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun setUsers(data: DataSnapshot) {
        users.clear()
        if (FirebaseAuth.getInstance().uid != null) {
            database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val currentUser = dataSnapshot.getValue(User::class.java)
                        if (data.children.toList().isNotEmpty()) {
                            for (snapshot in data.children) {
                                val user = snapshot.getValue(User::class.java)
                                user!!.userId = snapshot.key!!

                                if (user.userId != FirebaseAuth.getInstance().uid) {
                                    if (currentUser!!.parent) {
                                        database.reference.child("Users").orderByChild("parentMail")
                                            .equalTo(currentUser.mail)
                                            .addListenerForSingleValueEvent(object :
                                                ValueEventListener {
                                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                    for (snapshot1 in dataSnapshot.children) {
                                                        if (!(user.parent || !user.parent && !user.teacher && user.mail != snapshot1.getValue(
                                                                User::class.java
                                                            )!!.mail)
                                                        ) {
                                                            users.add(user)
                                                            users.sortBy { it.userName }
                                                            usersAdapter.notifyDataSetChanged()
                                                        }
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                }
                                            })
                                    } else if (currentUser.teacher) {
                                        users.add(user)
                                        users.sortBy { it.userName }
                                        usersAdapter.notifyDataSetChanged()
                                    } else {
                                        if (!user.parent) {
                                            users.add(user)
                                            users.sortBy { it.userName }
                                            usersAdapter.notifyDataSetChanged()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }
    }
}