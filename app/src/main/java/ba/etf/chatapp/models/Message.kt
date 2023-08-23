package ba.etf.chatapp.models

class Message {
    var uId: String? = null
    var message: String? = null
    var messageId: String? = null
    var timestamp: String? = null
    var image = false
    var record = false

    constructor(uId: String?, message: String?) {
        this.uId = uId
        this.message = message
    }

    constructor()
}