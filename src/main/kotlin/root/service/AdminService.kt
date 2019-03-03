package root.service

import root.data.entity.Admin
import root.data.entity.Campaign
import root.data.entity.Group
import root.data.entity.UserInGroup

interface AdminService {
    fun createCampaign(campaign: Campaign) : Campaign
    fun updateCampaign(campaign: Campaign) : Campaign
    fun getCampaignByName(name: String) : Campaign?
    fun getCampaignById(id: Long) : Campaign?
    fun getAllCampaigns() : Iterable<Campaign>
    fun getAllCampaignByUserId(userId: Int): Iterable<Campaign>
    fun getAllCampaignsByChatListNotContainsUser(chats: List<Long>, userId: Int): Iterable<Campaign>
    fun deleteCampaignByName(name: String)

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
