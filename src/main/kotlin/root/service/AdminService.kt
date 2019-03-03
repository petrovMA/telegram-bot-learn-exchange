package root.service

import root.data.entity.Admin
import root.data.entity.Campaign
import root.data.entity.Group
import root.data.entity.UserInGroup

interface AdminService {
    fun createOrUpdateGroupUser(user: UserInGroup) : UserInGroup
    fun saveAdmin(admin: Admin) : Admin?
    fun createGroup(group: Group) : Group
    fun createCampaign(campaign: Campaign) : Campaign
    fun updateCampaign(campaign: Campaign) : Campaign
    fun getCampaignByName(name: String) : Campaign?
    fun getCampaignById(id: Long) : Campaign?
    fun deleteCampaignByName(name: String)
    fun getAdminById(userId: Int) : Admin?
    fun deleteAdminById(userId: Int)
    fun deleteGroupById(id: Long)
    fun getUserById(userId: Int) : UserInGroup?
    fun getAllGroups() : Iterable<Group>
    fun getAllUsers() : Iterable<UserInGroup>
    fun getAllCampaigns() : Iterable<Campaign>
    fun getGroupsByCampaignId(campaignId: Long) : Iterable<Group>
    fun getUsersByCampaignId(campaignId: Long) : Iterable<UserInGroup>
    fun getAllCampaignByUserId(userId: Int): Iterable<Campaign>
    fun getAllCampaignsByChatListNotContainsUser(chats: List<Long>, userId: Int): Iterable<Campaign>
}
