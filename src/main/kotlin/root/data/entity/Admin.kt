package root.data.entity

import java.time.OffsetDateTime
import javax.persistence.*

/**
 * Created by knekrasov on 10/24/2018.
 */
@Entity
@Table(name = "groups_admin")
data class Admin (
    @Id
    var userId: Int,

    var firstName: String? = null,

    var lastName: String? = null,

    var userName: String? = null,

    var createDate: OffsetDateTime,

    @ManyToMany(fetch = FetchType.EAGER)
    var groups: List<Group>
)
