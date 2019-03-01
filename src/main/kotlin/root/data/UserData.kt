package root.data

import org.telegram.telegrambots.meta.api.objects.User

data class UserData(var state: UserState = UserState.NONE, val user: User, val taskName: String? = null)

enum class UserState {
    MSG_TO_CAMPAIGN,
    MSG_TO_USERS,
    ADD_ADMIN,
    CREATE_CAMPAIGN,
    ADD_ADMIN_TO_CAMPAIGN,
    ADD_GROUP_TO_CAMPAIGN,
    REMOVE_CAMPAIGN,
    REMOVE_ADMIN_FROM_CAMPAIGN,
    REMOVE_GROUP_FROM_CAMPAIGN,
    BACK,
    NONE,
    TASK
}