package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.Admin

interface AdminRepository : CrudRepository<Admin, Long> {
    fun findAdminByUserId(id: Int): Admin?
}
