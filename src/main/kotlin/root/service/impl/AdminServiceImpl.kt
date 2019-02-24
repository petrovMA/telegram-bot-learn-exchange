package root.service.impl

import root.repositories.AdminRepository
import root.service.AdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import root.data.entity.Admin
import root.data.entity.Campaign
import root.data.entity.Group
import root.data.entity.UserInGroup
import root.repositories.CampaignRepository
import root.repositories.GroupRepository
import root.repositories.GroupUserRepository

@Service
open class AdminServiceImpl(
    @Autowired open val adminRepository: AdminRepository,
    @Autowired open val groupUserRepository: GroupUserRepository,
    @Autowired open val groupRepository: GroupRepository,
    @Autowired open val campaignRepository: CampaignRepository
) : AdminService {

    override fun createOrUpdateGroupUser(user: UserInGroup): UserInGroup =
        groupUserRepository.findGroupUserByUserId(user.userId).run {
            if (this == user) this
            else groupUserRepository.save(user)
        }

    override fun saveAdmin(admin: Admin): Admin? = adminRepository.save(admin)
    override fun getCampaignByName(name: String): Campaign? = campaignRepository.findCampaignByName(name)

    override fun createGroup(group: Group): Group = groupRepository.save(group)
    override fun createCampaign(campaign: Campaign): Campaign = campaignRepository.save(campaign)
    override fun updateCampaign(campaign: Campaign): Campaign = campaignRepository.save(campaign)

//    override fun getAllUserIdInCampaigns(groups: Set<Campaign>) = groupRepository.findAllUserIdInGroups(
//        groups.joinToString(
//            prefix = "any(ARRAY[",
//            postfix = "])"
//        ) { it.groupId.toString() })

    override fun getAdminById(userId: Int): Admin? = adminRepository.findAdminByUserId(userId)
    override fun getAllGroups(): MutableIterable<Group> = groupRepository.findAll()
}
