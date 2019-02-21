package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.Group

interface GroupRepository : CrudRepository<Group, Long> {
    fun findGroupByGroupId(id: Long): Group?
}
