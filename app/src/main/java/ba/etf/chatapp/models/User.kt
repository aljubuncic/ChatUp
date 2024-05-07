package ba.etf.chatapp.models

class User {
    var userName: String? = null
    var mail: String? = null
    var password: String? = null
    var profileImage: String? = null
    var userId: String? = null
    var status: String? = null
    var parent = false
    var teacher = false
    var parentMail: String? = null
    var emergencyContactMail: String? = null

    constructor(userName: String, mail: String, password: String) {
        this.userName = userName
        this.mail = mail
        this.password = password
    }

    constructor(userId: String, userName: String) {
        this.userId = userId
        this.userName = userName
    }

    constructor()
}
