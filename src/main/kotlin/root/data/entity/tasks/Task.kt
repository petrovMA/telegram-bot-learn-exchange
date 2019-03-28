package root.data.entity.tasks

import root.data.entity.Campaign
import root.data.entity.ExcelEntity
import java.time.OffsetDateTime

interface Task: ExcelEntity {
    var id: Long?
    var createDate: OffsetDateTime
    var campaign: Campaign
}