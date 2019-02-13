package bot

import org.telegram.telegrambots.meta.api.objects.User

data class UserData(var state: UserState, val user: User, val taskName: String? = null)

enum class UserState {
    BACK,
    TASK
}