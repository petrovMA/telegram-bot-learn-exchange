package root.service

import root.data.entity.*

interface AdminService {
    fun createCampaign(campaign: Campaign) : Campaign
    fun updateCampaign(campaign: Campaign) : Campaign
    fun getCampaignByName(name: String) : Campaign?
    fun getCampaignById(id: Long) : Campaign?
    fun getAllCampaigns() : Iterable<Campaign>
    fun getAllCampaignByUserId(userId: Int): Iterable<Campaign>
    fun getAllCampaignsByChatListNotContainsUser(chats: List<Long>, userId: Int): Iterable<Campaign>
    fun deleteCampaignByName(name: String)

    fun getSuperAdminById(userId: Int) : SuperAdmin?
    fun saveSuperAdmin(superAdmin: SuperAdmin) : SuperAdmin?
    fun deleteSuperAdminById(userId: Int)

    fun getSurveyByCampaign(campaign: Campaign) : Iterable<Survey>
    fun getSurveyById(id: Long) : Survey?
    fun saveSurvey(survey: Survey) : Survey
    fun deleteSurveyById(id: Long)

    fun getAdminById(userId: Int) : Admin?
    fun saveAdmin(admin: Admin) : Admin?
    fun deleteAdminById(userId: Int)

    fun createGroup(group: Group) : Group
    fun getAllGroups() : Iterable<Group>
    fun getGroupsByCampaignId(campaignId: Long) : Iterable<Group>
    fun deleteGroupById(id: Long)

    fun createOrUpdateGroupUser(user: UserInGroup) : UserInGroup
    fun getAllUsers() : Iterable<UserInGroup>
    fun getUserById(userId: Int) : UserInGroup?
    fun getUsersByCampaignId(campaignId: Long) : Iterable<UserInGroup>
}
