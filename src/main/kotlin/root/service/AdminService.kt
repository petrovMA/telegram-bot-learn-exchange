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
    fun getAdminById(userId: Int) : Admin?
    fun getAllGroups() : MutableIterable<Group>
//    fun getAllUserIdInCampaigns(groups: Set<Campaign>) : MutableIterable<Int>
}
