package root.data.entity.tasks

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import root.data.entity.ExcelEntity
import root.data.entity.UserInCampaign
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "register_on_exchange")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class RegisterOnExchange(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long? = null,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var registered: Boolean = false,

    @Column(nullable = false)
    var createDate: OffsetDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    var user: UserInCampaign
):ExcelEntity {
    override fun toString(): String = "id:\n$id\nemail:\n$email\nregistered:\n$registered\ncreateTime:\n$createDate\nuser:\n$user"
    override fun toHead() = arrayOf("id", "email", "registered", "createDate", "user")
    override fun toRow() = arrayOf("$id", email, "$registered", "$createDate", "$user")

    override fun hashCode(): Int = createDate.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegisterOnExchange

        if (id != other.id) return false
        if (email != other.email) return false
        if (createDate != other.createDate) return false

        return true
    }
}