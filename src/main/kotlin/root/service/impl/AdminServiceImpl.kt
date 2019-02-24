package root.service.impl

import root.repositories.AdminRepository
import root.service.AdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import root.data.entity.Admin
import root.data.entity.Group
import root.data.entity.UserInGroup
import root.repositories.GroupRepository
import root.repositories.GroupUserRepository

@Service
open class AdminServiceImpl(
    @Autowired open val adminRepository: AdminRepository,
    @Autowired open val groupUserRepository: GroupUserRepository,
    @Autowired open val groupRepository: GroupRepository
) : AdminService {

    override fun createOrUpdateGroupUser(user: UserInGroup): UserInGroup =
        groupUserRepository.findGroupUserByUserId(user.userId).run {
            if (this == user) this
            else groupUserRepository.save(user)
        }

    override fun saveAdmin(admin: Admin, groupId: Long): Admin? {
//        admin.campaigns = listOf(groupRepository.save(Group(groupId, now())))
        return adminRepository.save(admin)
    }

//    override fun getAllUserIdInCampaigns(groups: Set<Campaign>) = groupRepository.findAllUserIdInGroups(
//        groups.joinToString(
//            prefix = "any(ARRAY[",
//            postfix = "])"
//        ) { it.groupId.toString() })

    override fun getAdminById(userId: Int): Admin? = adminRepository.findAdminByUserId(userId)
    override fun getAllGroups(): MutableIterable<Group> = groupRepository.findAll()
}
