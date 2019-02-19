package root.entity

import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id


import javax.persistence.*

@Entity
@Table(name = "Article")
class Article(
        @Id
        var id: Int?,
        var firstName: String?,
        var lastName: String?,
        var userName: String,
        var theme: String,
        var approve: Boolean?,
        var userId: Int,
        var url: String
) {
    fun toRow() = "$id;$firstName;$lastName;$userName;$theme;$approve;$userId;$url"
    override fun toString() = "id = $id\nfirstName = $firstName\nlastName = $lastName\nuserName = $userName\ntheme = $theme\napprove = $approve\nuserId = $userId\nurl = $url"
}