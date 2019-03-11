package root.service

import root.data.entity.*

interface Service {
    fun createCampaign(campaign: Campaign) : Campaign
    fun updateCampaign(campaign: Campaign) : Campaign
    fun getCampaignByName(name: String) : Campaign?
    fun getCampaignById(id: Long) : Campaign?
    fun getAllCampaigns() : Iterable<Campaign>
    fun getAllCampaignByUserId(userId: Int): Iterable<Campaign>
    fun getAllCampaignsByChatListNotContainsUser(chats: List<Long>, userId: Int): Iterable<Campaign>
    fun deleteCampaignByName(name: String)

    fun getAllSuperAdmins() : Iterable<SuperAdmin>
    fun getSuperAdminById(userId: Int) : SuperAdmin?
    fun saveSuperAdmin(superAdmin: SuperAdmin) : SuperAdmin?
    fun deleteSuperAdminById(userId: Int)

    fun getSurveyByCampaign(campaign: Campaign) : Iterable<Survey>
    fun getSurveyByCampaignId(campaignId: Long) : Iterable<Survey>
    fun getSurveyById(id: Long) : Survey?
    fun getAllSurveyForUser(campaignId: Long, userId: Int) : Iterable<Survey>
    fun saveSurvey(survey: Survey) : Survey
    fun deleteSurveyById(id: Long)

    fun getAdminByCampaigns(campaigns: Set<Campaign>) : Iterable<Admin>
    fun getAdminById(userId: Int) : Admin?
    fun saveAdmin(admin: Admin) : Admin?
    fun deleteAdminById(userId: Int)

    fun createGroup(group: Group) : Group
    fun getAllGroups() : Iterable<Group>
    fun getGroupsByCampaignId(campaignId: Long) : Iterable<Group>
    fun deleteGroupById(id: Long)

    fun createOrUpdateGroupUser(user: UserInCampaign) : UserInCampaign
    fun getAllUsers() : Iterable<UserInCampaign>
    fun getUserById(userId: Int) : UserInCampaign?
    fun getUsersByCampaignId(campaignId: Long) : Iterable<UserInCampaign>

    fun getAllPassedSurveysByUser(user:UserInCampaign) : Iterable<PassedSurvey>
    fun savePassedSurvey(passedSurvey: PassedSurvey) : PassedSurvey
}