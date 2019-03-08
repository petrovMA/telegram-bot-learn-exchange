package root.repositories

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import root.data.entity.UserInCampaign

interface GroupUserRepository : CrudRepository<UserInCampaign, Long> {
    fun findUserInGroupByUserId(userId: Int): UserInCampaign?

    @Query(value = "SELECT * from user_in_group where user_id in (SELECT user_in_group_id from campaign_to_user_in_group where campaign_id = ?1)", nativeQuery = true)
    fun findAllUsersByCampaignId(campaignId: Long): Iterable<UserInCampaign>
}
