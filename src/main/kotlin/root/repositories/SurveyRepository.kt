package root.repositories

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import root.data.entity.Campaign
import root.data.entity.tasks.surveus.Survey

interface SurveyRepository : CrudRepository<Survey, Long> {
    fun findAllByCampaign_Id(campaignId: Long) : Iterable<Survey>
    fun findAllByCampaign(campaign: Campaign) : Iterable<Survey>

    @Query(value = "SELECT * from surveys where campaign_id = ?1 and id not in (SELECT survey_id from passed_tasks where user_user_id = ?2)", nativeQuery = true)
    fun findAllForUser(campaignId: Long, userId: Int) : Iterable<Survey>

    @Query(value = "SELECT * from surveys where id not in (SELECT survey_id from passed_tasks where user_user_id = ?1) and campaign_id in (SELECT id from campaign where id = campaign_id and common = ?2)", nativeQuery = true)
    fun findAllSurveysByUserFromCampaigns(userId: Int, common: Boolean): Iterable<Survey>
}
