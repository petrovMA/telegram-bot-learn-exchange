package root.repositories

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import root.data.entity.Campaign

interface CampaignRepository : CrudRepository<Campaign, Long> {
    fun findCampaignById(id: Long): Campaign?
    fun findCampaignByName(name: String): Campaign?
    fun deleteByName(name: String)
    fun findAllByCommon(common: Boolean): Iterable<Campaign>

    @Query(value = "SELECT * from campaign where id in (SELECT campaign_id from campaign_to_user_in_group where user_in_group_id = ?1)", nativeQuery = true)
    fun findAllCampaignByUserId(userId: Int): Iterable<Campaign>

    @Query(value = "SELECT * from campaign where id in (SELECT campaign_id from campaign_to_exchange_group where exchange_group_id in( ?1 )) and id not in (SELECT campaign_id from campaign_to_user_in_group where user_in_group_id = ?2) and common = ?3", nativeQuery = true)
    fun findAllCampaignsByChatListNotContainsUser(chats: List<Long>, userId: Int, common: Boolean = false): Iterable<Campaign>
}
