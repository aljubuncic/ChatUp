package ba.etf.chatapp.models

class GroupChat {
    var groupId: String? = null
    var groupName: String? = null
    var image: String? = null

    constructor(groupName: String?, image: String?) {
        this.groupName = groupName
        this.image = image
    }

    constructor(groupName: String?) {
        this.groupName = groupName
    }

    constructor()
}