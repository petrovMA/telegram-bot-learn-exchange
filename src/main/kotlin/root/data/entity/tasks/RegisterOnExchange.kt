//package root.data.entity.tasks
//
//import org.hibernate.annotations.Cache
//import org.hibernate.annotations.CacheConcurrencyStrategy
//import root.data.entity.Campaign
//import root.data.entity.tasks.surveus.Question
//import java.time.OffsetDateTime
//import javax.persistence.*
//
//@Entity
//@Table(name = "register_on_exchange")
//@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
//data class RegisterOnExchange(
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE)
//    override var id: Long? = null,
//
//    @Column(nullable = false)
//    var email: String,
//
//    @Column(nullable = false)
//    override var createDate: OffsetDateTime
//) : Task {
//    override fun toString(): String = "email:\n$email\ncreateTime:\n$createDate"
//    override fun toHead() = arrayOf("id", "email", "createDate")
//    override fun toRow() = arrayOf("$id", email, "$createDate")
//
//    override fun hashCode(): Int = createDate.hashCode()
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as RegisterOnExchange
//
//        if (id != other.id) return false
//        if (email != other.email) return false
//        if (createDate != other.createDate) return false
//
//        return true
//    }
//}