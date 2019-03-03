package root.repositories

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import root.data.entity.Campaign
import root.data.entity.UserInGroup

interface GroupUserRepository : CrudRepository<UserInGroup, Long> {
    fun findUserInGroupByUserId(userId: Int): UserInGroup?

    @Query(value = "SELECT * from user_in_group where user_id in (SELECT users_user_id from campaign_users where campaign_id = ?1", nativeQuery = true)
    fun findAllUsersByCampaignId(campaignId: Long): Iterable<UserInGroup>
}
