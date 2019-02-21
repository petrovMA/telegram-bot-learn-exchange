package root.service

import root.data.entity.Admin
import root.data.entity.Group
import root.data.entity.GroupUser

interface AdminService {
    fun createOrUpdateGroupUser(user: GroupUser) : GroupUser
    fun saveAdmin(admin: Admin, groupId: Long) : Admin?
    fun getAdminById(userId: Int) : Admin?
    fun getAllGroups() : MutableIterable<Group>
    fun getAllUserIdInGroups(groups: List<Group>) : Iterable<Int>
}
