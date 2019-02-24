package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.Campaign

interface CampaignRepository : CrudRepository<Campaign, Long> {
    fun findCampaignById(id: Long): Campaign
}
