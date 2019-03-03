package root.data

import org.telegram.telegrambots.meta.api.objects.User
import root.data.entity.Group
import root.data.entity.UserInGroup

data class UserData(
    var state: UserState = UserState.NONE,
    val user: User,
    val taskName: String? = null,
    val groups: Set<Group>? = null,
    val users: Iterable<UserInGroup>? = null
)

enum class UserState {
    // super admin commands
    CREATE_CAMPAIGN,
    ADD_ADMIN_TO_CAMPAIGN,
    ADD_GROUP_TO_CAMPAIGN,
    REMOVE_CAMPAIGN,
    REMOVE_ADMIN_FROM_CAMPAIGN,
    REMOVE_GROUP_FROM_CAMPAIGN,

    // admin commands
    MSG_TO_CAMPAIGN,
    MSG_TO_USERS,
    CAMPAIGN_FOR_SEND_GROUP_MSG,
    CAMPAIGN_FOR_SEND_USERS_MSG,

    // user commands
    JOIN_TO_CAMPAIGN,
    USER_CAMPAIGN_MENU,
    USER_MENU,

    // common commands
    BACK,
    NONE,
    TASK
}