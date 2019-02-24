package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.UserInGroup

interface GroupUserRepository : CrudRepository<UserInGroup, Long> {
    fun findGroupUserByUserId(id: Int): UserInGroup?
}
