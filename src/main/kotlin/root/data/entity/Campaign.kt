package root.data.entity

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "campaign")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Campaign(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long? = null,

    @Column(unique = true)
    var name: String,

    var createDate: OffsetDateTime,

    @OneToMany(fetch = FetchType.LAZY)
    var groups: Set<Group>,

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var surveys: Set<Survey>
)