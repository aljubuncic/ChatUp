package ba.etf.chatapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import ba.etf.chatapp.ChatDetailsActivity
import ba.etf.chatapp.MainActivity
import ba.etf.chatapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationsService  : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val sent = remoteMessage.data["sent"]
        val user = FirebaseAuth.getInstance().currentUser

        Log.i("jupi", sent.toString())
        if(user != null && sent.toString() == user.uid) {
            Log.i("jupi", "eeee")
            sendNotification(remoteMessage)
        }

        /*if (remoteMessage.notification != null) {
            remoteMessage.notification!!.title?.let {
                remoteMessage.notification!!.body?.let { it1 ->
                    showNotification(
                        it,
                        it1
                    )
                }
            }
        }*/
    }

    private fun sendNotification(remoteMessage: RemoteMessage) {
        val user = remoteMessage.data["user"]
        val icon = remoteMessage.data["icon"]
        val title = remoteMessage.data["title"]
        val body = remoteMessage.data["body"]

        val notification = remoteMessage.notification
        val channelId = "notification_channel"
        //val i = user!!.replace("[\\D", "").toInt()
        val intent = Intent(this, ChatDetailsActivity::class.java)
        intent.putExtra("userId", user)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        var builder: NotificationCompat.Builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon!!.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(sound)
            .setVibrate(
                longArrayOf(
                    1000, 1000, 1000,
                    1000, 1000
                )
            )
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
        builder = builder.setContent(
            getCustomDesign(title!!, body!!)
        )
        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager?

        if (Build.VERSION.SDK_INT
            >= Build.VERSION_CODES.O
        ) {
            val notificationChannel = NotificationChannel(
                channelId, "chat_app",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager!!.createNotificationChannel(
                notificationChannel
            )
        }
        //var j = 0
        //if(i > 0) j = i
        notificationManager!!.notify(0, builder.build())
    }

    private fun getCustomDesign(
        title: String,
        description: String
    ): RemoteViews {
        val remoteViews = RemoteViews(
            applicationContext.packageName,
            R.layout.notification
        )
        remoteViews.setTextViewText(R.id.title, title)
        remoteViews.setTextViewText(R.id.description, description)
        remoteViews.setImageViewResource(
            R.id.logo,
            R.drawable.logo
        )
        return remoteViews
    }

    private fun showNotification(
        title: String,
        message: String
    ) {
        val intent = Intent(this, MainActivity::class.java)
        val channelId = "notification_channel"

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        var builder: NotificationCompat.Builder = NotificationCompat.Builder(
            ApplicationProvider.getApplicationContext(),
            channelId
        )
            .setSmallIcon(R.drawable.logo)
            .setAutoCancel(true)
            .setVibrate(
                longArrayOf(
                    1000, 1000, 1000,
                    1000, 1000
                )
            )
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        builder = builder.setContent(
            getCustomDesign(title, message)
        )

        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager?
        // Check if the Android Version is greater than Oreo
        if (Build.VERSION.SDK_INT
            >= Build.VERSION_CODES.O
        ) {
            val notificationChannel = NotificationChannel(
                channelId, "web_app",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager!!.createNotificationChannel(
                notificationChannel
            )
        }
        notificationManager!!.notify(0, builder.build())
    }
}