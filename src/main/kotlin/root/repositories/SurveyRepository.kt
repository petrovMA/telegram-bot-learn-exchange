package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.Campaign
import root.data.entity.Survey

interface SurveyRepository : CrudRepository<Survey, Long> {
    fun findAllByCampaign(campaign: Campaign) : Iterable<Survey>
}
