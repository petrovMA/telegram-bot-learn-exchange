package root.data.entity

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "common_campaign")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class CommonCampaign(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var name: String,

    @Column(nullable = false)
    var createDate: OffsetDateTime,

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "common_campaign_to_missions",
        joinColumns = [JoinColumn(name = "common_campaign_id")],
        inverseJoinColumns = [JoinColumn(name = "mission_id")]
    )
    var missions: Set<Survey>
) : ExcelEntity {

    override fun toHead() = arrayOf("id", "name", "createDate", "missions")
    override fun toRow() =
        arrayOf("$id", name, "$createDate", missions.joinToString("\n") { "${it.id} ${it.name}" })

    override fun hashCode() = id?.toInt() ?: 0
}