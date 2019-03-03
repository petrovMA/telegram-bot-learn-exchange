package root.data

import org.telegram.telegrambots.meta.api.objects.User

data class UserData(var state: UserState = UserState.NONE, val user: User, val taskName: String? = null)

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

    // user commands
    JOIN_TO_CAMPAIGN,
    USER_CAMPAIGN_MENU,
    USER_MENU,

    // common commands
    BACK,
    NONE,
    TASK
}