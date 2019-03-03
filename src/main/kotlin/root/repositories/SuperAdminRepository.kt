package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.SuperAdmin

interface SuperAdminRepository : CrudRepository<SuperAdmin, Long> {
    fun findSuperAdminByUserId(id: Int): SuperAdmin?
}
