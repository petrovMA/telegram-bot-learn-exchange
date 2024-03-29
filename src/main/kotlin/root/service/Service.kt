package root.service

import org.telegram.telegrambots.meta.api.objects.User
import root.data.MainAdmin
import root.data.entity.*
import root.data.entity.tasks.PassedTask
import root.data.entity.tasks.RegisterOnExchange
import root.data.entity.tasks.Task
import root.data.entity.tasks.surveus.Survey

interface Service {
    fun createCampaign(campaign: Campaign): Campaign
    fun updateCampaign(campaign: Campaign): Campaign
    fun getCampaignByName(name: String): Campaign?
    fun getCampaignById(id: Long): Campaign?
    fun getAllCampaigns(): Iterable<Campaign>
    fun getAllCommonCampaigns(common: Boolean): Iterable<Campaign>
    fun getAllCampaignByUserId(userId: Int): Iterable<Campaign>
    fun getAllCampaignsByChatListNotContainsUser(
        chats: List<Long>,
        userId: Int,
        common: Boolean = false
    ): Iterable<Campaign>

    fun deleteCampaignByName(name: String)

    fun getAllSuperAdmins(): Iterable<SuperAdmin>
    fun getSuperAdminById(userId: Int): SuperAdmin?
    fun saveSuperAdmin(superAdmin: SuperAdmin): SuperAdmin?
    fun deleteSuperAdminById(userId: Int)

    fun getSurveyByCampaign(campaign: Campaign): Iterable<Survey>
    fun getAllTasksByUserFromCampaigns(userId: Int, common: Boolean = true): Iterable<Task>
    fun getSurveyByCampaignId(campaignId: Long): Iterable<Survey>
    fun getSurveyById(id: Long): Survey?
    fun getAllSurveyForUser(campaignId: Long, userId: Int): Iterable<Task>
    fun saveSurvey(survey: Survey): Survey
    fun deleteSurveyById(id: Long)

    fun getAdminsByCampaigns(campaigns: Set<Campaign>): Iterable<Admin>
    fun addAdmin(userId: Int, adminId: Int, camp: Campaign, maimAdmins: List<MainAdmin> = emptyList()): Pair<Admin, Campaign>
    fun deleteAdmin(userId: Int, adminId: Int, camp: Campaign, maimAdmins: List<MainAdmin> = emptyList()): Pair<Admin, Campaign>
    fun getAdminById(userId: Int): Admin?
    fun saveAdmin(admin: Admin): Admin?
    fun deleteAdminById(userId: Int)

    fun createGroup(group: Group): Group
    fun addGroup(userId: Int, groupId: Long, camp: Campaign, maimAdmins: List<MainAdmin> = emptyList()): Pair<Group, Campaign>
    fun getAllGroups(): Iterable<Group>
    fun getGroupsByCampaignId(campaignId: Long): Iterable<Group>
    fun deleteGroupById(id: Long)
    fun deleteGroup(userId: Int, groupId: Long, camp: Campaign, maimAdmins: List<MainAdmin> = emptyList()): Pair<Group, Campaign>

    fun createOrUpdateGroupUser(user: UserInCampaign): UserInCampaign
    fun getAllUsers(): Iterable<UserInCampaign>
    fun getUserById(userId: Int): UserInCampaign?
    fun getUsersByCampaignId(campaignId: Long): Iterable<UserInCampaign>

    fun getAllPassedSurveysByUser(user: UserInCampaign): Iterable<PassedTask>
    fun savePassedSurvey(passedTask: PassedTask): PassedTask
    fun savePassedTaskAndUpdateUser(passedTask: PassedTask): UserInCampaign

    fun getRegistered(user: UserInCampaign): RegisterOnExchange?
    fun saveRegistered(email: String, user: User): RegisterOnExchange
}
