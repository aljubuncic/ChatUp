package ba.etf.chatapp.notifications


class Sender {
    lateinit var data: NotificationData
    lateinit var to: String

    constructor(data: NotificationData, to: String) {
        this.data = data
        this.to = to
    }

    constructor()
}