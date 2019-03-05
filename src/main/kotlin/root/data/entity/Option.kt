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

    var text: String,

    var sortPoints: Int? = null,

    var value: Int = Int.MAX_VALUE
) {
    override fun toString(): String = "Option text:\n$text\nvalue:\n$value\nsort_point:\n$sortPoints"
    override fun hashCode(): Int = 31 + text.hashCode()
    override fun equals(other: Any?): Boolean =
        this.javaClass == other?.javaClass && let {
            other is Option && (id == other.id)
                    && (text == other.text)
                    && (sortPoints == other.sortPoints)
                    && (value == other.value)
        }
}