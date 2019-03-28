package root.data.entity.tasks

import root.data.entity.Campaign
import root.data.entity.ExcelEntity
import java.time.OffsetDateTime

abstract class Task(
    open var id: Long? = null,
    open var createDate: OffsetDateTime,
    open var campaign: Campaign
) : ExcelEntity