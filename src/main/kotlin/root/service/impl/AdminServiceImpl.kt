package root.service.impl

import root.service.AdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import root.data.entity.*
import root.repositories.*

@Service
open class AdminServiceImpl(
    @Autowired open val adminRepository: AdminRepository,
    @Autowired open val optionRepository: OptionRepository,
    @Autowired open val questionRepository: QuestionRepository,
    @Autowired open val surveyRepository: SurveyRepository,
    @Autowired open val superAdminRepository: SuperAdminRepository,
    @Autowired open val groupUserRepository: GroupUserRepository,
    @Autowired open val groupRepository: GroupRepository,
    @Autowired open val campaignRepository: CampaignRepository
) : AdminService {
    @Transactional
    override fun getCampaignByName(name: String): Campaign? = campaignRepository.findCampaignByName(name)
    @Transactional
    override fun getCampaignById(id: Long): Campaign? = campaignRepository.findCampaignById(id)
    @Transactional
    override fun getAllCampaignByUserId(userId: Int): Iterable<Campaign> =
        campaignRepository.findAllCampaignByUserId(userId)
    @Transactional
    override fun createCampaign(campaign: Campaign): Campaign = campaignRepository.save(campaign)
    @Transactional
    override fun updateCampaign(campaign: Campaign): Campaign = campaignRepository.save(campaign)
    @Transactional
    override fun deleteCampaignByName(name: String) = campaignRepository.deleteByName(name)
    @Transactional
    override fun getAllCampaigns(): Iterable<Campaign> = campaignRepository.findAll()
    @Transactional
    override fun getAllCampaignsByChatListNotContainsUser(chats: List<Long>, userId: Int): Iterable<Campaign> =
        campaignRepository.findAllCampaignsByChatListNotContainsUser(chats, userId)


    @Transactional
    override fun getSuperAdminById(userId: Int) : SuperAdmin? = superAdminRepository.findSuperAdminByUserId(userId)
    @Transactional
    override fun saveSuperAdmin(superAdmin: SuperAdmin) : SuperAdmin = superAdminRepository.save(superAdmin)
    @Transactional
    override fun deleteSuperAdminById(userId: Int) = superAdminRepository.deleteByUserId(userId)


    @Transactional
    override fun getSurveyById(id: Long) : Survey? = surveyRepository.findById(id).orElse(null)
    @Transactional
    override fun saveSurvey(survey: Survey) : Survey = surveyRepository.save(survey)
    @Transactional
    override fun deleteSurveyById(id: Long) = surveyRepository.deleteById(id)


    @Transactional
    override fun createOrUpdateGroupUser(user: UserInGroup): UserInGroup =
        groupUserRepository.findUserInGroupByUserId(user.userId).run {
            if (this == user) this
            else groupUserRepository.save(user)
        }

    @Transactional
    override fun saveAdmin(admin: Admin): Admin? = adminRepository.save(admin)

    @Transactional
    override fun createGroup(group: Group): Group = groupRepository.save(group)

    @Transactional
    override fun getAdminById(userId: Int): Admin? = adminRepository.findAdminByUserId(userId)

    @Transactional
    override fun getUserById(userId: Int): UserInGroup? = groupUserRepository.findUserInGroupByUserId(userId)

    @Transactional
    override fun deleteAdminById(userId: Int) = adminRepository.deleteByUserId(userId)

    @Transactional
    override fun deleteGroupById(id: Long) = groupRepository.deleteById(id)

    @Transactional
    override fun getAllGroups(): Iterable<Group> = groupRepository.findAll()

    @Transactional
    override fun getAllUsers(): Iterable<UserInGroup> = groupUserRepository.findAll()

    @Transactional
    override fun getGroupsByCampaignId(campaignId: Long): Iterable<Group> =
        groupRepository.findAllByCampaignId(campaignId)

    @Transactional
    override fun getUsersByCampaignId(campaignId: Long): Iterable<UserInGroup> =
        groupUserRepository.findAllUsersByCampaignId(campaignId)
}
