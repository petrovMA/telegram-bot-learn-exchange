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

    var sortPoints: Int? = null,

    var text: String,

    @OneToMany(fetch = FetchType.LAZY)
    var options: Set<Option>
) {
    override fun toString(): String = "Question text:\n$text\nsort_point:\n$sortPoints"
}