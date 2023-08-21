package ba.etf.chatapp.notifications

class NotificationData {
    lateinit var user: String
    var icon = 0
    lateinit var body: String
    lateinit var title: String
    lateinit var sent: String

    constructor(user: String, icon: Int, body: String, title: String, sent: String) {
        this.user = user
        this.icon = icon
        this.body = body
        this.title = title
        this.sent = sent
    }

    constructor()
}