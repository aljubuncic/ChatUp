package ba.etf.chatapp.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import ba.etf.chatapp.MainActivity
import ba.etf.chatapp.adapters.UsersAdapter
import ba.etf.chatapp.databinding.FragmentContactsBinding
import ba.etf.chatapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ContactsFragment : Fragment() {
    private lateinit var binding: FragmentContactsBinding
    private lateinit var users: ArrayList<User>
    private lateinit var database: FirebaseDatabase
    private lateinit var usersAdapter: UsersAdapter

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var search: LinearLayout
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactsBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance()
        users = ArrayList()

        usersAdapter = UsersAdapter(users, requireContext(), false)
        binding.contactsRecyclerView.adapter = usersAdapter

        val layoutManager = LinearLayoutManager(context)
        binding.contactsRecyclerView.layoutManager = layoutManager

        search = binding.searchLinearLayout
        binding.searchLinearLayout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        /*val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.theme)*/

        binding.search.setOnClickListener {
            val username = binding.searchText.text.toString()
            if(username == "") {
                database.reference.child("Users").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        setUsers(dataSnapshot)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }
            else {
                database.reference.child("Users").orderByChild("userName").equalTo(username)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            setUsers(dataSnapshot)
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
            }
        }

        /*database.reference.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                users.clear()
                for(snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(User::class.java)
                    user!!.userId = snapshot.key!!

                    if(user.userId != FirebaseAuth.getInstance().uid) {
                        users.add(user)
                    }
                }
                users.sortBy { it.userName }
                usersAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })*/
        database.reference.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                setUsers(dataSnapshot)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        return binding.root
    }

    private fun setUsers(data: DataSnapshot) {
        users.clear()
        if (FirebaseAuth.getInstance().uid != null) {
            database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val currentUser = dataSnapshot.getValue(User::class.java)
                        Log.i("mailovi", currentUser!!.mail.toString())
                        if (data.children.toList().isNotEmpty()) {
                            for (snapshot in data.children) {
                                val user = snapshot.getValue(User::class.java)
                                user!!.userId = snapshot.key!!

                                if (user.userId != FirebaseAuth.getInstance().uid) {
                                    if (currentUser.parent) {
                                        database.reference.child("Users").orderByChild("parentMail")
                                            .equalTo(currentUser!!.mail)
                                            .addListenerForSingleValueEvent(object :
                                                ValueEventListener {
                                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                    for (snapshot1 in dataSnapshot.children) {
                                                        if (!(user.parent || !user.parent && !user.teacher && user.mail != snapshot1.getValue(
                                                                User::class.java
                                                            )!!.mail)
                                                        ) {
                                                            //|| !currentUser.parent && !currentUser.teacher && user.parent)) {
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
                            //users.sortBy { it.userName }
                        }
                        //usersAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }
    }
}