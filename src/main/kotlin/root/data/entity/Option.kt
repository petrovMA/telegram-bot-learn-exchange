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

    @Column(nullable = false)
    var text: String,

    var sortPoints: Int = Int.MAX_VALUE,

    @Column(nullable = false)
    var value: Int = 0
) {
    override fun toString(): String = "Option text:\n$text\nvalue:\n$value\nsort_point:\n$sortPoints"
    override fun hashCode() = id?.toInt() ?: 0
    override fun equals(other: Any?): Boolean =
        this.javaClass == other?.javaClass && let {
            other is Option && (id == other.id)
                    && (text == other.text)
                    && (sortPoints == other.sortPoints)
                    && (value == other.value)
        }
}