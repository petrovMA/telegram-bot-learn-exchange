package root.data.entity.tasks

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import root.data.entity.UserInCampaign
import root.data.entity.tasks.surveus.Survey
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "passed_tasks")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class PassedTask(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long? = null,

    @Column(nullable = false)
    var value: Int,

    var description: String? = null,

    @Column(nullable = false)
    var passDate: OffsetDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    var survey: Survey,

    @ManyToOne(fetch = FetchType.LAZY)
    var user: UserInCampaign
)