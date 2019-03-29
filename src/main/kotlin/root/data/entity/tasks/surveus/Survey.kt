package root.data.entity.tasks.surveus

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import root.data.entity.Campaign
import root.data.entity.tasks.TaskCampaign
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "surveys")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Survey(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    override var id: Long? = null,

    @Column(nullable = false)
    override var name: String,

    var description: String? = null,

    @Column(nullable = false)
    override var createDate: OffsetDateTime,

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinTable(
        name = "survey_to_questions",
        joinColumns = [JoinColumn(name = "surveys_id")],
        inverseJoinColumns = [JoinColumn(name = "questions_id")]
    )
    var questions: Set<Question>,

    @ManyToOne(fetch = FetchType.LAZY)
    override var campaign: Campaign
) : TaskCampaign() {
    override fun toString(): String = "name:\n$name\ndescription:\n$description\ncreateTime:\n$createDate"

    override fun hashCode(): Int = createDate.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Survey

        if (id != other.id) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (createDate != other.createDate) return false
        if (questions != other.questions) return false
        if (campaign != other.campaign) return false

        return true
    }

    override fun toHead() = arrayOf("id", "name", "description", "createDate", "questions", "campaign_name")
    override fun toRow() = arrayOf("$id", name, "$description", "$createDate", "$questions", "${campaign?.name}")
}