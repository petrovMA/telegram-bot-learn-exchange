package root.data

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import root.data.dao.SurveyDAO
import root.data.entity.*
import root.data.entity.tasks.surveus.Option
import root.data.entity.tasks.surveus.Question
import root.data.entity.tasks.surveus.Survey

data class UserData(
    var state: UserState = UserState.NONE,
    var user: User,
    var taskName: String? = null,
    var groups: Set<Group>? = null,
    var campaign: Campaign? = null,
    var users: Iterable<UserInCampaign>? = null,
    var survey: Survey? = null,
    var question: Question? = null,
    var option: Option? = null,
    var updCallback: Update? = null,
    var surveyInProgress: SurveyDAO? = null
)

enum class UserState {
    // main admin commands
    MAIN_MENU_ADD_COMMON_CAMPAIGN,
    MAIN_MENU_DELETE_COMMON_CAMPAIGN,
    ADD_SUPER_ADMIN,
    REMOVE_SUPER_ADMIN,
    SEND_TABLE_FILE_MENU,

    // super admin commands
    CREATE_CAMPAIGN,
    ADD_GROUP_TO_CAMPAIGN,
    MAIN_MENU_ADD_CAMPAIGN,
    MAIN_MENU_ADD_GROUP,
    MAIN_MENU_ADD_SUPER_ADMIN,
    MAIN_MENU_DELETE_CAMPAIGN,
    MAIN_MENU_DELETE_GROUP,
    MAIN_MENU_DELETE_SUPER_ADMIN,
    REMOVE_CAMPAIGN,
    REMOVE_ADMIN_FROM_CAMPAIGN,
    REMOVE_GROUP_FROM_CAMPAIGN,

    // admin commands
    MAIN_MENU_ADD,
    MAIN_MENU_DELETE,
    MAIN_MENU_MESSAGE,
    MAIN_MENU_STATISTIC,
    MAIN_MENU_ADD_MISSION,
    MAIN_MENU_ADD_TASK,
    MAIN_MENU_ADD_ADMIN,
    MAIN_MENU_DELETE_MISSION,
    MAIN_MENU_DELETE_TASK,
    MAIN_MENU_DELETE_ADMIN,

    MSG_TO_CAMPAIGN,
    MSG_TO_USERS,
    CAMPAIGN_FOR_SEND_GROUP_MSG,
    CAMPAIGN_FOR_SEND_USERS_MSG,
    CAMPAIGN_FOR_SURVEY,
    CHOOSE_TASK,
    SURVEY_USERS_ANSWER,
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
    SURVEY_OPTION_EDIT_CORRECT,
    SURVEY_OPTION_EDIT_BACK,
    SURVEY_OPTION_DELETE,
    SURVEY_OPTION_SELECT,
    SURVEY_OPTION_SELECT_BACK,


    // user commands
    JOIN_TO_CAMPAIGN,
    JOIN_TO_CAMPAIGN_MENU,
    JOIN_TO_CAMPAIGN_BACK,
    USER_MENU_ACTIVE_CAMPAIGN,
    USER_MENU_ACTIVE_CAMPAIGN_SELECT,
    USER_MENU_ACTIVE_COMMON_CAMPAIGN_SELECT,
    USER_MENU_MY_ACCOUNT,
    USER_MENU_STATUS,
    USER_MENU,
    USER_ENTER_EMAIL,

    // common commands
    RESET,
    BACK,
    NONE,
    TASK
}