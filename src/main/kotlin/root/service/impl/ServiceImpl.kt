package root.service.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import root.data.MainAdmin
import root.data.entity.*
import root.libs.AdminNotFoundException
import root.libs.NoAccessException
import root.libs.stubCampaign
import root.repositories.*
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now

@Service
open class ServiceImpl(
    @Autowired open val adminRepository: AdminRepository,
    @Autowired open val optionRepository: OptionRepository,
    @Autowired open val questionRepository: QuestionRepository,
    @Autowired open val surveyRepository: SurveyRepository,
    @Autowired open val superAdminRepository: SuperAdminRepository,
    @Autowired open val groupUserRepository: GroupUserRepository,
    @Autowired open val groupRepository: GroupRepository,
    @Autowired open val campaignRepository: CampaignRepository,
    @Autowired open val commonCampaignRepository: CommonCampaignRepository,
    @Autowired open val passedSurveyRepository: PassedSurveyRepository
) : root.service.Service {
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
    override fun createCommonCampaign(commonCampaign: CommonCampaign): CommonCampaign =
        commonCampaignRepository.save(commonCampaign)

    @Transactional
    override fun deleteCommonCampaignByName(name: String) = commonCampaignRepository.deleteByName(name)


    @Transactional
    override fun getSuperAdminById(userId: Int): SuperAdmin? = superAdminRepository.findSuperAdminByUserId(userId)

    @Transactional
    override fun getAllSuperAdmins(): Iterable<SuperAdmin> = superAdminRepository.findAll()

    @Transactional
    override fun saveSuperAdmin(superAdmin: SuperAdmin): SuperAdmin = superAdminRepository.save(superAdmin)

    @Transactional
    override fun deleteSuperAdminById(userId: Int) = superAdminRepository.deleteByUserId(userId)


    @Transactional
    override fun getSurveyByCampaign(campaign: Campaign): Iterable<Survey> =
        surveyRepository.findAllByCampaign(campaign)

    @Transactional
    override fun getSurveyByCampaignId(campaignId: Long): Iterable<Survey> =
        surveyRepository.findAllByCampaign_Id(campaignId)

    @Transactional
    override fun getSurveyById(id: Long): Survey? = surveyRepository.findById(id).orElse(null)

    @Transactional
    override fun getAllSurveyForUser(campaignId: Long, userId: Int): Iterable<Survey> =
        surveyRepository.findAllForUser(campaignId, userId)

    @Transactional
    override fun saveSurvey(survey: Survey): Survey = surveyRepository.save(survey)

    @Transactional
    override fun deleteSurveyById(id: Long) = surveyRepository.deleteById(id)


    @Transactional
    override fun createOrUpdateGroupUser(user: UserInCampaign): UserInCampaign =
        groupUserRepository.findUserInGroupByUserId(user.userId).run {
            if (this == user) this
            else groupUserRepository.save(user)
        }

    @Transactional
    override fun saveAdmin(admin: Admin): Admin = adminRepository.save(admin)

    @Transactional
    override fun createGroup(group: Group): Group = groupRepository.save(group)

    @Transactional
    override fun getAdminById(userId: Int): Admin? = adminRepository.findAdminByUserId(userId)

    @Transactional
    override fun getAdminsByCampaigns(campaigns: Set<Campaign>): Iterable<Admin> =
        adminRepository.findAllByCampaigns(campaigns)

    @Transactional
    override fun addAdmin(userId: Int, adminId: Int, camp: Campaign, maimAdmins: List<MainAdmin>) =
        if (hasAccessToEditAdmin(userId, adminId, camp, maimAdmins)) {
            getAdminById(adminId)?.let {
                saveAdmin(it.apply { campaigns = campaigns.toHashSet().apply { add(camp) } })
            } ?: saveAdmin(Admin(userId = adminId, createDate = now(), campaigns = hashSetOf(camp)))
        } else throw NoAccessException()

    @Transactional
    override fun deleteAdmin(userId: Int, adminId: Int, camp: Campaign, maimAdmins: List<MainAdmin>) =
        if (hasAccessToEditAdmin(userId, adminId, camp, maimAdmins)) {
            getAdminById(adminId)?.let {
                saveAdmin(it.apply { campaigns = campaigns.toHashSet().apply { remove(camp) } })
            } ?: throw AdminNotFoundException()
        } else throw NoAccessException()

    @Transactional
    override fun getUserById(userId: Int): UserInCampaign? = groupUserRepository.findUserInGroupByUserId(userId)

    @Transactional
    override fun deleteAdminById(userId: Int) = adminRepository.deleteByUserId(userId)

    @Transactional
    override fun deleteGroupById(id: Long) = groupRepository.deleteById(id)

    @Transactional
    override fun getAllGroups(): Iterable<Group> = groupRepository.findAll()

    @Transactional
    override fun getAllUsers(): Iterable<UserInCampaign> = groupUserRepository.findAll()

    @Transactional
    override fun getGroupsByCampaignId(campaignId: Long): Iterable<Group> =
        groupRepository.findAllByCampaignId(campaignId)

    @Transactional
    override fun getUsersByCampaignId(campaignId: Long): Iterable<UserInCampaign> =
        groupUserRepository.findAllUsersByCampaignId(campaignId)

    @Transactional
    override fun getAllPassedSurveysByUser(user: UserInCampaign) =
        passedSurveyRepository.findAllByUser(user)

    @Transactional
    override fun savePassedSurvey(passedSurvey: PassedSurvey): PassedSurvey = passedSurveyRepository.save(passedSurvey)

    private fun hasAccessToEditAdmin(userId: Int, adminId: Int, camp: Campaign, maimAdmins: List<MainAdmin>) =
        (maimAdmins.any { it.userId == userId } ||
                getAdminsByCampaigns(setOf(camp)).any { it.userId == adminId } ||
                getAllSuperAdmins().any { it.userId == userId })
}
