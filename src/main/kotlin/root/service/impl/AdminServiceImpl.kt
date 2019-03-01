package root.service.impl

import root.repositories.AdminRepository
import root.service.AdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

    @Transactional
    override fun createOrUpdateGroupUser(user: UserInGroup): UserInGroup =
        groupUserRepository.findGroupUserByUserId(user.userId).run {
            if (this == user) this
            else groupUserRepository.save(user)
        }

    @Transactional
    override fun saveAdmin(admin: Admin): Admin? = adminRepository.save(admin)

    @Transactional
    override fun getCampaignByName(name: String): Campaign? = campaignRepository.findCampaignByName(name)

    @Transactional
    override fun deleteCampaignByName(name: String) = campaignRepository.deleteByName(name)

    @Transactional
    override fun createGroup(group: Group): Group = groupRepository.save(group)

    @Transactional
    override fun createCampaign(campaign: Campaign): Campaign = campaignRepository.save(campaign)

    @Transactional
    override fun updateCampaign(campaign: Campaign): Campaign = campaignRepository.save(campaign)

//    override fun getAllUserIdInCampaigns(groups: Set<Campaign>) = groupRepository.findAllUserIdInGroups(
//        groups.joinToString(
//            prefix = "any(ARRAY[",
//            postfix = "])"
//        ) { it.groupId.toString() })

    @Transactional
    override fun getAdminById(userId: Int): Admin? = adminRepository.findAdminByUserId(userId)

    @Transactional
    override fun deleteAdminById(userId: Int) = adminRepository.deleteById(userId.toLong())

    @Transactional
    override fun deleteGroupById(id: Long) = groupRepository.deleteById(id)

    @Transactional
    override fun getAllGroups(): MutableIterable<Group> = groupRepository.findAll()

    @Transactional
    override fun getAllUsers(): Iterable<UserInGroup> = groupUserRepository.findAll()

    @Transactional
    override fun getGroupsByCampaignId(campaignId: Long) : Iterable<Group> =
        groupRepository.findAllByCampaignId(campaignId)

    @Transactional
    override fun getUsersByCampaignId(campaignId: Long) : Iterable<UserInGroup> =
        groupUserRepository.findAllUsersByCampaignId(campaignId)
}
