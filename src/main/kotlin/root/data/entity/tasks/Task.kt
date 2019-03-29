package root.data.entity.tasks

import root.data.entity.ExcelEntity
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
abstract class Task: ExcelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    open var id: Long? = null

    @Column(nullable = false)
    open var createDate: OffsetDateTime = OffsetDateTime.now()
}