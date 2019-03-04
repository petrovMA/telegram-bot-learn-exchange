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

    var text: String,

    var createDate: OffsetDateTime,

    @OneToMany(fetch = FetchType.LAZY)
    var options: Set<Option>
)