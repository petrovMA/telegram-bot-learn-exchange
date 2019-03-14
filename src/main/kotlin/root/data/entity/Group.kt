package root.data.entity

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "exchange_group")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Group(
    @Id
    var groupId: Long,

    @Column(nullable = false)
    var createDate: OffsetDateTime
) {
    override fun hashCode() = groupId.toInt()
}