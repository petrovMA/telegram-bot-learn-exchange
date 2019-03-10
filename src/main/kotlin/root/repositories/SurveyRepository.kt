package root.repositories

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import root.data.entity.Campaign
import root.data.entity.Survey

interface SurveyRepository : CrudRepository<Survey, Long> {
    fun findAllByCampaign_Id(campaignId: Long) : Iterable<Survey>
    fun findAllByCampaign(campaign: Campaign) : Iterable<Survey>

    @Query(value = "SELECT * from surveys where campaign_id = ?1 and id not in (SELECT survey_id from passed_surveys where user_user_id = ?2)", nativeQuery = true)
    fun findAllForUser(campaignId: Long, userId: Int) : Iterable<Survey>
}
