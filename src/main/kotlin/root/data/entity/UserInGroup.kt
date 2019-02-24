package root.data.entity

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.OffsetDateTime
import javax.persistence.*

/**
 * Created by knekrasov on 10/24/2018.
 */
@Entity
@Table(name = "user_in_group")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class UserInGroup (
    @Id
    var userId: Int,

    var firstName: String? = null,

    var lastName: String? = null,

    var userName: String? = null,

    var createDate: OffsetDateTime
)
