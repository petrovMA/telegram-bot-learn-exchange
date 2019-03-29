package root.data.entity.tasks

import root.data.entity.Campaign
import root.data.entity.ExcelEntity
import java.time.OffsetDateTime
import javax.persistence.FetchType
import javax.persistence.ManyToOne

abstract class TaskCampaign: Task() {
    abstract var campaign: Campaign
}