package root.data.entity.tasks.surveus

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
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
    var value: Int = 0,

    @Column(nullable = false)
    var correct: Boolean = false
) {
    override fun toString(): String = "Option text:\t$text\nvalue:\t$value\nsort_point:\t$sortPoints\ncorrect:\t$correct"
    override fun hashCode() = id?.toInt() ?: 0
    override fun equals(other: Any?): Boolean =
        this.javaClass == other?.javaClass && let {
            other is Option && (id == other.id)
                    && (text == other.text)
                    && (sortPoints == other.sortPoints)
                    && (value == other.value)
        }
}