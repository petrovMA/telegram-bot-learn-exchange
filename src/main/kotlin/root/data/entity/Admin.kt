package root.data.entity

import java.time.OffsetDateTime
import javax.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.telegram.telegrambots.meta.api.objects.User

@Entity
@Table(name = "admin_for_camp")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Admin(
    @Id
    var userId: Int,

    var firstName: String? = null,

    var lastName: String? = null,

    var userName: String? = null,

    @Column(nullable = false)
    var createDate: OffsetDateTime,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "campaign_to_admin_for_camp",
        joinColumns = [JoinColumn(name = "admin_for_camp_id")],
        inverseJoinColumns = [JoinColumn(name = "campaign_id")]
    )
    var campaigns: Set<Campaign>
) : ExcelEntity {
    override fun equals(other: Any?): Boolean = when (other) {
        is User -> {
            other.id == this.userId && other.firstName == this.firstName && other.lastName == this.lastName && other.userName == this.userName
        }
        is Admin -> {
            other.userId == this.userId && other.firstName == this.firstName && other.lastName == this.lastName && other.userName == this.userName
        }
        else -> false
    }

    fun update(user: Any?) = when (user) {
        is User -> {
            this.userId = user.id
            this.firstName = user.firstName
            this.lastName = user.lastName
            this.userName = user.userName
        }
        is Admin -> {
            this.userId = user.userId
            this.firstName = user.firstName
            this.lastName = user.lastName
            this.userName = user.userName
        }
        else -> TODO("throw exception here")
    }

    override fun hashCode(): Int {
        var result = userId
        result = 31 * result + (firstName?.hashCode() ?: 0)
        result = 31 * result + (lastName?.hashCode() ?: 0)
        result = 31 * result + (userName?.hashCode() ?: 0)
        result = 31 * result + createDate.hashCode()
        result = 31 * result + campaigns.hashCode()
        return result
    }

    override fun toHead(): Array<String> =
        arrayOf("user_Id", "user_Name", "first_Name", "last_Name", "create_date", "campaign")

    override fun toRow(): Array<String> =
        arrayOf(
            "${userId.toLong()}",
            "$userName",
            "$firstName",
            "$lastName",
            "$createDate",
            campaigns.joinToString("\n") { it.name }
        )
}