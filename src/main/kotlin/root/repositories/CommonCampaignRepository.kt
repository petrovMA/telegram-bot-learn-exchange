package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.CommonCampaign

interface CommonCampaignRepository : CrudRepository<CommonCampaign, Long> {
    fun deleteByName(name: String)
}
