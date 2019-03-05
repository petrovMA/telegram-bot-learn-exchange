package root.data.entity

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "questions")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long? = null,

    @Column(nullable = false)
    var text: String,

    var sortPoints: Int? = null,

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinTable(
        name="questions_to_options",
        joinColumns = [JoinColumn(name="questions_id")],
        inverseJoinColumns = [JoinColumn(name="options_id")]
    )
    var options: Set<Option>
) {
    override fun toString(): String = "Question text:\n$text\nsort_point:\n$sortPoints"
    override fun hashCode(): Int = 31 + text.hashCode()
    override fun equals(other: Any?): Boolean =
        this.javaClass == other?.javaClass && let {
            other is Question && (id == other.id)
                    && (text == other.text)
                    && (sortPoints == other.sortPoints)
                    && (options == other.options)
        }
}