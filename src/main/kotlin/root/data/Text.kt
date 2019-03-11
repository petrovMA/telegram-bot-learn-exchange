package root.data

import kotlinx.serialization.*

@Serializable
data class Text(
     val msgAddAdmin: String,
     val msgAddGroup: String,
     val msgNoAdmin: String,
     val msgNoCampaign: String,
     val sendToEveryUser: String,
     val sendToEveryGroup: String,
     val msgSendToEveryUser: String,
     val msgSendToEveryGroup: String,
     val msgNotAdmin: String,
     val mainMenu: String,
     val addAdmin: String,
     val addGroup: String,
     val addAdminToCampaign: String,
     val msgAdminToCampaign: String,
     val errAdminToCampaign: String,
     val addGroupToCampaign: String,
     val errGroupToCampaign: String,
     val msgGroupToCampaign: String,
     val createCampaign: String,
     val errCreateCampaign: String,
     val msgCreateCampaign: String,
     val removeCampaign: String,
     val errRemoveCampaign: String,
     val msgRemoveCampaign: String,
     val timeOutTask: String,
     val showTasksList: String,
     val taskNotFound: String,
     val inviteText: String,
     val userMainMenu: String,
     val joinToCampaign: String,
     val showUserCampaigns: String,
     val userAvailableCampaigns: String,
     val msgUserAvailableCampaignsNotFound: String,
     val errClbUser: String,
     val clbUserAddedToCampaign: String,
     val errUserAddedToCampaign: String,
     val userAddedToCampaign: String,
     val removeAdminFromCampaign: String,
     val msgRemoveAdminFromCampaign: String,
     val errRemoveAdminFromCampaign: String,
     val removeGroupFromCampaign: String,
     val msgRemoveGroupFromCampaign: String,
     val errRemoveGroupFromCampaign: String,
     val sucCreateCampaign: String,
     val sucAdminToCampaign: String,
     val sucGroupToCampaign: String,
     val sucRemoveCampaign: String,
     val sucRemoveAdminFromCampaign: String,
     val sucRemoveGroupFromCampaign: String,
     val sucMsgToUsers: String,
     val errMsgToUsersNotFound: String,
     val errMsgToUsers: String,
     val sucMsgToCampaign: String,
     val errMsgToCampaignNotFound: String,
     val errMsgToCampaign: String,
     val adminAvailableCampaigns: String,
     val errClbSendMessageToEveryGroup: String,
     val errSendMessageToEveryGroup: String,
     val clbSendMessageToEveryGroup: String,
     val sucSendMessageToEveryGroup: String,
     val clbSendMessageToEveryUsers: String,
     val sucSendMessageToEveryUsers: String,
     val errClbSendMessageToEveryUsers: String,
     val errCommon: String,
     val addSuperAdmin: String,
     val msgAddSuperAdmin: String,
     val sucAddSuperAdmin: String,
     val errCampaignNotFound: String,
     val errAddSuperAdmin: String,
     val removeSuperAdmin: String,
     val sucRemoveSuperAdmin: String,
     val errRemoveSuperAdmin: String,
     val errAddSuperAdminAlreadyExist: String,
     val errClbCommon: String,
     val surveyOptions: String,
     val surveyOptionCreate: String,
     val surveyDelete: String,
     val surveyDeleted: String,
     val msgSurveyActionsName: String,
     val editQuestions: String,
     val surveyOptionBack: String,
     val surveyOptionSelectBack: String,
     val surveyQuestionBack: String,
     val surveyQuestionSelectBack: String,
     val editSurveyName: String,
     val editSurveyDescription: String,
     val errCallback: String,
     val clbSurveyOptions: String,
     val clbSurveyQuestions: String,
     val clbSurvey: String,
     val clbEditSurvey: String,
     val editSurvey: String,
     val clbSurveyQuestionEdit: String,
     val errNotFoundSurvey: String,
     val errSurveyDelete: String,
     val adminAvailableCampaignsSurveys: String,
     val surveyQuestionDelete: String,
     val surveyQuestionEditOptions: String,
     val surveyQuestionEditSort: String,
     val surveyQuestionEditText: String,
     val surveyOptionDelete: String,
     val surveyOptionEditValue: String,
     val surveyOptionEditSort: String,
     val surveyOptionEditText: String,
     val msgSurvey: String,
     val saveSurvey: String,
     val backSurvey: String,
     val enterTextBack: String,
     val backToSurveyCRUDMenu: String,
     val clbSurveySave: String,
     val backToSurveyMenu: String,
     val msgSurveyActionsDesc: String,
     val backToSurveyQuestionMenu: String,
     val errSurveyEnterNumber: String,
     val msgSurveyQuestionActionsText: String,
     val msgSurveyOptionActionsText: String,
     val msgSurveyQuestionActionsSort: String,
     val surveyQuestionCreate: String,
     val backToSurveyQuestionsListMenu: String,
     val backToSurveyOptionMenu: String,
     val msgSurveyOptionActionsSort: String,
     val msgSurveyOptionActionsValue: String,
     val surveyCreate: String,
     val clbSurveyOptionDeleted: String,
     val clbSurveyQuestionDeleted: String,
     val fileNameTextTmp: String,
     val msgDataInTableNotExist: String,
     val getTableFile: String,
     val sendCampaignsTable: String,
     val sendUsersInCampaign: String,
     val sendSuperAdminTable: String,
     val sendAdminsTable: String,
     val sendSurveysTable: String,
     val msgSurveysTable: String,
     val msgAdminsTable: String,
     val msgUsersInCampaign: String,
     val msgUserInfo: String,
     val sendUserInfo: String,
     val userCampaignsTask: String,
     val clbSurveyTimeOut: String,
     val sendChooseTask: String,
     val clbSurveyCollectProcess: String,
     val userCampaignsNotFound: String,
     val userTaskNotFound: String,
     val surveyPassed: String,
     val editSurveys: String,
     val surveyBack: String,
     val survey: String,
     val reset: String
)