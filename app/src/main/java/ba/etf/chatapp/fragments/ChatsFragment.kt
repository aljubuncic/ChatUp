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
import ba.etf.chatapp.databinding.FragmentChatsBinding
import ba.etf.chatapp.models.GroupChat
import ba.etf.chatapp.models.User
import ba.etf.chatapp.notifications.Token
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ChatsFragment : Fragment() {
    private lateinit var binding: FragmentChatsBinding
    private lateinit var users: ArrayList<User>
    private lateinit var database: FirebaseDatabase

    //private lateinit var chats: ArrayList<String>
    //private lateinit var mapForSorting: HashMap<User, Date>
    private lateinit var usersAdapter: UsersAdapter

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var search: LinearLayout
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatsBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance()
        users = ArrayList()

        usersAdapter = UsersAdapter(users, requireContext(), true)
        binding.chatRecyclerView.adapter = usersAdapter

        val layoutManager = LinearLayoutManager(context)
        binding.chatRecyclerView.layoutManager = layoutManager

        search = binding.searchLinearLayout
        binding.searchLinearLayout.setBackgroundColor(Color.parseColor(MainActivity.appTheme))
        /*val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(MainActivity.theme)*/

        binding.search.setOnClickListener {
            val username = binding.searchText.text.toString()
            if(username == "") setUsers("")
            else {
                database.reference.child("Users").orderByChild("userName").equalTo(username)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.children.toList().isEmpty()) {
                                users.clear()
                                usersAdapter.notifyDataSetChanged()
                            } else {
                                for (snapshot in dataSnapshot.children) {
                                    //val user = snapshot.getValue(User::class.java)
                                    setUsers(snapshot.key!!)
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })

                database.reference.child("Groups").orderByChild("groupName").equalTo(username)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.children.toList().isEmpty()) {
                                users.clear()
                                usersAdapter.notifyDataSetChanged()
                            } else {
                                for (snapshot in dataSnapshot.children) {
                                    //val user = snapshot.getValue(User::class.java)
                                    setUsers(snapshot.key!!)
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
            }
        }

        /*database.reference.child("Chats").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                users.clear()
                usersAdapter.notifyDataSetChanged()
                for (snapshot in dataSnapshot.children) {
                    //val chatId = (snapshot.getValue(Message::class.java) as Message).uId
                    val chatId = snapshot.key
                    if (chatId!!.contains(FirebaseAuth.getInstance().uid!!)) {
                        chats.add(chatId)
                    }
                }
                Log.i("chats", chats.toString())
                //usersAdapter.notifyDataSetChanged()

                database.reference.child("Users")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            setUsers(dataSnapshot)
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })*/
        /*val chats = ArrayList<String>()
        var mapForSorting = HashMap<User, Date>()
        database.reference.child("Chats").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                chats.clear()
                for (snapshot in dataSnapshot.children) {
                    //val chatId = (snapshot.getValue(Message::class.java) as Message).uId
                    val chatId = snapshot.key
                    if (chatId!!.contains(FirebaseAuth.getInstance().uid!!)) {
                        chats.add(chatId)
                    }
                }
                Log.i("chats", chats.toString())
                //usersAdapter.notifyDataSetChanged()

                database.reference.child("Users")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            users.clear()
                            for (snapshot in dataSnapshot.children) {
                                val user = snapshot.getValue(User::class.java)
                                user!!.userId = snapshot.key!!

                                //mozda prvi dio ne treba ako zelimo da se dopisujemo sami sa sobom
                                if (user.userId != FirebaseAuth.getInstance().uid && chats.any {
                                        it.contains(
                                            user.userId.toString()
                                        )
                                    }) {
                                    //users.add(user)
                                    mapForSorting = HashMap()
                                    FirebaseDatabase.getInstance().reference.child("Chats")
                                        .child(FirebaseAuth.getInstance().uid + user.userId)
                                        .orderByChild("timestamp").limitToLast(1)
                                        .addListenerForSingleValueEvent(object :
                                            ValueEventListener {
                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                for (snapshot in dataSnapshot.children) {
                                                    ///mapForSorting[user] = Date(snapshot.child("timestamp").value as Long * 1000)
                                                    mapForSorting[user] = Date(
                                                        snapshot.child("timestamp").value.toString()
                                                            .toLong() * 1000
                                                    )
                                                    val sortedMap = mapForSorting.toList()
                                                        .sortedByDescending { (_, value) -> value }
                                                        .toMap()
                                                    users.clear()
                                                    sortedMap.forEach { entry -> users.add(entry.key) }
                                                    usersAdapter.notifyDataSetChanged()
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                            }
                                        })
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })*/
        setUsers("")

        FirebaseMessaging.getInstance().token.addOnSuccessListener { updateToken(it) }

        return binding.root
    }

    private fun setUsers(uid: String) {
        val chats = ArrayList<String>()
        var mapForSorting = HashMap<User, Date>()
        database.reference.child("Chats").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                chats.clear()
                users.clear()
                for (snapshot in dataSnapshot.children) {
                    val chatId = snapshot.key
                    if (FirebaseAuth.getInstance().uid != null && chatId!!.contains(FirebaseAuth.getInstance().uid!!)) {
                        chats.add(chatId)
                    }
                }

                database.reference.child("Users")
                    .addValueEventListener(object : ValueEventListener {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            users.clear()
                            usersAdapter.notifyDataSetChanged()
                            mapForSorting = HashMap()
                            for (snapshot in dataSnapshot.children) {
                                val user = snapshot.getValue(User::class.java)
                                user!!.userId = snapshot.key!!

                                //mozda prvi dio ne treba ako zelimo da se dopisujemo sami sa sobom
                                if (user.userId != FirebaseAuth.getInstance().uid && chats.any {
                                        it.contains(
                                            user.userId.toString()
                                        )
                                    }) {
                                    //users.add(user)
                                    ////mapForSorting = HashMap()
                                    FirebaseDatabase.getInstance().reference.child("Chats")
                                        .child(FirebaseAuth.getInstance().uid + user.userId)
                                        .orderByChild("timestamp").limitToLast(1)
                                        .addValueEventListener(object :
                                            ValueEventListener {
                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                for (snapshot in dataSnapshot.children) {
                                                    ///mapForSorting[user] = Date(snapshot.child("timestamp").value as Long * 1000)
                                                    mapForSorting[user] = Date(
                                                        snapshot.child("timestamp").value.toString()
                                                            .toLong() * 1000
                                                    )
                                                    val sortedMap = mapForSorting.toList()
                                                        .sortedByDescending { (_, value) -> value }
                                                        .toMap()
                                                    users.clear()
                                                    sortedMap.forEach { entry ->
                                                        if(entry.key.userName == "SVI") users.add(0, entry.key)
                                                        else users.add(entry.key)
                                                    }
                                                    usersAdapter.notifyDataSetChanged()
                                                }
                                                if(uid != "") users.removeIf { u -> u.userId != uid }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                            }
                                        })
                                }
                            }

                            val groups = ArrayList<String>()
                            database.reference.child("Group Participants").addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    groups.clear()
                                    for (snapshot in dataSnapshot.children) {
                                        val groupId = snapshot.key
                                        for (user in snapshot.children) {
                                            if (FirebaseAuth.getInstance().uid == user.value.toString()
                                                    .substring(8).dropLast(1)
                                            ) {
                                                groups.add(groupId!!)
                                            }
                                        }
                                    }
                                        ////mapForSorting = HashMap()
                                    for(gr in groups) {
                                        Log.i("grrr", gr)
                                        database.reference.child("Groups").child(gr).addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                val group = dataSnapshot.getValue(GroupChat::class.java)
                                                val user = User(gr, group!!.groupName!!)
                                                if(group.image != null) {
                                                    user.profileImage = group.image
                                                }
                                                FirebaseDatabase.getInstance().reference.child("Group Chats")
                                                    .child(gr)
                                                    .orderByChild("timestamp").limitToLast(1)
                                                    .addValueEventListener(object :
                                                        ValueEventListener {
                                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                            for (snapshot in dataSnapshot.children) {
                                                                ///mapForSorting[user] = Date(snapshot.child("timestamp").value as Long * 1000)
                                                                mapForSorting[user] = Date(
                                                                    snapshot.child("timestamp").value.toString()
                                                                        .toLong() * 1000
                                                                )
                                                                val sortedMap = mapForSorting.toList()
                                                                    .sortedByDescending { (_, value) -> value }
                                                                    .toMap()
                                                                users.clear()
                                                                sortedMap.forEach { entry ->
                                                                    if(entry.key.userName == "SVI") users.add(0, entry.key)
                                                                    else users.add(entry.key)
                                                                }
                                                                usersAdapter.notifyDataSetChanged()
                                                            }
                                                            if(uid != "") users.removeIf { u -> u.userId != uid }
                                                        }

                                                        override fun onCancelled(error: DatabaseError) {
                                                        }

                                                    })
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                            }
                                        })
                                    }
                                    /*for (gp in groups) {
                                        if(users.filter { u -> u.userId == gp }.isEmpty())
                                            users.add()
                                    }*/
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        /*val groups = ArrayList<String>()
        database.reference.child("Group Participants").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                groups.clear()
                for (snapshot in dataSnapshot.children) {
                    val groupId = snapshot.key
                    for(user in snapshot.children) {
                        if (FirebaseAuth.getInstance().uid == user.value.toString().substring(8).dropLast(1)) {
                            groups.add(groupId!!)
                        }
                    }
                    ////mapForSorting = HashMap()
                    for(gr in groups) {
                        database.reference.child("Groups").child(gr).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val group = snapshot.getValue(GroupChat::class.java)
                                val user = User(gr, group!!.groupName!!, "", "", group.image!!)
                                FirebaseDatabase.getInstance().reference.child("Group Chats")
                                    .child(gr)
                                    .orderByChild("timestamp").limitToLast(1)
                                    .addListenerForSingleValueEvent(object :
                                        ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            for (snapshot in dataSnapshot.children) {
                                                ///mapForSorting[user] = Date(snapshot.child("timestamp").value as Long * 1000)
                                                mapForSorting[user] = Date(
                                                    snapshot.child("timestamp").value.toString()
                                                        .toLong() * 1000
                                                )
                                                val sortedMap = mapForSorting.toList()
                                                    .sortedByDescending { (_, value) -> value }
                                                    .toMap()
                                                users.clear()
                                                sortedMap.forEach { entry -> users.add(entry.key) }
                                                usersAdapter.notifyDataSetChanged()
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                        }
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })*/
    }

    private fun updateToken(token: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val database = FirebaseDatabase.getInstance().getReference("Tokens")
        val token1 = Token(token)
        database.child(user!!.uid).setValue(token1)
    }
}

