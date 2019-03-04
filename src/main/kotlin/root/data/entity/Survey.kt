package root.data.entity

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "surveys")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Survey(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long? = null,

    var name: String,

    var description: String? = null,

    var createDate: OffsetDateTime,

    @OneToMany(fetch = FetchType.LAZY)
    var questions: Set<Question>
)