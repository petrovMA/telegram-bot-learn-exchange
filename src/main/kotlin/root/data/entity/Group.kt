package root.data.entity

import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "exchange_group")
data class Group(
    @Id
    var groupId: Long,

    var createDate: OffsetDateTime
)