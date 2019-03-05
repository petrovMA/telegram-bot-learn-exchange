package root.data.entity

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "surveys")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Survey(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    var description: String? = null,

    @Column(nullable = false)
    var createDate: OffsetDateTime,

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinTable(
        name="questions_to_questions",
        joinColumns = [JoinColumn(name="surveys_id")],
        inverseJoinColumns = [JoinColumn(name="questions_id")]
    )
    var questions: Set<Question>,

    @ManyToOne(fetch = FetchType.LAZY)
    var campaign: Campaign
) {
    override fun toString(): String = "name:\n$name\ndescription:\n$description\ncreateTime:\n$createDate"

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + createDate.hashCode()
        result = 31 * result + questions.hashCode()
        result = 31 * result + campaign.hashCode()
        return result
    }

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

}