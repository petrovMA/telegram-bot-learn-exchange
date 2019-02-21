package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.GroupUser

interface GroupUserRepository : CrudRepository<GroupUser, Long> {
    fun findGroupUserByUserId(id: Int): GroupUser?
}
