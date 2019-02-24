package root.repositories

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import root.data.entity.Group

interface GroupRepository : CrudRepository<Group, Long> {
    fun findGroupByGroupId(id: Long): Group?

//    @Query(value = "SELECT group_user_user_id FROM group_user_groups where groups_group_id = ?1 group by group_user_user_id", nativeQuery = true)
//    fun findAllUserIdInGroups(array: String): List<Int>
}
