package root.data.entity

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "options")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Option(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long? = null,

    var sortPoints: Int? = null,

    var value: Int,

    var text: String
) {
    override fun toString(): String = "Option text:\n$text\nvalue:\n$value\nsort_point:\n$sortPoints"
}