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
    var createDate: OffsetDateTime
) : ExcelEntity() {

    override fun toHead() = arrayOf("id", "name", "createDate")
    override fun toRow() =
        arrayOf("$id", name, "$createDate")

    override fun hashCode() = id?.toInt() ?: 0
}