package root.repositories

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import root.data.entity.Group

interface GroupRepository : CrudRepository<Group, Long> {
    fun findGroupByGroupId(id: Long): Group?

    @Query(value = "SELECT * from exchange_group where group_id in (SELECT exchange_group_id from campaign_to_exchange_group where campaign_id = ?1)", nativeQuery = true)
    fun findAllByCampaignId(campaignId: Long): Iterable<Group>
}
