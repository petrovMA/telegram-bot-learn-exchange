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
    var groupId: Long,

    var createDate: OffsetDateTime,

    @OneToMany(fetch = FetchType.LAZY)
    var groups: Set<Group>,

    @ManyToMany(fetch = FetchType.LAZY)
    var users: Set<UserInGroup>
)