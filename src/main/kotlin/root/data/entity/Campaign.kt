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

    @Column(unique = true, nullable = false)
    var name: String,

    @Column(nullable = false)
    var createDate: OffsetDateTime,

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "campaign_to_exchange_group",
        joinColumns = [JoinColumn(name = "campaign_id")],
        inverseJoinColumns = [JoinColumn(name = "exchange_group_id")]
    )
    var groups: HashSet<Group>
) : ExcelEntity() {

    override fun toHead() = arrayOf("id", "name", "createDate", "groups")
    override fun toRow() =
        arrayOf("$id", name, "$createDate", groups.joinToString("\n") { "${it.groupId} ${it.createDate}" })

    override fun hashCode() = id?.toInt() ?: 0
}