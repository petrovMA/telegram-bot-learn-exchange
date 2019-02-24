package root.service

import root.data.entity.Admin
import root.data.entity.Campaign
import root.data.entity.Group
import root.data.entity.UserInGroup

interface AdminService {
    fun createOrUpdateGroupUser(user: UserInGroup) : UserInGroup
    fun saveAdmin(admin: Admin, groupId: Long) : Admin?
    fun createCampaign(campaign: Campaign) : Campaign
    fun getAdminById(userId: Int) : Admin?
    fun getAllGroups() : MutableIterable<Group>
//    fun getAllUserIdInCampaigns(groups: Set<Campaign>) : MutableIterable<Int>
}
