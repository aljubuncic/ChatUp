package ba.etf.chatapp.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService

class FirebaseIdService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val user = FirebaseAuth.getInstance().currentUser
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            if(user != null) {
                updateToken(it)
            }
        }
    }

    private fun updateToken(newToken: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val database = FirebaseDatabase.getInstance().getReference("Tokens")
        //val database = FirebaseDatabase.getInstance().reference.child("Tokens")
        val token = Token(newToken)
        database.child(user!!.uid).setValue(token)
    }
}