package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.Admin
import root.data.entity.Campaign
import root.data.entity.Survey

interface AdminRepository : CrudRepository<Admin, Long> {
    fun findAllByCampaigns(campaigns: Set<Campaign>): Iterable<Admin>
    fun findAdminByUserId(id: Int): Admin?
    fun deleteByUserId(userId: Int)
}
