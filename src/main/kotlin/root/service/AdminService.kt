package root.service

import org.telegram.telegrambots.meta.api.objects.User
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
    fun deleteCampaignByName(name: String)
    fun getAdminById(userId: Int) : Admin?
    fun deleteAdminById(userId: Int)
    fun deleteGroupById(id: Long)
    fun getAllGroups() : MutableIterable<Group>
    fun getAllUsers() : Iterable<UserInGroup>
    fun getGroupsByCampaignId(campaignId: Long) : Iterable<Group>
    fun getUsersByCampaignId(campaignId: Long) : Iterable<UserInGroup>
//    fun getAllUserIdInCampaigns(groups: Set<Campaign>) : MutableIterable<Int>
}
