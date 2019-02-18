package root.entity

import javax.persistence.*

@Entity
@Table(name = "Like")
data class Like(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_Sequence")
        var id: Int?,
        var userName: String,
        var messageId: Int,
        var userId: Int,
        var url: String,
        var like: Boolean
) {

    fun toRow() = "$id;$userName;$messageId;$userId;$url;$like"

    override fun toString() =
            "id = $id\nuserName = $userName\nmessageId = $messageId\nuserId = $userId\nurl = $url\nlike = $like"
}