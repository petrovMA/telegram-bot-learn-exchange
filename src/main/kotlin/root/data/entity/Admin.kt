package root.data.entity

import java.time.OffsetDateTime
import javax.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy

/**
 * Created by knekrasov on 10/24/2018.
 */
@Entity
@Table(name = "admin_for_camp")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Admin (
    @Id
    var userId: Int,

    var firstName: String? = null,

    var lastName: String? = null,

    var userName: String? = null,

    var createDate: OffsetDateTime,

    @ManyToMany(fetch = FetchType.LAZY)
    var campaigns: Set<Campaign>
)
