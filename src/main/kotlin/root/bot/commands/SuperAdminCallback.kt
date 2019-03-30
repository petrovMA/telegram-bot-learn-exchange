package root.bot.commands

import org.apache.log4j.Logger
import org.springframework.dao.DataIntegrityViolationException
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import root.bot.*
import root.data.MainAdmin
import root.data.Text
import root.data.UserData
import root.data.UserState
import root.data.UserState.*
import root.data.entity.*
import root.data.entity.tasks.surveus.Option
import root.data.entity.tasks.surveus.Question
import root.data.entity.tasks.surveus.Survey
import root.libs.*
import root.service.Service
import java.io.Serializable
import java.time.OffsetDateTime.now

class SuperAdminCallback

private val log = Logger.getLogger(SuperAdminCallback::class.java)

fun UserData.doSuperAdminCallback(
    upd: Update,
    text: Text,
    service: Service,
    send: (method: SendMessage) -> Any?,
    editMessage: (message: EditMessageText) -> Any?,
    sendTable: (chatId: Long, excelEntities: Iterable<ExcelEntity>, entityName: String) -> Any?,
    clbExecute: (callback: AnswerCallbackQuery) -> Unit,
    executeEdit: (message: EditMessageText) -> Unit,
    deleteMessage: (message: Message) -> Unit,
    errorAnswer: () -> Unit
): Unit {
    val params = upd.callbackQuery.data.split("\\s+".toRegex(), 2)
    val callbackAnswer = AnswerCallbackQuery().also { it.callbackQueryId = upd.callbackQuery.id }
    val callBackCommand: UserState

    try {
        callBackCommand = UserState.valueOf(params[0])
    } catch (e: Exception) {
        log.error("UserState = \"${upd.callbackQuery.data}\", not found", e)
        clbExecute(callbackAnswer.also { it.text = text.errClbCommon })
        throw e
    }

    when (callBackCommand) {
        SURVEY_CREATE -> {
            state = callBackCommand
            updCallback = upd
            editMessage(
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyActionsName,
                    text.backToSurveyCRUDMenu,
                    SURVEY_BACK
                )
            )
        }
        SURVEY_DELETE -> {
            service.deleteSurveyById(params[1].toLong())

            executeEdit(showSurveys(text, service.getSurveyByCampaign(campaign!!).toList(), upd))
        }
        SURVEY_SAVE -> {
            campaign?.let {
                val survey = survey!!
                survey.campaign = it
                service.saveSurvey(fixSurvey(survey))
            }

            executeEdit(
                editMessage(
                    upd.callbackQuery.message,
                    msgAvailableCampaignsListDivideCommon(
                        text.clbSurveySave,
                        CAMPAIGN_FOR_SURVEY.toString(),
                        service.getAllCampaigns().toList()
                    )
                )
            )
            state = CAMPAIGN_FOR_SURVEY
        }
        SURVEY_EDIT -> {
            survey = service.getSurveyById(params[1].toLong())
            editMessage(editSurvey(text, survey!!, upd))
            clbExecute(callbackAnswer.also { it.text = text.clbEditSurvey })
        }
        SURVEY -> {
            editMessage(editSurvey(text, survey!!, upd))
            clbExecute(callbackAnswer.also { it.text = text.clbEditSurvey })
        }
        SURVEY_NAME -> {
            apply {
                this.state = callBackCommand
                this.updCallback = upd
            }
            editMessage(
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyActionsName,
                    text.backToSurveyMenu,
                    SURVEY
                )
            )
        }
        SURVEY_DESCRIPTION -> {
            apply {
                this.state = callBackCommand
                this.updCallback = upd
            }
            editMessage(
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyActionsDesc,
                    text.backToSurveyMenu,
                    SURVEY
                )
            )
        }
        SURVEY_QUESTION_CREATE -> {
            apply {
                this.state = callBackCommand
                this.updCallback = upd
            }
            editMessage(
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyQuestionActionsText,
                    text.backToSurveyQuestionsListMenu,
                    SURVEY_QUESTIONS
                )
            )
        }
        SURVEY_QUESTION_EDIT_TEXT -> {
            apply {
                this.state = callBackCommand
                this.updCallback = upd
            }
            editMessage(
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyQuestionActionsText,
                    text.backToSurveyQuestionMenu,
                    SURVEY_QUESTION_SELECT
                )
            )
        }
        SURVEY_QUESTION_EDIT_SORT -> {
            apply {
                this.state = callBackCommand
                this.updCallback = upd
            }
            editMessage(
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyQuestionActionsSort,
                    text.backToSurveyQuestionMenu,
                    SURVEY_QUESTION_SELECT
                )
            )
        }
        SURVEY_OPTION_CREATE -> {
            apply {
                this.state = callBackCommand
                this.updCallback = upd
            }
            editMessage(
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyOptionActionsText,
                    text.backToSurveyQuestionMenu,
                    SURVEY_OPTION_SELECT_BACK
                )
            )
        }
        SURVEY_OPTION_EDIT_TEXT -> {
            apply {
                this.state = callBackCommand
                this.updCallback = upd
            }
            editMessage(
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyOptionActionsText,
                    text.backToSurveyOptionMenu,
                    SURVEY_OPTION_EDIT_BACK
                )
            )
        }
        SURVEY_OPTION_EDIT_CORRECT -> {
            apply {
                this.state = callBackCommand
                this.updCallback = upd
            }
            editMessage(
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyOptionActionsCorrect,
                    text.backToSurveyOptionMenu,
                    SURVEY_OPTION_EDIT_BACK
                )
            )
        }
        SURVEY_OPTION_EDIT_SORT -> {
            apply {
                this.state = callBackCommand
                this.updCallback = upd
            }
            editMessage(
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyOptionActionsSort,
                    text.backToSurveyOptionMenu,
                    SURVEY_OPTION_EDIT_BACK
                )
            )
        }
        SURVEY_BACK -> {
            deleteMessage(upd.callbackQuery.message)
            send(mainAdminsMenu(text).apply { chatId = chatId(upd).toString() })
        }
        SURVEY_OPTION_SELECT_BACK -> {
            editMessage(editQuestion(text, question!!, upd))
            clbExecute(callbackAnswer.also { it.text = text.clbSurvey })
        }
        SURVEY_QUESTIONS -> {
            question?.let {
                survey!!.questions = survey!!.questions.toHashSet().apply { add(it) }
                question = null
                executeEdit(showQuestions(text, survey!!, upd))
            } ?: {
                executeEdit(showQuestions(text, survey!!, upd))
                clbExecute(callbackAnswer.also { it.text = text.clbSurveyQuestions })
            }.invoke()
        }
        SURVEY_QUESTION_SELECT -> {
            question = survey!!.questions.first { it.text.hashCode() == params[1].toInt() }
            editMessage(editQuestion(text, question!!, upd))
            clbExecute(callbackAnswer.also { it.text = text.clbSurveyQuestionEdit })
        }
        SURVEY_QUESTION_DELETE -> {
            survey!!.questions = survey!!.questions.toHashSet().apply { remove(question) }
            question = null

            executeEdit(showQuestions(text, survey!!, upd))
            clbExecute(callbackAnswer.also { it.text = text.clbSurveyQuestionDeleted })
        }
        SURVEY_OPTION_DELETE -> {
            question!!.options = question!!.options.toHashSet().apply { remove(option) }
            option = null

            executeEdit(showOptions(text, question!!, upd))
            clbExecute(callbackAnswer.also { it.text = text.clbSurveyOptionDeleted })
        }
        SURVEY_OPTIONS -> {
            option?.let {
                question!!.options = question!!.options.toHashSet().apply { add(it) }
                option = null
                executeEdit(showOptions(text, question!!, upd))
            } ?: {
                executeEdit(showOptions(text, question!!, upd))
                clbExecute(callbackAnswer.also { it.text = text.clbSurveyOptions })
            }.invoke()
        }
        SURVEY_OPTION_SELECT -> {
            option = question!!.options.first { it.text.hashCode() == params[1].toInt() }
            editMessage(editOption(text, option!!, upd))
            clbExecute(callbackAnswer.also { it.text = text.clbSurveyOptions })
        }
        SURVEY_OPTION_EDIT_BACK -> {
            editMessage(editOption(text, option!!, upd))
            clbExecute(callbackAnswer.also { it.text = text.clbSurveyOptions })
        }

        GET_EXCEL_TABLE_SURVEY -> {
            deleteMessage(upd.callbackQuery.message)
            sendTable(
                chatId(upd),
                service.getSurveyByCampaignId(params[1].toLong()),
                text.tableNameSurvey
            )
        }
        GET_EXCEL_TABLE_USERS_IN_CAMPAIGN -> {
            deleteMessage(upd.callbackQuery.message)
            sendTable(
                chatId(upd),
                service.getUsersByCampaignId(params[1].toLong()),
                text.tableNameUsers
            )
        }
        GET_EXCEL_TABLE_ADMINS -> {
            deleteMessage(upd.callbackQuery.message)
            sendTable(
                chatId(upd),
                service.getAdminsByCampaigns(setOf(stubCampaign(id = params[1].toLong()))),
                text.tableNameAdmins
            )
        }

        CAMPAIGN_FOR_SEND_GROUP_MSG -> if (state == CAMPAIGN_FOR_SEND_GROUP_MSG) try {

            val campaign = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()

            groups = campaign.groups
            state = MSG_TO_CAMPAIGN
            user = upd.callbackQuery.from

            clbExecute(callbackAnswer.also { it.text = text.clbSendMessageToEveryGroup })
            editMessage(EditMessageText().apply {
                chatId = chatId(upd).toString()
                messageId = message(upd).messageId
                this.text = resourceText(text.sucSendMessageToEveryGroup, "campaign.name" to campaign.name)
            })
        } catch (t: Throwable) {
            log.error("CAMPAIGN_FOR_SEND_GROUP_MSG execute error", t)
            clbExecute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryGroup })
            throw t
        }
        else errorAnswer.invoke()
        CAMPAIGN_FOR_SEND_USERS_MSG -> if (state == CAMPAIGN_FOR_SEND_USERS_MSG) try {

            users = service.getUsersByCampaignId(params[1].toLong())
            state = MSG_TO_USERS
            user = upd.callbackQuery.from

            clbExecute(callbackAnswer.also { it.text = text.clbSendMessageToEveryUsers })

            editMessage(EditMessageText().apply {
                chatId = chatId(upd).toString()
                messageId = message(upd).messageId
                this.text = resourceText("${text.sucSendMessageToEveryUsers} id = ", "campaign.name" to params[1])
            })
        } catch (t: Throwable) {
            log.error("CAMPAIGN_FOR_SEND_USERS_MSG execute error", t)
            clbExecute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryUsers })
            throw t
        }
        else errorAnswer.invoke()
        CAMPAIGN_FOR_SURVEY -> if (state == CAMPAIGN_FOR_SURVEY) try {
            campaign = if (params[1].endsWith("common"))
                service.getCampaignById(params[1].split("\\s+".toRegex())[0].toLong())
                    ?: throw CampaignNotFoundException()
            else
                service.getCampaignById(params[1].toLong())
                    ?: throw CampaignNotFoundException()

            state = CAMPAIGN_FOR_SURVEY
            user = upd.callbackQuery.from

            editMessage(showSurveys(text, service.getSurveyByCampaign(campaign!!).toList(), upd))
        } catch (t: Throwable) {
            log.error("CAMPAIGN_FOR_SURVEY execute error", t)
            clbExecute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryUsers })
            throw t
        }
        else errorAnswer.invoke()
        MAIN_MENU_ADD_ADMIN, MAIN_MENU_ADD_GROUP, MAIN_MENU_DELETE_ADMIN, MAIN_MENU_DELETE_GROUP -> try {

            campaign = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()

            state = callBackCommand
            user = upd.callbackQuery.from

            clbExecute(callbackAnswer.also {
                it.text = when (callBackCommand) {
                    MAIN_MENU_ADD_ADMIN -> text.clbAddAdminToCampaign
                    MAIN_MENU_DELETE_ADMIN -> text.clbDeleteAdminFromCampaign
                    MAIN_MENU_ADD_GROUP -> text.clbAddGroupToCampaign
                    MAIN_MENU_DELETE_GROUP -> text.clbDeleteGroupFromCampaign
                    else -> throw CommandNotFoundException()
                }
            })
            deleteMessage(upd.callbackQuery.message)
            send(
                msgBackMenu(
                    when (callBackCommand) {
                        MAIN_MENU_ADD_ADMIN -> text.msgAdminToCampaignAdminId
                        MAIN_MENU_DELETE_ADMIN -> text.msgAdminDeleteFromCampaignAdminId
                        MAIN_MENU_ADD_GROUP -> text.msgGroupToCampaignGroupId
                        MAIN_MENU_DELETE_GROUP -> text.msgGroupDeleteFromCampaignGroupId
                        else -> throw CommandNotFoundException()
                    }, text.back
                ).apply { chatId = chatId(upd).toString() }
            )
        } catch (t: Throwable) {
            log.error("$callBackCommand execute error", t)
            clbExecute(callbackAnswer.also {
                it.text = when (callBackCommand) {
                    MAIN_MENU_ADD_ADMIN -> text.errClbAddAdminToCampaign
                    MAIN_MENU_DELETE_ADMIN -> text.errClbDeleteAdminFromCampaign
                    MAIN_MENU_ADD_GROUP -> text.errClbAddGroupFromCampaign
                    MAIN_MENU_DELETE_GROUP -> text.errClbDeleteGroupFromCampaign
                    else -> throw CommandNotFoundException()
                }
            })
            throw t
        }
        else -> errorAnswer.invoke()
    }
}