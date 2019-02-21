package root.data

import org.telegram.telegrambots.meta.api.objects.User

data class UserData(var state: UserState = UserState.NONE, val user: User, val taskName: String? = null)

enum class UserState {
    MSG_TO_GROUPS,
    MSG_TO_USERS,
    ADD_ADMIN,
    ADD_ADMIN_TO_GROUP,
    ADD_GROUP,
    BACK,
    NONE,
    TASK
}