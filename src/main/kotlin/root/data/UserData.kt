package root.data

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import root.data.entity.*

data class UserData(
    var state: UserState = UserState.NONE,
    val user: User,
    val taskName: String? = null,
    val groups: Set<Group>? = null,
    val campaign: Campaign? = null,
    val users: Iterable<UserInCampaign>? = null,
    var survey: Survey? = null,
    var question: Question? = null,
    var option: Option? = null,
    var updCallback: Update? = null
)

enum class UserState {
    // main admin commands
    ADD_SUPER_ADMIN,
    REMOVE_SUPER_ADMIN,
    SEND_TABLE_FILE_MENU,

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
    CAMPAIGN_FOR_SURVEY,
    CHOOSE_TASK,
    USER_CAMPAIGN_FOR_TASK,
    GET_EXCEL_TABLE_SURVEY,
    GET_EXCEL_TABLE_ADMINS,
    GET_EXCEL_TABLE_USERS_IN_CAMPAIGN,
    SURVEY,
    SURVEY_DESCRIPTION,
    SURVEY_NAME,
    SURVEY_SAVE,
    SURVEY_BACK,
    SURVEY_EDIT,
    SURVEY_ACTIONS,
    SURVEY_CREATE,
    SURVEY_DELETE,
    SURVEY_QUESTION_CREATE,
    SURVEY_QUESTION_EDIT_TEXT,
    SURVEY_QUESTION_EDIT_SORT,
    SURVEY_QUESTION_SELECT,
    SURVEY_QUESTION_DELETE,
    SURVEY_QUESTIONS,
    SURVEY_OPTIONS,
    SURVEY_OPTION_CREATE,
    SURVEY_OPTION_EDIT_TEXT,
    SURVEY_OPTION_EDIT_SORT,
    SURVEY_OPTION_EDIT_VALUE,
    SURVEY_OPTION_EDIT_BACK,
    SURVEY_OPTION_DELETE,
    SURVEY_OPTION_SELECT,
    SURVEY_OPTION_SELECT_BACK,


    // user commands
    JOIN_TO_CAMPAIGN,
    USER_CAMPAIGN_MENU,
    USER_MENU,

    // common commands
    BACK,
    NONE,
    TASK
}