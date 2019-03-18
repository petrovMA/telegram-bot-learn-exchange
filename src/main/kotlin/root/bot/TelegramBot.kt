package root.bot

import org.apache.log4j.Logger
import org.springframework.dao.DataIntegrityViolationException
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import root.data.MainAdmin
import root.data.Text
import root.data.UserData
import root.data.UserState
import root.service.Service
import java.time.OffsetDateTime.now
import java.util.ArrayList
import java.util.HashMap

import root.data.UserState.*
import root.data.dao.SurveyDAO
import root.data.entity.*
import root.libs.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat

class TelegramBot : TelegramLongPollingBot {

    private val userStates: HashMap<Int, UserData> = HashMap()

    companion object {
        private val log = Logger.getLogger(TelegramBot::class.java)!!
    }

    private val botUsername: String
    private val botToken: String
    private val tasks: Map<String, String>
    private val text: Text
    private val mainAdmins: List<MainAdmin>
    private val service: Service


    constructor(
        botUsername: String,
        botToken: String,
        tasks: Map<String, String>,
        text: Text,
        service: Service,
        mainAdmins: List<MainAdmin>,
        options: DefaultBotOptions?
    ) : super(options) {
        this.botUsername = botUsername
        this.botToken = botToken
        this.tasks = tasks
        this.text = text
        this.service = service
        this.mainAdmins = mainAdmins
    }

    constructor(
        botUsername: String,
        botToken: String,
        tasks: Map<String, String>,
        text: Text,
        service: Service,
        mainAdmins: List<MainAdmin>
    ) : super() {
        this.botUsername = botUsername
        this.botToken = botToken
        this.tasks = tasks
        this.text = text
        this.service = service
        this.mainAdmins = mainAdmins
    }

    override fun onUpdateReceived(update: Update) {
        log.info(
            "\nMessage: " + update.message?.text +
                    "\nFromMsg: " + update.message?.from +
                    "\nChat: " + update.message?.chat +
                    "\nCallbackQuery: " + update.callbackQuery?.data +
                    "\nFromCallBck: " + update.callbackQuery?.from +
                    "\nChatId: " + update.message?.chatId
        )
        if (update.hasCallbackQuery()) {
            val sender = update.callbackQuery.from
            try {
//            todo if (update.message.isGroupMessage || update.message.isChannelMessage || update.message.isSuperGroupMessage) {
                when {
                    mainAdmins.contains(MainAdmin(sender.id, sender.userName)) -> doMainAdminCallback(update)
                    else -> service.getSuperAdminById(sender.id)?.let { doSuperAdminCallback(update) }
                        ?: service.getAdminById(sender.id)?.let { doAdminCallback(update) } ?: doUserCallback(update)
                }

            } catch (t: Throwable) {
                t.printStackTrace()
                log.error("error when try response to callback: ${update.callbackQuery.data}", t)
                execute(AnswerCallbackQuery().also {
                    it.callbackQueryId = update.callbackQuery.id
                    it.text = text.errCallback
                })
                deleteMessage(update.callbackQuery.message)
                userStates.remove(sender.id)
            }
        } else if (update.message.isUserMessage) {
            val sender = update.message.from
//            todo if (update.message.isGroupMessage || update.message.isChannelMessage || update.message.isSuperGroupMessage) {
            when {
                mainAdmins.contains(MainAdmin(sender.id, sender.userName)) -> doMainAdminUpdate(update)
                else -> service.getSuperAdminById(sender.id)?.let { doSuperAdminUpdate(update, it) }
                    ?: service.getAdminById(sender.id)?.let {
                        try {
                            doAdminUpdate(update, it)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            log.error("error when try response to message: ${update.message.text}", t)
                            userStates.remove(sender.id)
                            sendMessage(mainAdminsMenu(text, text.errAdmins), update.message.chatId)
                        }
                    } ?: try {
                        doUserUpdate(update)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        log.error("error when try response to message: ${update.message.text}", t)
                        userStates.remove(sender.id)
                        sendMessage(mainUsersMenu(text, text.errUsers), update.message.chatId)
                    }
            }
        }
    }

    private fun doMainAdminUpdate(upd: Update) {
        val actionBack: () -> Unit = {
            when (userStates[upd.message.from.id]?.state) {
                MAIN_MENU_ADD_MISSION, MAIN_MENU_ADD_TASK, MAIN_MENU_ADD_ADMIN, MAIN_MENU_ADD_CAMPAIGN,
                MAIN_MENU_ADD_COMMON_CAMPAIGN, MAIN_MENU_ADD_GROUP, MAIN_MENU_ADD_SUPER_ADMIN,
                CAMPAIGN_FOR_SURVEY -> {
                    userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD, upd.message.from)
                    sendMessage(mainAdminAddMenu(text).apply {
                        replyMarkup = (replyMarkup as ReplyKeyboardMarkup).apply {
                            keyboard = createKeyboard(keyboard.flatten().toArrayList().apply {
                                addElements(
                                    0,
                                    KeyboardButton(this@TelegramBot.text.addMenuCampaign),
                                    KeyboardButton(this@TelegramBot.text.addMenuCommonCampaign),
                                    KeyboardButton(this@TelegramBot.text.addMenuGroup),
                                    KeyboardButton(this@TelegramBot.text.addMenuSuperAdmin)
                                )
                            })
                        }
                    }, upd.message.chatId)
                }
                MAIN_MENU_DELETE_MISSION, MAIN_MENU_DELETE_TASK, MAIN_MENU_DELETE_ADMIN, MAIN_MENU_DELETE_CAMPAIGN,
                MAIN_MENU_DELETE_COMMON_CAMPAIGN, MAIN_MENU_DELETE_GROUP, MAIN_MENU_DELETE_SUPER_ADMIN -> {
                    userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE, upd.message.from)
                    sendMessage(mainAdminDeleteMenu(text).apply {
                        replyMarkup = (replyMarkup as ReplyKeyboardMarkup).apply {
                            keyboard = createKeyboard(keyboard.flatten().toArrayList().apply {
                                addElements(
                                    0,
                                    KeyboardButton(this@TelegramBot.text.deleteMenuCampaign),
                                    KeyboardButton(this@TelegramBot.text.deleteMenuCommonCampaign),
                                    KeyboardButton(this@TelegramBot.text.deleteMenuGroup),
                                    KeyboardButton(this@TelegramBot.text.deleteMenuSuperAdmin)
                                )
                            })
                        }
                    }, upd.message.chatId)
                }
                else -> {
                    userStates.remove(upd.message.from.id)
                    sendMessage(mainAdminsMenu(text), upd.message.chatId)
                }
            }
        }

        when (userStates[upd.message.from.id]?.state) {
            MAIN_MENU_ADD -> {
                when (upd.message.text) {
                    text.addMenuCampaign -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD_CAMPAIGN, upd.message.from)
                        sendMessage(msgBackMenu(text.msgCreateCampaign, text.back), upd.message.chatId)
                    }
                    text.addMenuCommonCampaign -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD_COMMON_CAMPAIGN, upd.message.from)
                        sendMessage(msgBackMenu(text.msgCreateCommonCampaign, text.back), upd.message.chatId)
                    }
                    text.addMenuGroup -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD_GROUP, upd.message.from)
                        sendMessage(
                            msgAvailableCampaignsList(
                                text.msgGroupToCampaignSelectCamp,
                                MAIN_MENU_ADD_GROUP.toString(),
                                service.getAllCampaigns()
                            ), upd.message.chatId
                        )
                    }
                    text.addMenuSuperAdmin -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD_SUPER_ADMIN, upd.message.from)
                        sendMessage(msgBackMenu(text.msgAddSuperAdmin, text.back), upd.message.chatId)
                    }
                    text.addMenuMission -> {
                        // todo refactor it userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD_MISSION, upd.message.from)

                        sendMessage(msgBackMenu(text.msgSurvey, text.back), upd.message.chatId)

                        val availableCampaigns = service.getAllCampaigns().toList()

                        if (availableCampaigns.isNotEmpty()) {
                            sendMessage(
                                msgAvailableCampaignsList(
                                    text.adminAvailableCampaignsSurveys,
                                    CAMPAIGN_FOR_SURVEY.toString(),
                                    availableCampaigns
                                ), upd.message.chatId
                            )
                            userStates[upd.message.from.id] =
                                UserData(CAMPAIGN_FOR_SURVEY, upd.message.from)
                        } else {
                            sendMessage(mainAdminsMenu(text, text.msgNoCampaign), upd.message.chatId)
                            userStates.remove(upd.message.from.id)
                        }
                    }
                    text.addMenuTask -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD_TASK, upd.message.from)
                        TODO("MAIN_MENU_ADD_TASK")
                    }
                    text.addMenuAdmin -> {
                        sendMessage(
                            msgAvailableCampaignsList(
                                text.msgAdminToCampaignSelectCamp,
                                MAIN_MENU_ADD_ADMIN.toString(),
                                service.getAllCampaigns()
                            ), upd.message.chatId
                        )
                    }
                    text.back -> actionBack.invoke()
                }
            }
            MAIN_MENU_DELETE -> {
                when (upd.message.text) {
                    text.deleteMenuCampaign -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE_CAMPAIGN, upd.message.from)
                        sendMessage(msgBackMenu(text.msgRemoveCampaign, text.back), upd.message.chatId)
                    }
                    text.deleteMenuCommonCampaign -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE_COMMON_CAMPAIGN, upd.message.from)
                        sendMessage(msgBackMenu(text.msgRemoveCommonCampaign, text.back), upd.message.chatId)
                    }
                    text.deleteMenuGroup -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE_GROUP, upd.message.from)
                        sendMessage(
                            msgAvailableCampaignsList(
                                text.msgRemoveGroupFromCampaign,
                                MAIN_MENU_DELETE_GROUP.toString(),
                                service.getAllCampaigns()
                            ), upd.message.chatId
                        )
                    }
                    text.deleteMenuSuperAdmin -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE_SUPER_ADMIN, upd.message.from)
                        sendMessage(msgBackMenu(text.msgDeleteSuperAdmin, text.back), upd.message.chatId)
                    }
                    text.deleteMenuMission -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE_MISSION, upd.message.from)
                        TODO("MAIN_MENU_DELETE_MISSION")
                    }
                    text.deleteMenuTask -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE_TASK, upd.message.from)
                        TODO("MAIN_MENU_DELETE_TASK")
                    }
                    text.deleteMenuAdmin -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE_ADMIN, upd.message.from)
                        sendMessage(
                            msgAvailableCampaignsList(
                                text.msgRemoveAdminFromCampaign,
                                MAIN_MENU_DELETE_ADMIN.toString(),
                                service.getAllCampaigns()
                            ), upd.message.chatId
                        )
                    }
                    text.back -> actionBack.invoke()
                }
            }
            MAIN_MENU_ADD_SUPER_ADMIN -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val adminId = upd.message.text.toInt()

                        service.getSuperAdminById(adminId)?.run {
                            sendMessage(text.errAddSuperAdminAlreadyExist, upd.message.chatId)
                        } ?: {
                            service.saveSuperAdmin(SuperAdmin(userId = adminId, createDate = now()))
                            sendMessage(text.sucAddSuperAdmin, upd.message.chatId)
                        }.invoke()

                    } catch (t: Throwable) {
                        sendMessage(text.errAddSuperAdmin, upd.message.chatId)
                        log.error("SuperAdmin creating err.", t)
                    }
                }
            }
            MAIN_MENU_DELETE_SUPER_ADMIN -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        service.deleteSuperAdminById(upd.message.text.toInt())
                        sendMessage(text.sucRemoveSuperAdmin, upd.message.chatId)
                    } catch (t: Throwable) {
                        sendMessage(text.errRemoveSuperAdmin, upd.message.chatId)
                        log.error("SuperAdmin deleting err.", t)
                    }
                }
            }
            MAIN_MENU_ADD_COMMON_CAMPAIGN -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val newCampName = upd.message.text

                        service.createCampaign(
                            Campaign(
                                name = newCampName,
                                createDate = now(),
                                common = true,
                                groups = emptySet()
                            )
                        )

                        sendMessage(text.sucCreateCommonCampaign, upd.message.chatId)
                    } catch (e: DataIntegrityViolationException) {
                        sendMessage(text.errCreateCommonCampaignAlreadyExist, upd.message.chatId)
                        log.error("Campaign creating err.", e)
                    } catch (t: Throwable) {
                        sendMessage(text.errCreateCommonCampaign, upd.message.chatId)
                        log.error("Campaign creating err.", t)
                    }
                }
            }
            MAIN_MENU_DELETE_COMMON_CAMPAIGN -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val newCampName = upd.message.text

                        service.deleteCampaignByName(newCampName)

                        sendMessage(text.sucRemoveCommonCampaign, upd.message.chatId)
                    } catch (t: Throwable) {
                        sendMessage(text.errRemoveCommonCampaign, upd.message.chatId)
                        log.error("Campaign remove err.", t)
                    }
                }
            }
            MAIN_MENU_ADD_CAMPAIGN -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val newCampName = upd.message.text

                        service.createCampaign(Campaign(name = newCampName, createDate = now(), groups = emptySet()))

                        sendMessage(text.sucCreateCampaign, upd.message.chatId)
                    } catch (e: DataIntegrityViolationException) {
                        sendMessage(text.errCreateCampaignAlreadyExist, upd.message.chatId)
                        log.error("Campaign creating err.", e)
                    } catch (t: Throwable) {
                        sendMessage(text.errCreateCampaign, upd.message.chatId)
                        log.error("Campaign creating err.", t)
                    }
                }
            }
            MAIN_MENU_DELETE_CAMPAIGN -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val newCampName = upd.message.text

                        service.deleteCampaignByName(newCampName)

                        sendMessage(text.sucRemoveCampaign, upd.message.chatId)
                    } catch (t: Throwable) {
                        sendMessage(text.errRemoveCampaign, upd.message.chatId)
                        log.error("Campaign remove err.", t)
                    }
                }
            }
            MAIN_MENU_ADD_GROUP -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val params = upd.message.text.split("\\s+".toRegex())
                        val groupId = params[0].toLong()
                        val camp = userStates[upd.message.from.id]!!.campaign!!

                        val userId = upd.message.chatId

                        val (addedGroup, campaign) = service.addGroup(
                            userId = userId.toInt(),
                            groupId = groupId,
                            camp = camp,
                            maimAdmins = mainAdmins
                        )

                        sendMessage(
                            msgBackMenu(
                                resourceText(
                                    text.msgSuccessAddGroup,
                                    "group.id" to "${addedGroup.groupId}",
                                    "camp.desc" to "${campaign.id} ${campaign.name}"
                                ), text.back
                            ), userId
                        )

                    } catch (e: NoAccessException) {
                        sendMessage(text.errAddGroupAccessDenied, upd.message.chatId)
                        log.error("Group creating err (access denied).", e)
                    } catch (t: Throwable) {
                        sendMessage(text.errAddGroup, upd.message.chatId)
                        log.error("Group creating err.", t)
                    }
                }
            }
            MAIN_MENU_DELETE_GROUP -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val params = upd.message.text.split("\\s+".toRegex(), 2)
                        val groupId = params[0].toLong()
                        val camp = userStates[upd.message.from.id]!!.campaign!!

                        val userId = upd.message.chatId

                        val (deletedGroup, campaign) = service.deleteGroup(
                            userId = userId.toInt(),
                            groupId = groupId,
                            camp = camp,
                            maimAdmins = mainAdmins
                        )

                        sendMessage(
                            msgBackMenu(
                                resourceText(
                                    text.msgSuccessDeleteGroup,
                                    "group.id" to "${deletedGroup.groupId}",
                                    "camp.desc" to "${campaign.id} ${campaign.name}"
                                ), text.back
                            ), userId
                        )

                    } catch (e: AdminNotFoundException) {
                        sendMessage(text.errDeleteGroupNotFound, upd.message.chatId)
                        log.error("Group deleting err (not found).", e)
                    } catch (e: NoAccessException) {
                        sendMessage(text.errDeleteGroupAccessDenied, upd.message.chatId)
                        log.error("Group deleting err (access denied).", e)
                    } catch (t: Throwable) {
                        sendMessage(text.errDeleteGroup, upd.message.chatId)
                        log.error("Group deleting err.", t)
                    }
                }
            }
            MAIN_MENU_ADD_ADMIN -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val params = upd.message.text.split("\\s+".toRegex())
                        val adminId = params[0].toInt()
                        val camp = userStates[upd.message.from.id]!!.campaign!!

                        val userId = upd.message.chatId

                        val (addedAdmin, campaign) = service.addAdmin(
                            userId = userId.toInt(),
                            adminId = adminId,
                            camp = camp,
                            maimAdmins = mainAdmins
                        )

                        sendMessage(
                            msgBackMenu(
                                resourceText(
                                    text.msgSuccessAddAdmin,
                                    "admin.desc" to "${addedAdmin.userId} ${addedAdmin.userName}",
                                    "camp.desc" to "${campaign.id} ${campaign.name}"
                                ), text.back
                            ), userId
                        )

                    } catch (e: NoAccessException) {
                        sendMessage(text.errAddAdminAccessDenied, upd.message.chatId)
                        log.error("AdminGroup creating err (access denied).", e)
                    } catch (t: Throwable) {
                        sendMessage(text.errAddAdmin, upd.message.chatId)
                        log.error("AdminGroup creating err.", t)
                    }
                }
            }
            MAIN_MENU_DELETE_ADMIN -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val params = upd.message.text.split("\\s+".toRegex(), 2)
                        val adminId = params[0].toInt()
                        val camp = userStates[upd.message.from.id]!!.campaign!!

                        val userId = upd.message.chatId

                        val (deletedAdmin, campaign) = service.deleteAdmin(
                            userId = userId.toInt(),
                            adminId = adminId,
                            camp = camp,
                            maimAdmins = mainAdmins
                        )

                        sendMessage(
                            msgBackMenu(
                                resourceText(
                                    text.msgSuccessDeleteAdmin,
                                    "admin.desc" to "${deletedAdmin.userId} ${deletedAdmin.userName}",
                                    "camp.desc" to "${campaign.id} ${campaign.name}"
                                ), text.back
                            ), userId
                        )

                    } catch (e: AdminNotFoundException) {
                        sendMessage(text.errDeleteAdminNotFound, upd.message.chatId)
                        log.error("AdminGroup deleting err (not found).", e)
                    } catch (e: NoAccessException) {
                        sendMessage(text.errDeleteAdminAccessDenied, upd.message.chatId)
                        log.error("AdminGroup deleting err (access denied).", e)
                    } catch (t: Throwable) {
                        sendMessage(text.errDeleteAdmin, upd.message.chatId)
                        log.error("AdminGroup deleting err.", t)
                    }
                }
            }

            SURVEY_CREATE -> {
                val survey = Survey(
                    name = upd.message.text,
                    createDate = now(),
                    questions = HashSet(),
                    campaign = userStates[upd.message.from.id]!!.campaign!!
                )

                userStates[upd.message.from.id]!!.apply {
                    this.state = NONE
                    this.survey = survey
                }
                editSurvey(survey, userStates[upd.message.from.id]!!.updCallback!!)
            }
            SURVEY_NAME -> {
                val survey = userStates[upd.message.from.id]?.survey?.also { it.name = upd.message.text }
                    ?: Survey(
                        name = upd.message.text,
                        createDate = now(),
                        questions = HashSet(),
                        campaign = userStates[upd.callbackQuery.from.id]!!.campaign!!
                    )

                userStates[upd.message.from.id]!!.apply {
                    this.state = NONE
                    this.survey = survey
                }
                editSurvey(survey, userStates[upd.message.from.id]!!.updCallback!!)
            }
            SURVEY_DESCRIPTION -> {
                val survey = userStates[upd.message.from.id]?.survey!!.also { it.description = upd.message.text }

                userStates[upd.message.from.id]!!.apply {
                    this.state = NONE
                    this.survey = survey
                }
                editSurvey(survey, userStates[upd.message.from.id]!!.updCallback!!)
            }
            SURVEY_QUESTION_CREATE -> {
                val question = Question(text = upd.message.text, options = HashSet())

                userStates[upd.message.from.id]!!.apply {
                    state = NONE
                    this.question = question
                }
                editQuestion(question, userStates[upd.message.from.id]!!.updCallback!!)
            }
            SURVEY_QUESTION_EDIT_TEXT -> {
                val question = userStates[upd.message.from.id]?.question!!.also { it.text = upd.message.text }

                userStates[upd.message.from.id]!!.apply {
                    this.state = NONE
                    this.question = question
                }
                editQuestion(question, userStates[upd.message.from.id]!!.updCallback!!)
            }
            SURVEY_QUESTION_EDIT_SORT -> {
                try {
                    val question =
                        userStates[upd.message.from.id]?.question!!.also { it.sortPoints = upd.message.text.toInt() }

                    userStates[upd.message.from.id]!!.apply {
                        this.state = NONE
                        this.question = question
                    }
                    editQuestion(question, userStates[upd.message.from.id]!!.updCallback!!)
                } catch (t: Throwable) {
                    log.warn("error read sortPoints", t)

                    enterText(
                        userStates[upd.message.from.id]!!.updCallback!!.callbackQuery!!.message,
                        text.errSurveyEnterNumber,
                        text.backToSurveyQuestionMenu,
                        SURVEY_QUESTION_SELECT
                    )
                }
            }
            SURVEY_OPTION_CREATE -> {
                val option = Option(text = upd.message.text)

                userStates[upd.message.from.id]!!.apply {
                    this.state = NONE
                    this.option = option
                }
                editOption(option, userStates[upd.message.from.id]!!.updCallback!!)
            }
            SURVEY_OPTION_EDIT_TEXT -> {
                val option = userStates[upd.message.from.id]?.option!!.also { it.text = upd.message.text }

                userStates[upd.message.from.id]!!.apply {
                    this.state = NONE
                    this.option = option
                }
                editOption(option, userStates[upd.message.from.id]!!.updCallback!!)
            }
            SURVEY_OPTION_EDIT_CORRECT -> {
                try {
                    val option =
                        userStates[upd.message.from.id]?.option!!.also {
                            it.correct = upd.message.text.equals("true", ignoreCase = true)
                        }

                    userStates[upd.message.from.id]!!.apply {
                        this.state = NONE
                        this.option = option
                    }
                    editOption(option, userStates[upd.message.from.id]!!.updCallback!!)
                } catch (t: Throwable) {
                    log.warn("error read sortPoints", t)

                    enterText(
                        userStates[upd.message.from.id]!!.updCallback!!.callbackQuery!!.message,
                        text.errSurveyEnterNumber,
                        text.backToSurveyQuestionMenu,
                        SURVEY_QUESTION_SELECT
                    )
                }
            }
            SURVEY_OPTION_EDIT_SORT -> {
                try {
                    val option =
                        userStates[upd.message.from.id]?.option!!.also { it.sortPoints = upd.message.text.toInt() }

                    userStates[upd.message.from.id]!!.apply {
                        state = NONE
                        survey!!.questions = survey!!.questions.toHashSet().apply { add(question!!) }
                        this.option = option
                    }
                    editOption(option, userStates[upd.message.from.id]!!.updCallback!!)
                } catch (t: Throwable) {
                    log.warn("error read sortPoints", t)

                    enterText(
                        userStates[upd.message.from.id]!!.updCallback!!.callbackQuery!!.message,
                        text.errSurveyEnterNumber,
                        text.backToSurveyQuestionMenu,
                        SURVEY_QUESTION_SELECT
                    )
                }
            }
            else -> {
                when (upd.message.text) {
                    text.mainMenuAdd -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD, upd.message.from)
                        sendMessage(mainAdminAddMenu(text).apply {
                            replyMarkup = (replyMarkup as ReplyKeyboardMarkup).apply {
                                keyboard = createKeyboard(keyboard.flatten().toArrayList().apply {
                                    addElements(
                                        0,
                                        KeyboardButton(this@TelegramBot.text.addMenuCampaign),
                                        KeyboardButton(this@TelegramBot.text.addMenuCommonCampaign),
                                        KeyboardButton(this@TelegramBot.text.addMenuGroup),
                                        KeyboardButton(this@TelegramBot.text.addMenuSuperAdmin)
                                    )
                                })
                            }
                        }, upd.message.chatId)
                    }
                    text.mainMenuDelete -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE, upd.message.from)
                        sendMessage(mainAdminDeleteMenu(text).apply {
                            replyMarkup = (replyMarkup as ReplyKeyboardMarkup).apply {
                                keyboard = createKeyboard(keyboard.flatten().toArrayList().apply {
                                    addElements(
                                        0,
                                        KeyboardButton(this@TelegramBot.text.deleteMenuCampaign),
                                        KeyboardButton(this@TelegramBot.text.deleteMenuCommonCampaign),
                                        KeyboardButton(this@TelegramBot.text.deleteMenuGroup),
                                        KeyboardButton(this@TelegramBot.text.deleteMenuSuperAdmin)
                                    )
                                })
                            }
                        }, upd.message.chatId)
                    }
                    text.mainMenuMessages -> {
                        sendMessage(msgBackMenu(text.msgSendToEveryGroup, text.reset), upd.message.chatId)

                        service.getAllCampaigns().toList().run {
                            if (isNotEmpty()) {
                                sendMessage(
                                    msgAvailableCampaignsList(
                                        text.adminAvailableCampaigns,
                                        CAMPAIGN_FOR_SEND_GROUP_MSG.toString(),
                                        this
                                    ), upd.message.chatId
                                )
                                userStates[upd.message.from.id] =
                                    UserData(CAMPAIGN_FOR_SEND_GROUP_MSG, upd.message.from)
                            }
                        }
                    }
                    text.mainMenuStatistic -> {
                        userStates[upd.message.from.id] =
                            UserData(MAIN_MENU_STATISTIC, upd.message.from)
                        sendMessage(mainAdminStatisticMenu(text), upd.message.chatId)
                    }
                    text.back -> actionBack.invoke()
                    else -> {
                        when (userStates[upd.message.from.id]?.state) {
                            MAIN_MENU_STATISTIC -> sendMessage(mainAdminStatisticMenu(text), upd.message.chatId)
                            MAIN_MENU_DELETE -> sendMessage(mainAdminDeleteMenu(text), upd.message.chatId)
                            MAIN_MENU_ADD -> sendMessage(mainAdminAddMenu(text), upd.message.chatId)
                            CAMPAIGN_FOR_SEND_GROUP_MSG -> sendMessage(mainAdminsMenu(text), upd.message.chatId)
                            else -> {
                                sendMessage(mainAdminsMenu(text), upd.message.chatId)
                                log.warn("Not supported action!\n${upd.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun doSuperAdminUpdate(upd: Update, admin: SuperAdmin) {
        if (!admin.equals(upd.message.from)) {
            admin.update(upd.message.from)
            service.saveSuperAdmin(admin)
        }
        when (userStates[upd.message.from.id]?.state) {
            CREATE_CAMPAIGN -> {
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superAdminMenu(upd.message)
                    }
                    else -> try {
                        val name = upd.message.text

                        service.createCampaign(
                            Campaign(
                                name = name,
                                createDate = now(),
                                groups = HashSet()
                            )
                        )

                        end(upd, text.sucCreateCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                    } catch (t: Throwable) {
                        sendMessage(text.errCreateCampaign, upd.message.chatId)
                        log.error("Campaign creating err.", t)
                    }
                }
            }
            MAIN_MENU_ADD_ADMIN -> {
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superAdminMenu(upd.message)
                    }
                    else -> try {
                        val ids = upd.message.text.split("\\s+".toRegex(), 2)
                        val adminId = ids[0].toInt()
                        val name = ids[1]

                        service.getCampaignByName(name)?.let { camp ->
                            service.getAdminById(adminId)?.let { admin ->
                                admin.campaigns =
                                    admin.campaigns.toHashSet().also { gr -> gr.add(camp) }
                                service.saveAdmin(admin)
                            } ?: service.saveAdmin(
                                Admin(
                                    userId = adminId,
                                    createDate = now(),
                                    campaigns = hashSetOf(camp)
                                )
                            )

//                            end(upd, text.sucAdminToCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                        } ?: {
                            sendMessage(text.errCampaignNotFound, upd.message.chatId)
                        }.invoke()

                    } catch (t: Throwable) {
                        sendMessage(text.errAdminToCampaign, upd.message.chatId)
                        log.error("AdminGroup creating err.", t)
                    }
                }
            }
            ADD_GROUP_TO_CAMPAIGN -> {
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superAdminMenu(upd.message)
                    }
                    else -> try {
                        val ids = upd.message.text.split("\\s+".toRegex(), 2)
                        val groupId = ids[0].toLong()
                        val name = ids[1]

                        service.getCampaignByName(name)?.let {
                            val group = service.createGroup(Group(groupId, now()))
                            it.groups = it.groups.toHashSet().also { gr -> gr.add(group) }
                            service.updateCampaign(it)
                        } ?: {
                            sendMessage(text.errCampaignNotFound, upd.message.chatId)
                        }.invoke()

                        end(upd, text.sucGroupToCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                    } catch (t: Throwable) {
                        sendMessage(text.errGroupToCampaign, upd.message.chatId)
                        log.error("AdminGroup creating err.", t)
                    }
                }
            }
            REMOVE_CAMPAIGN -> {
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superAdminMenu(upd.message)
                    }
                    else -> try {
                        service.deleteCampaignByName(upd.message.text)
                        end(upd, text.sucRemoveCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                    } catch (t: Throwable) {
                        sendMessage(text.errRemoveCampaign, upd.message.chatId)
                        log.error("Campaign creating err.", t)
                    }
                }
            }
            REMOVE_ADMIN_FROM_CAMPAIGN -> {
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superAdminMenu(upd.message)
                    }
                    else -> try {
                        val params = upd.message.text.split("\\s+".toRegex(), 2)
                        val adminForDelete = service.getAdminById(params[0].toInt())
                        adminForDelete!!.campaigns =
                            adminForDelete.campaigns.filter { it.name != params[1] }.toHashSet()

                        service.saveAdmin(adminForDelete)
                        end(upd, text.sucRemoveAdminFromCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                    } catch (t: Throwable) {
                        sendMessage(text.errRemoveAdminFromCampaign, upd.message.chatId)
                        log.error("AdminGroup deleting err.", t)
                    }
                }
            }
            REMOVE_GROUP_FROM_CAMPAIGN -> {
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superAdminMenu(upd.message)
                    }
                    else -> try {
                        val params = upd.message.text.split("\\s+".toRegex(), 2)
                        val groupId = params[0].toLong()
                        val campaign = service.getCampaignByName(params[1])
                        campaign!!.groups = campaign.groups.filter { it.groupId != groupId }.toHashSet()

                        service.updateCampaign(campaign)
                        end(upd, text.sucRemoveGroupFromCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                    } catch (t: Throwable) {
                        sendMessage(text.errRemoveGroupFromCampaign, upd.message.chatId)
                        log.error("AdminGroup creating err.", t)
                    }
                }
            }
            MSG_TO_USERS -> {
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superAdminMenu(upd.message)
                    }
                    else -> try {
                        val users = userStates[upd.message.from.id]?.users
                        if (users?.firstOrNull() != null) {
                            msgToUsers(users, upd)
                            end(upd, text.sucMsgToUsers) { msg: Message, _: String ->
                                superAdminMenu(msg)
                            }
                        } else
                            end(upd, text.errMsgToUsersNotFound) { msg: Message, _: String ->
                                superAdminMenu(msg)
                            }
                    } catch (t: Throwable) {
                        sendMessage(text.errMsgToUsers, upd.message.chatId)
                        log.error("error msgToUsers", t)
                    }
                }
            }
            MSG_TO_CAMPAIGN -> {
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superAdminMenu(upd.message)
                    }
                    else -> try {
                        val groups = userStates[upd.message.from.id]?.groups
                        if (!groups.isNullOrEmpty()) {
                            msgToCampaign(groups.toList(), upd)
                            end(upd, text.sucMsgToCampaign) { msg: Message, _: String ->
                                superAdminMenu(msg)
                            }
                        } else {
                            end(upd, text.errMsgToCampaignNotFound) { msg: Message, _: String ->
                                superAdminMenu(msg)
                            }
                        }
                    } catch (t: Throwable) {
                        sendMessage(text.errMsgToCampaign, upd.message.chatId)
                        log.error("error msgToUsers", t)
                    }
                }
            }
            else -> {
                when (upd.message.text) {
                    text.removeCampaign -> {
                        sendMessage(msgBackMenu(text.msgRemoveCampaign, text.reset), upd.message.chatId)
                        userStates[upd.message.from.id] =
                            UserData(REMOVE_CAMPAIGN, upd.message.from)
                    }
                    text.removeAdminFromCampaign -> {
                        sendMessage(msgBackMenu(text.msgRemoveAdminFromCampaign, text.reset), upd.message.chatId)
                        userStates[upd.message.from.id] =
                            UserData(REMOVE_ADMIN_FROM_CAMPAIGN, upd.message.from)
                    }
                    text.removeGroupFromCampaign -> {
                        sendMessage(msgBackMenu(text.msgRemoveGroupFromCampaign, text.reset), upd.message.chatId)
                        userStates[upd.message.from.id] =
                            UserData(REMOVE_GROUP_FROM_CAMPAIGN, upd.message.from)
                    }
                    text.addAdminToCampaign -> {
                        sendMessage(msgBackMenu(text.msgAdminToCampaignAdminId, text.reset), upd.message.chatId)
                        userStates[upd.message.from.id] =
                            UserData(MAIN_MENU_ADD_ADMIN, upd.message.from)
                    }
                    text.addGroupToCampaign -> {
                        sendMessage(msgBackMenu(text.msgGroupToCampaign, text.reset), upd.message.chatId)
                        userStates[upd.message.from.id] =
                            UserData(ADD_GROUP_TO_CAMPAIGN, upd.message.from)
                    }
                    text.createCampaign -> {
                        sendMessage(msgBackMenu(text.msgCreateCampaign, text.reset), upd.message.chatId)
                        userStates[upd.message.from.id] =
                            UserData(CREATE_CAMPAIGN, upd.message.from)
                    }
                    text.sendToEveryUser -> {
                        sendMessage(msgBackMenu(text.msgSendToEveryUser, text.reset), upd.message.chatId)

                        val availableCampaigns = service.getAllCampaigns().toList()

                        if (availableCampaigns.isNotEmpty()) {
                            sendMessage(
                                msgAvailableCampaignsList(
                                    text.adminAvailableCampaigns,
                                    CAMPAIGN_FOR_SEND_USERS_MSG.toString(),
                                    availableCampaigns
                                ), upd.message.chatId
                            )
                            userStates[upd.message.from.id] =
                                UserData(CAMPAIGN_FOR_SEND_USERS_MSG, upd.message.from)
                        }
                    }
                    text.sendToEveryGroup -> {
                        sendMessage(msgBackMenu(text.msgSendToEveryGroup, text.reset), upd.message.chatId)

                        val availableCampaigns = service.getAllCampaigns().toList()

                        if (availableCampaigns.isNotEmpty()) {
                            sendMessage(
                                msgAvailableCampaignsList(
                                    text.adminAvailableCampaigns,
                                    CAMPAIGN_FOR_SEND_GROUP_MSG.toString(),
                                    availableCampaigns
                                ), upd.message.chatId
                            )
                            userStates[upd.message.from.id] =
                                UserData(CAMPAIGN_FOR_SEND_GROUP_MSG, upd.message.from)
                        }
                    }
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superAdminMenu(upd.message)
                    }
                    else -> {
                        superAdminMenu(upd.message)
                    }
                }
            }
        }
    }

    private fun doAdminUpdate(upd: Update, admin: Admin) {
        if (!admin.equals(upd.message.from)) {
            admin.update(upd.message.from)
            service.saveAdmin(admin)
        }
        val actionBack: () -> Unit = {
            when (userStates[upd.message.from.id]?.state) {
                MAIN_MENU_ADD_MISSION, MAIN_MENU_ADD_TASK, MAIN_MENU_ADD_ADMIN -> {
                    userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD, upd.message.from)
                    sendMessage(mainAdminAddMenu(text), upd.message.chatId)
                }
                MAIN_MENU_DELETE_MISSION, MAIN_MENU_DELETE_TASK, MAIN_MENU_DELETE_ADMIN -> {
                    userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE, upd.message.from)
                    sendMessage(mainAdminDeleteMenu(text), upd.message.chatId)
                }
                else -> {
                    userStates.remove(upd.message.from.id)
                    sendMessage(mainAdminsMenu(text, text.infoForAdmin), upd.message.chatId)
                }
            }
        }

        when (userStates[admin.userId]?.state) {
            MAIN_MENU_ADD -> {
                when (upd.message.text) {
                    text.addMenuMission -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD_MISSION, upd.message.from)
                        TODO("MAIN_MENU_ADD_MISSION")
                    }
                    text.addMenuTask -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD_TASK, upd.message.from)
                        TODO("MAIN_MENU_ADD_TASK")
                    }
                    text.addMenuAdmin -> {
                        sendMessage(
                            msgAvailableCampaignsList(
                                text.msgAdminToCampaignSelectCamp,
                                MAIN_MENU_ADD_ADMIN.toString(),
                                admin.campaigns
                            ), upd.message.chatId
                        )
                    }
                    text.back -> actionBack.invoke()
                }
            }
            MAIN_MENU_DELETE -> {
                when (upd.message.text) {
                    text.deleteMenuMission -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE_MISSION, upd.message.from)
                        TODO("MAIN_MENU_DELETE_MISSION")
                    }
                    text.deleteMenuTask -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE_TASK, upd.message.from)
                        TODO("MAIN_MENU_DELETE_TASK")
                    }
                    text.deleteMenuAdmin -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE_ADMIN, upd.message.from)
                        sendMessage(
                            msgAvailableCampaignsList(
                                text.msgRemoveAdminFromCampaign,
                                MAIN_MENU_DELETE_ADMIN.toString(),
                                service.getAllCampaigns()
                            ), upd.message.chatId
                        )
                    }
                    text.back -> actionBack.invoke()
                }
            }
            MAIN_MENU_ADD_ADMIN -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val params = upd.message.text.split("\\s+".toRegex())
                        val adminId = params[0].toInt()
                        val camp = userStates[upd.message.from.id]!!.campaign!!

                        val userId = upd.message.chatId

                        val (addedAdmin, campaign) = service.addAdmin(
                            userId = userId.toInt(),
                            adminId = adminId,
                            camp = camp,
                            maimAdmins = mainAdmins
                        )

                        sendMessage(
                            msgBackMenu(
                                resourceText(
                                    text.msgSuccessAddAdmin,
                                    "admin.desc" to "${addedAdmin.userId} ${addedAdmin.userName}",
                                    "camp.desc" to "${campaign.id} ${campaign.name}"
                                ), text.back
                            ), userId
                        )

                    } catch (e: NoAccessException) {
                        sendMessage(text.errAddAdminAccessDenied, upd.message.chatId)
                        log.error("AdminGroup creating err (access denied).", e)
                    } catch (t: Throwable) {
                        sendMessage(text.errAddAdmin, upd.message.chatId)
                        log.error("AdminGroup creating err.", t)
                    }
                }
            }
            MAIN_MENU_DELETE_ADMIN -> {
                when (upd.message.text) {
                    text.back -> actionBack.invoke()
                    else -> try {
                        val params = upd.message.text.split("\\s+".toRegex(), 2)
                        val adminId = params[0].toInt()
                        val camp = userStates[upd.message.from.id]!!.campaign!!

                        val userId = upd.message.chatId

                        val (deletedAdmin, campaign) = service.deleteAdmin(
                            userId = userId.toInt(),
                            adminId = adminId,
                            camp = camp,
                            maimAdmins = mainAdmins
                        )

                        sendMessage(
                            msgBackMenu(
                                resourceText(
                                    text.msgSuccessDeleteAdmin,
                                    "admin.desc" to "${deletedAdmin.userId} ${deletedAdmin.userName}",
                                    "camp.desc" to "${campaign.id} ${campaign.name}"
                                ), text.back
                            ), userId
                        )

                    } catch (e: AdminNotFoundException) {
                        sendMessage(text.errDeleteAdminNotFound, upd.message.chatId)
                        log.error("AdminGroup deleting err (not found).", e)
                    } catch (e: NoAccessException) {
                        sendMessage(text.errDeleteAdminAccessDenied, upd.message.chatId)
                        log.error("AdminGroup deleting err (access denied).", e)
                    } catch (t: Throwable) {
                        sendMessage(text.errDeleteAdmin, upd.message.chatId)
                        log.error("AdminGroup deleting err.", t)
                    }
                }
            }
            else -> {
                when (upd.message.text) {
                    text.mainMenuAdd -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_ADD, upd.message.from)
                        sendMessage(mainAdminAddMenu(text), upd.message.chatId)
                    }
                    text.mainMenuDelete -> {
                        userStates[upd.message.from.id] = UserData(MAIN_MENU_DELETE, upd.message.from)
                        sendMessage(mainAdminDeleteMenu(text), upd.message.chatId)
                    }
                    text.mainMenuMessages -> {
                        sendMessage(msgBackMenu(text.msgSendToEveryGroup, text.reset), upd.message.chatId)

                        if (admin.campaigns.isNotEmpty()) {
                            sendMessage(
                                msgAvailableCampaignsList(
                                    text.adminAvailableCampaigns,
                                    CAMPAIGN_FOR_SEND_GROUP_MSG.toString(),
                                    admin.campaigns
                                ), upd.message.chatId
                            )
                            userStates[upd.message.from.id] =
                                UserData(CAMPAIGN_FOR_SEND_GROUP_MSG, upd.message.from)
                        }
                    }
                    text.mainMenuStatistic -> {
                        userStates[upd.message.from.id] =
                            UserData(MAIN_MENU_STATISTIC, upd.message.from)
                        sendMessage(mainAdminStatisticMenu(text), upd.message.chatId)
                    }
                    text.back -> actionBack.invoke()
                    else -> {
                        when (userStates[upd.message.from.id]?.state) {
                            MAIN_MENU_STATISTIC -> sendMessage(mainAdminStatisticMenu(text), upd.message.chatId)
                            MAIN_MENU_DELETE -> sendMessage(mainAdminDeleteMenu(text), upd.message.chatId)
                            MAIN_MENU_ADD -> sendMessage(mainAdminAddMenu(text), upd.message.chatId)
                            CAMPAIGN_FOR_SEND_GROUP_MSG -> sendMessage(mainAdminsMenu(text), upd.message.chatId)
                            else -> {
                                sendMessage(mainAdminsMenu(text, text.infoForAdmin), upd.message.chatId)
                                log.warn("Not supported action!\n${upd.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun doUserUpdate(upd: Update) {
        when (userStates[upd.message.from.id]?.state) {
            JOIN_TO_CAMPAIGN -> {
                val userChats = getAllUserChats(service.getAllGroups().toList(), upd.message.from.id)
            }
            USER_MENU -> {

            }
            else -> when (upd.message.text) {
                text.joinToCampaign -> {
                    val userChats = getAllUserChats(service.getAllGroups().toList(), upd.message.from.id)

                    if (userChats.isNotEmpty()) {
                        val availableCampaigns = service.getAllCampaignsByChatListNotContainsUser(
                            userChats.map { it.groupId },
                            upd.message.from.id
                        ).toList()

                        if (availableCampaigns.isNotEmpty()) {
                            sendMessage(
                                msgAvailableCampaignsList(
                                    text.userAvailableCampaigns,
                                    USER_CAMPAIGN_MENU.toString(),
                                    availableCampaigns
                                ), upd.message.chatId
                            )
                            userStates[upd.message.from.id] = UserData(USER_CAMPAIGN_MENU, upd.message.from)
                        } else sendMessage(text.msgUserAvailableCampaignsNotFound, upd.message.chatId)
                    } else sendMessage(text.inviteText, upd.message.chatId)
                }
                text.msgUserInfo -> {
                    sendMessage(
                        msgUserInfo(
                            service.getAllPassedSurveysByUser(stubUserInCampaign(userId = upd.message.chatId.toInt())),
                            text.sendUserInfo
                        ), upd.message.chatId
                    )
                }
                text.showUserCampaigns -> {
                    val campaigns = service.getAllCampaignByUserId(upd.message.chatId.toInt()).toList()
                    if (campaigns.isNotEmpty()) {
                        sendMessage(
                            msgAvailableCampaignsList(
                                text.userCampaignsTask,
                                USER_CAMPAIGN_FOR_TASK.toString(),
                                campaigns

                            ), upd.message.chatId
                        )
                        userStates[upd.message.from.id] = UserData(USER_CAMPAIGN_FOR_TASK, upd.message.from)
                    } else sendMessage(mainUsersMenu(text, text.userCampaignsNotFound), upd.message.chatId)
                }
                else -> sendMessage(mainUsersMenu(text), upd.message.chatId)
            }
        }
    }

    private fun doMainAdminCallback(upd: Update) {
        val params = upd.callbackQuery.data.split("\\s+".toRegex(), 2)
        val callbackAnswer = AnswerCallbackQuery().also { it.callbackQueryId = upd.callbackQuery.id }
        val callBackCommand: UserState

        try {
            callBackCommand = UserState.valueOf(params[0])
        } catch (e: Exception) {
            log.error("UserState = \"${upd.callbackQuery.data}\", not found", e)
            execute(callbackAnswer.also { it.text = text.errClbCommon })
            throw e
        }

        val errorAnswer = {
            setTextToMessage(
                resourceText(text.errCommon),
                upd.callbackQuery.message.messageId,
                upd.callbackQuery.message.chatId
            )
            userStates.remove(upd.callbackQuery.from.id)
            execute(callbackAnswer.also { it.text = text.errClbCommon })
        }

        when (callBackCommand) {
            SURVEY_CREATE -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
                enterText(upd.callbackQuery.message, text.msgSurveyActionsName, text.backToSurveyCRUDMenu, SURVEY_BACK)
            }
            SURVEY_DELETE -> {
                service.deleteSurveyById(params[1].toLong())

                showSurveys(
                    service.getSurveyByCampaign(userStates[upd.callbackQuery.from.id]!!.campaign!!).toList(),
                    upd
                )
            }
            SURVEY_SAVE -> {
                userStates[upd.callbackQuery.from.id]!!.campaign?.let {
                    val survey = userStates[upd.callbackQuery.from.id]!!.survey!!
                    survey.campaign = it
                    service.saveSurvey(fixSurvey(survey))
                }

                sendMessage(mainAdminsMenu(text, text.clbSurveySave), upd.callbackQuery.message.chatId)
            }
            SURVEY_EDIT -> {
                userStates[upd.callbackQuery.from.id]!!.survey = service.getSurveyById(params[1].toLong())
                editSurvey(userStates[upd.callbackQuery.from.id]!!.survey!!, upd)
                execute(callbackAnswer.also { it.text = text.clbEditSurvey })
            }
            SURVEY -> {
                editSurvey(userStates[upd.callbackQuery.from.id]!!.survey!!, upd)
                execute(callbackAnswer.also { it.text = text.clbEditSurvey })
            }
            SURVEY_NAME -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
                enterText(upd.callbackQuery.message, text.msgSurveyActionsName, text.backToSurveyMenu, SURVEY)
            }
            SURVEY_DESCRIPTION -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
                enterText(upd.callbackQuery.message, text.msgSurveyActionsDesc, text.backToSurveyMenu, SURVEY)
            }
            SURVEY_QUESTION_CREATE -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyQuestionActionsText,
                    text.backToSurveyQuestionsListMenu,
                    SURVEY_QUESTIONS
                )
            }
            SURVEY_QUESTION_EDIT_TEXT -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyQuestionActionsText,
                    text.backToSurveyQuestionMenu,
                    SURVEY_QUESTION_SELECT
                )
            }
            SURVEY_QUESTION_EDIT_SORT -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyQuestionActionsSort,
                    text.backToSurveyQuestionMenu,
                    SURVEY_QUESTION_SELECT
                )
            }
            SURVEY_OPTION_CREATE -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyOptionActionsText,
                    text.backToSurveyQuestionMenu,
                    SURVEY_OPTION_SELECT_BACK
                )
            }
            SURVEY_OPTION_EDIT_TEXT -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyOptionActionsText,
                    text.backToSurveyOptionMenu,
                    SURVEY_OPTION_EDIT_BACK
                )
            }
            SURVEY_OPTION_EDIT_CORRECT -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyOptionActionsCorrect,
                    text.backToSurveyOptionMenu,
                    SURVEY_OPTION_EDIT_BACK
                )
            }
            SURVEY_OPTION_EDIT_SORT -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
                enterText(
                    upd.callbackQuery.message,
                    text.msgSurveyOptionActionsSort,
                    text.backToSurveyOptionMenu,
                    SURVEY_OPTION_EDIT_BACK
                )
            }
            SURVEY_BACK -> {
                deleteMessage(upd.callbackQuery.message)
                sendMessage(mainAdminsMenu(text), upd.callbackQuery.message.chatId)
            }
            SURVEY_OPTION_SELECT_BACK -> {
                editQuestion(userStates[upd.callbackQuery.from.id]!!.question!!, upd)
                execute(callbackAnswer.also { it.text = text.clbSurvey })
            }
            SURVEY_QUESTIONS -> {
                userStates[upd.callbackQuery.from.id]!!.question?.let {
                    userStates[upd.callbackQuery.from.id]!!.survey!!.questions =
                        userStates[upd.callbackQuery.from.id]!!.survey!!.questions.toHashSet().apply { add(it) }
                    userStates[upd.callbackQuery.from.id]!!.question = null
                    showQuestions(userStates[upd.callbackQuery.from.id]!!.survey!!, upd)
                } ?: {
                    showQuestions(userStates[upd.callbackQuery.from.id]!!.survey!!, upd)
                    execute(callbackAnswer.also { it.text = text.clbSurveyQuestions })
                }.invoke()
            }
            SURVEY_QUESTION_SELECT -> {
                userStates[upd.callbackQuery.from.id]!!.question =
                    userStates[upd.callbackQuery.from.id]!!.survey!!.questions.first { it.text.hashCode() == params[1].toInt() }
                editQuestion(userStates[upd.callbackQuery.from.id]!!.question!!, upd)
                execute(callbackAnswer.also { it.text = text.clbSurveyQuestionEdit })
            }
            SURVEY_QUESTION_DELETE -> {
                val survey = userStates[upd.callbackQuery.from.id]!!.survey!!
                survey.questions =
                    survey.questions.toHashSet().apply { remove(userStates[upd.callbackQuery.from.id]!!.question) }
                userStates[upd.callbackQuery.from.id]!!.question = null

                showQuestions(userStates[upd.callbackQuery.from.id]!!.survey!!, upd)
                execute(callbackAnswer.also { it.text = text.clbSurveyQuestionDeleted })
            }
            SURVEY_OPTION_DELETE -> {
                val question = userStates[upd.callbackQuery.from.id]!!.question!!
                question.options =
                    question.options.toHashSet().apply { remove(userStates[upd.callbackQuery.from.id]!!.option) }
                userStates[upd.callbackQuery.from.id]!!.option = null

                showOptions(userStates[upd.callbackQuery.from.id]!!.question!!, upd)
                execute(callbackAnswer.also { it.text = text.clbSurveyOptionDeleted })
            }
            SURVEY_OPTIONS -> {
                userStates[upd.callbackQuery.from.id]!!.option?.let {
                    userStates[upd.callbackQuery.from.id]!!.question!!.options =
                        userStates[upd.callbackQuery.from.id]!!.question!!.options.toHashSet().apply { add(it) }
                    userStates[upd.callbackQuery.from.id]!!.option = null
                    showOptions(userStates[upd.callbackQuery.from.id]!!.question!!, upd)
                } ?: {
                    showOptions(userStates[upd.callbackQuery.from.id]!!.question!!, upd)
                    execute(callbackAnswer.also { it.text = text.clbSurveyOptions })
                }.invoke()
            }
            SURVEY_OPTION_SELECT -> {
                userStates[upd.callbackQuery.from.id]!!.option =
                    userStates[upd.callbackQuery.from.id]!!.question!!.options.first { it.text.hashCode() == params[1].toInt() }
                editOption(userStates[upd.callbackQuery.from.id]!!.option!!, upd)
                execute(callbackAnswer.also { it.text = text.clbSurveyOptions })
            }
            SURVEY_OPTION_EDIT_BACK -> {
                editOption(userStates[upd.callbackQuery.from.id]!!.option!!, upd)
                execute(callbackAnswer.also { it.text = text.clbSurveyOptions })
            }

            GET_EXCEL_TABLE_SURVEY -> {
                deleteMessage(upd.callbackQuery.message)
                sendTable(
                    upd.callbackQuery.message.chatId,
                    service.getSurveyByCampaignId(params[1].toLong())
                )
            }
            GET_EXCEL_TABLE_USERS_IN_CAMPAIGN -> {
                deleteMessage(upd.callbackQuery.message)
                sendTable(
                    upd.callbackQuery.message.chatId,
                    service.getUsersByCampaignId(params[1].toLong())
                )
            }
            GET_EXCEL_TABLE_ADMINS -> {
                deleteMessage(upd.callbackQuery.message)
                sendTable(
                    upd.callbackQuery.message.chatId,
                    service.getAdminsByCampaigns(setOf(stubCampaign(id = params[1].toLong())))
                )
            }

            CAMPAIGN_FOR_SEND_GROUP_MSG -> if (userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SEND_GROUP_MSG) try {

                val campaign = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()
                val groups = campaign.groups

                userStates[upd.callbackQuery.from.id] =
                    UserData(MSG_TO_CAMPAIGN, upd.callbackQuery.from, groups = groups)
                execute(callbackAnswer.also { it.text = text.clbSendMessageToEveryGroup })
                setTextToMessage(
                    resourceText(text.sucSendMessageToEveryGroup, "campaign.name" to campaign.name),
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
            } catch (t: Throwable) {
                log.error("CAMPAIGN_FOR_SEND_GROUP_MSG execute error", t)
                execute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryGroup })
                throw t
            }
            else errorAnswer.invoke()
            CAMPAIGN_FOR_SEND_USERS_MSG -> if (userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SEND_USERS_MSG) try {

                val users = service.getUsersByCampaignId(params[1].toLong())

                userStates[upd.callbackQuery.from.id] =
                    UserData(MSG_TO_USERS, upd.callbackQuery.from, users = users)
                execute(callbackAnswer.also { it.text = text.clbSendMessageToEveryUsers })
                setTextToMessage(
                    resourceText("${text.sucSendMessageToEveryUsers} id = ", "campaign.name" to params[1]),
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
            } catch (t: Throwable) {
                log.error("CAMPAIGN_FOR_SEND_USERS_MSG execute error", t)
                execute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryUsers })
                throw t
            }
            else errorAnswer.invoke()
            CAMPAIGN_FOR_SURVEY -> if (userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SURVEY) try {
                val campaign = if (params[1].endsWith("common"))
                    service.getCampaignById(params[1].split("\\s+".toRegex())[0].toLong())
                        ?: throw CampaignNotFoundException()
                else
                    service.getCampaignById(params[1].toLong())
                        ?: throw CampaignNotFoundException()

                userStates[upd.callbackQuery.from.id] =
                    UserData(CAMPAIGN_FOR_SURVEY, upd.callbackQuery.from, campaign = campaign)

                val surveys = service.getSurveyByCampaign(campaign)
                showSurveys(surveys.toList(), upd)
            } catch (t: Throwable) {
                log.error("CAMPAIGN_FOR_SURVEY execute error", t)
                execute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryUsers })
                throw t
            }
            else errorAnswer.invoke()
            MAIN_MENU_ADD_ADMIN, MAIN_MENU_ADD_GROUP, MAIN_MENU_DELETE_ADMIN, MAIN_MENU_DELETE_GROUP -> try {

                val campaign = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()

                userStates[upd.callbackQuery.from.id] =
                    UserData(callBackCommand, upd.callbackQuery.from, campaign = campaign)
                execute(callbackAnswer.also {
                    it.text = when (callBackCommand) {
                        MAIN_MENU_ADD_ADMIN -> text.clbAddAdminToCampaign
                        MAIN_MENU_DELETE_ADMIN -> text.clbDeleteAdminFromCampaign
                        MAIN_MENU_ADD_GROUP -> text.clbAddGroupToCampaign
                        MAIN_MENU_DELETE_GROUP -> text.clbDeleteGroupFromCampaign
                        else -> throw CommandNotFoundException()
                    }
                })
                deleteMessage(upd.callbackQuery.message)
                sendMessage(
                    msgBackMenu(
                        when (callBackCommand) {
                            MAIN_MENU_ADD_ADMIN -> text.msgAdminToCampaignAdminId
                            MAIN_MENU_DELETE_ADMIN -> text.msgAdminDeleteFromCampaignAdminId
                            MAIN_MENU_ADD_GROUP -> text.msgGroupToCampaignGroupId
                            MAIN_MENU_DELETE_GROUP -> text.msgGroupDeleteFromCampaignGroupId
                            else -> throw CommandNotFoundException()
                        }, text.back
                    ), upd.callbackQuery.message.chatId
                )
            } catch (t: Throwable) {
                log.error("$callBackCommand execute error", t)
                execute(callbackAnswer.also {
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

    private fun doSuperAdminCallback(upd: Update) {
        val params = upd.callbackQuery.data.split("\\s+".toRegex(), 2)
        val callbackAnswer = AnswerCallbackQuery().also { it.callbackQueryId = upd.callbackQuery.id }
        val callBackCommand: UserState

        try {
            callBackCommand = UserState.valueOf(params[0])
        } catch (e: Exception) {
            log.error("UserState = \"${upd.callbackQuery.data}\", not found", e)
            execute(callbackAnswer.also { it.text = text.errClbCommon })
            throw e
        }

        when {
            CAMPAIGN_FOR_SEND_GROUP_MSG == callBackCommand &&
                    userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SEND_GROUP_MSG -> try {

                val campaign = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()
                val groups = campaign.groups

                userStates[upd.callbackQuery.from.id] =
                    UserData(MSG_TO_CAMPAIGN, upd.callbackQuery.from, groups = groups)
                execute(callbackAnswer.also { it.text = text.clbSendMessageToEveryGroup })
                setTextToMessage(
                    resourceText(text.sucSendMessageToEveryGroup, "campaign.name" to campaign.name),
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
            } catch (t: Throwable) {
                log.error("CAMPAIGN_FOR_SEND_GROUP_MSG execute error", t)
                execute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryGroup })
                throw t
            }
            CAMPAIGN_FOR_SEND_USERS_MSG == callBackCommand &&
                    userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SEND_USERS_MSG -> try {

                val users = service.getUsersByCampaignId(params[1].toLong())

                userStates[upd.callbackQuery.from.id] =
                    UserData(MSG_TO_USERS, upd.callbackQuery.from, users = users)
                execute(callbackAnswer.also { it.text = text.clbSendMessageToEveryUsers })
                setTextToMessage(
                    resourceText("${text.sucSendMessageToEveryUsers} id = ", "campaign.name" to params[1]),
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
            } catch (t: Throwable) {
                log.error("CAMPAIGN_FOR_SEND_USERS_MSG execute error", t)
                execute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryUsers })
                throw t
            }
            else -> {
                setTextToMessage(
                    resourceText(text.errCommon),
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
                userStates.remove(upd.callbackQuery.from.id)
                execute(callbackAnswer.also { it.text = text.errClbCommon })
            }
        }
    }

    private fun doAdminCallback(upd: Update) {
        val params = upd.callbackQuery.data.split("\\s+".toRegex(), 2)
        val callbackAnswer = AnswerCallbackQuery().also { it.callbackQueryId = upd.callbackQuery.id }
        val callBackCommand: UserState

        try {
            callBackCommand = UserState.valueOf(params[0])
        } catch (e: Exception) {
            log.error("UserState = \"${upd.callbackQuery.data}\", not found", e)
            execute(callbackAnswer.also { it.text = text.errClbCommon })
            throw e
        }

        when {
            CAMPAIGN_FOR_SEND_GROUP_MSG == callBackCommand &&
                    userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SEND_GROUP_MSG -> try {

                val campaign = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()
                val groups = campaign.groups

                userStates[upd.callbackQuery.from.id] =
                    UserData(MSG_TO_CAMPAIGN, upd.callbackQuery.from, groups = groups)
                execute(callbackAnswer.also { it.text = text.clbSendMessageToEveryGroup })
                setTextToMessage(
                    resourceText(text.sucSendMessageToEveryGroup, "campaign.name" to campaign.name),
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
            } catch (t: Throwable) {
                log.error("CAMPAIGN_FOR_SEND_GROUP_MSG execute error", t)
                execute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryGroup })
                throw t
            }
            CAMPAIGN_FOR_SEND_USERS_MSG == callBackCommand &&
                    userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SEND_USERS_MSG -> try {

                val users = service.getUsersByCampaignId(params[1].toLong())

                userStates[upd.callbackQuery.from.id] =
                    UserData(MSG_TO_USERS, upd.callbackQuery.from, users = users)
                execute(callbackAnswer.also { it.text = text.clbSendMessageToEveryUsers })
                setTextToMessage(
                    resourceText("${text.sucSendMessageToEveryUsers} id = ", "campaign.name" to params[1]),
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
            } catch (t: Throwable) {
                log.error("CAMPAIGN_FOR_SEND_USERS_MSG execute error", t)
                execute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryUsers })
                throw t
            }
            MAIN_MENU_ADD_ADMIN == callBackCommand || MAIN_MENU_DELETE_ADMIN == callBackCommand -> try {

                val campaign = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()

                userStates[upd.callbackQuery.from.id] =
                    UserData(callBackCommand, upd.callbackQuery.from, campaign = campaign)
                execute(callbackAnswer.also {
                    it.text = when (callBackCommand) {
                        MAIN_MENU_ADD_ADMIN -> text.clbAddAdminToCampaign
                        else -> text.clbDeleteAdminFromCampaign
                    }
                })
                deleteMessage(upd.callbackQuery.message)
                sendMessage(
                    msgBackMenu(
                        when (callBackCommand) {
                            MAIN_MENU_ADD_ADMIN -> text.msgAdminToCampaignAdminId
                            else -> text.msgAdminDeleteFromCampaignAdminId
                        }, text.back
                    ), upd.callbackQuery.message.chatId
                )
            } catch (t: Throwable) {
                log.error("$callBackCommand execute error", t)
                execute(callbackAnswer.also {
                    it.text = when (callBackCommand) {
                        MAIN_MENU_ADD_ADMIN -> text.errClbAddAdminToCampaign
                        else -> text.errClbDeleteAdminFromCampaign
                    }
                })
                throw t
            }
            else -> {
                setTextToMessage(
                    resourceText(text.errCommon),
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
                userStates.remove(upd.callbackQuery.from.id)
                execute(callbackAnswer.also { it.text = text.errClbCommon })
            }
        }
    }

    private fun doUserCallback(upd: Update) {
        val params = upd.callbackQuery.data.split("\\s+".toRegex(), 2)
        val callbackAnswer = AnswerCallbackQuery()
        val callBackCommand: UserState
        callbackAnswer.callbackQueryId = upd.callbackQuery.id

        try {
            callBackCommand = UserState.valueOf(params[0])
        } catch (e: Exception) {
            log.error("UserState = \"${upd.callbackQuery.data}\", not found", e)
            execute(callbackAnswer.also { it.text = text.errClbUser })
            throw e
        }

        val timeOutBack = {
            execute(callbackAnswer.also { it.text = text.clbSurveyTimeOut })
            deleteMessage(upd.callbackQuery.message)
            sendMessage(mainUsersMenu(text, text.errUsers), upd.message.chatId)
        }


        when (callBackCommand) {
            USER_CAMPAIGN_MENU -> {
                val campaignForAdd = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()
                execute(callbackAnswer.also { it.text = text.clbUserAddedToCampaign })
                service.getUserById(upd.callbackQuery.from.id)?.let {
                    service.createOrUpdateGroupUser(
                        UserInCampaign(
                            upd.callbackQuery.from.id,
                            createDate = now(),
                            firstName = upd.callbackQuery.from.firstName,
                            lastName = upd.callbackQuery.from.lastName,
                            userName = upd.callbackQuery.from.userName,
                            campaigns = it.campaigns.toHashSet().apply { add(campaignForAdd) }
                        )
                    )
                } ?: {
                    service.createOrUpdateGroupUser(
                        UserInCampaign(
                            upd.callbackQuery.from.id,
                            createDate = now(),
                            firstName = upd.callbackQuery.from.firstName,
                            lastName = upd.callbackQuery.from.lastName,
                            userName = upd.callbackQuery.from.userName,
                            campaigns = hashSetOf(campaignForAdd)
                        )
                    )
                }.invoke()
                setTextToMessage(
                    text.userAddedToCampaign,
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
            }
            USER_CAMPAIGN_FOR_TASK -> {
                if (userStates[upd.callbackQuery.from.id]?.state == USER_CAMPAIGN_FOR_TASK) {
                    execute(callbackAnswer.also { it.text = text.clbSurveyCollectProcess })
                    val tasks = service.getAllSurveyForUser(params[1].toLong(), upd.callbackQuery.from.id).toList()
                    if (tasks.isNotEmpty())
                        editMessage(
                            upd.callbackQuery.message,
                            msgTaskList(
                                text.sendChooseTask,
                                CHOOSE_TASK.toString(),
                                tasks
                            )
                        )
                    else sendMessage(mainUsersMenu(text, text.userTaskNotFound), upd.callbackQuery.message.chatId)
                } else timeOutBack.invoke()
            }
            CHOOSE_TASK -> {
                if (userStates[upd.callbackQuery.from.id]?.state == USER_CAMPAIGN_FOR_TASK) {
                    val survey = service.getSurveyById(params[1].toLong()) ?: throw SurveyNotFoundException()
                    userStates[upd.callbackQuery.from.id]!!.survey = survey
                    userStates[upd.callbackQuery.from.id]!!.surveyInProgress = SurveyDAO(
                        id = survey.id!!,
                        name = survey.name,
                        description = survey.description,
                        createDate = survey.createDate,
                        questions = survey.questions.toList().sortedBy { it.sortPoints },
                        state = 0
                    )
                    editMessage(
                        upd.callbackQuery.message,
                        msgQuestion(userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!, "$SURVEY_USERS_ANSWER")
                    )
                } else timeOutBack.invoke()
            }
            SURVEY_USERS_ANSWER -> {
                if (userStates[upd.callbackQuery.from.id]?.state == USER_CAMPAIGN_FOR_TASK) {
                    val prevState = userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.state
                    val question = userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.questions[prevState]
                    userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.currentValue += question.options.find {
                        it.id == params[1].toLong()
                    }!!.value
                    userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.state++
                    if (userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.state < userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.questions.size)
                        editMessage(
                            upd.callbackQuery.message,
                            msgQuestion(
                                userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!,
                                "$SURVEY_USERS_ANSWER"
                            )
                        )
                    else {
                        service.savePassedSurvey(
                            PassedSurvey(
                                value = userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.currentValue,
                                description = userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.description,
                                passDate = now(),
                                survey = userStates[upd.callbackQuery.from.id]!!.survey!!,
                                user = stubUserInCampaign(userId = upd.callbackQuery.from.id)
                            )
                        )
                        sendMessage(mainUsersMenu(text, text.surveyPassed), upd.callbackQuery.message.chatId)
                    }
                } else timeOutBack.invoke()
            }
            else -> {
                setTextToMessage(
                    text.errUserAddedToCampaign,
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
                userStates.remove(upd.callbackQuery.from.id)
                execute(callbackAnswer.also { it.text = text.clbUserAddedToCampaign })
            }
        }
    }

    private fun superAdminMenu(message: Message) = sendMessage(SendMessage().also { msg ->
        msg.text = text.mainMenu
        msg.enableMarkdown(true)
        msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
            markup.selective = true
            markup.resizeKeyboard = true
            markup.oneTimeKeyboard = false
            markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
                keyboard.addElements(KeyboardRow().also {
                    it.add(text.sendToEveryUser)
                    it.add(text.sendToEveryGroup)
                }, KeyboardRow().also {
                    it.add(text.addGroupToCampaign)
                    it.add(text.addAdminToCampaign)
                    it.add(text.createCampaign)
                }, KeyboardRow().also {
                    it.add(text.removeGroupFromCampaign)
                    it.add(text.removeAdminFromCampaign)
                    it.add(text.removeCampaign)
                })
            }
        }
    }, message.chatId)

    private fun sendTableSuperAdmin(message: Message) = sendMessage(SendMessage().also { msg ->
        msg.text = text.mainMenu
        msg.enableMarkdown(true)
        msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
            markup.selective = true
            markup.resizeKeyboard = true
            markup.oneTimeKeyboard = false
            markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
                keyboard.addElements(KeyboardRow().also {
                    it.add(text.sendCampaignsTable)
                    it.add(text.sendSuperAdminTable)
                }, KeyboardRow().also {
                    it.add(text.sendUsersInCampaign)
                    it.add(text.sendAdminsTable)
                    it.add(text.sendSurveysTable)
                }, KeyboardRow().also {
                    it.add(text.reset)
                })
            }
        }
    }, message.chatId)

    private fun sendMessage(messageText: String, chatId: Long) = try {
        val message = SendMessage().setChatId(chatId)
        log.debug("Send to chatId = $chatId\nMessage: \"$messageText\"")
        message.text = messageText
        execute(message)
    } catch (e: Exception) {
        log.warn(e.message, e)
    }

    private fun sendMessage(message: SendMessage, chatId: Long) = try {
        log.debug("Send to chatId = $chatId\nMessage: \"${message.text}\"")
        message.setChatId(chatId)
        execute<Message, SendMessage>(message)
    } catch (e: Exception) {
        log.warn(e.message, e)
    }

    private fun getAllUserChats(chats: List<Group>, userId: Int) = chats.filter {
        try {
            listOf("creator", "administrator", "member", "restricted").contains(getUser(it.groupId, userId).status)
        } catch (e: TelegramApiRequestException) {
            log.info("User: $userId not found in chat ${it.groupId}")
            false
        }
    }

    private fun msgToCampaign(groups: Iterable<Group>, upd: Update) = groups.forEach {
        execute(
            ForwardMessage(
                it.groupId,
                upd.message.chatId,
                upd.message.messageId
            )
        )
    }

    private fun msgToUsers(users: Iterable<UserInCampaign>, upd: Update) = users.forEach {
        execute(
            ForwardMessage(
                it.userId.toLong(),
                upd.message.chatId,
                upd.message.messageId
            )
        )
    }

    private fun showSurveys(surveys: List<Survey>, upd: Update) = editMessage(EditMessageText().also { msg ->
        msg.chatId = upd.callbackQuery.message.chatId.toString()
        msg.messageId = upd.callbackQuery.message.messageId
        msg.text = text.editSurveys
        msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
            markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
                surveys.sortedBy { it.name }.forEach {
                    keyboard.add(
                        listOf(
                            InlineKeyboardButton().setText(it.name.subStr(25, "..."))
                                .setCallbackData("$SURVEY_EDIT ${it.id}")
                        )
                    )
                }
                keyboard.addElements(
                    listOf(
                        InlineKeyboardButton().setText(text.surveyCreate)
                            .setCallbackData("$SURVEY_CREATE")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyBack)
                            .setCallbackData("$SURVEY_BACK")
                    )
                )
            }
        }
    })

    private fun editSurvey(survey: Survey, upd: Update) = editMessage(EditMessageText().also { msg ->
        msg.chatId = upd.callbackQuery.message.chatId.toString()
        msg.messageId = upd.callbackQuery.message.messageId
        msg.text = "$survey\n${printQuestions(survey.questions)}"
        msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
            markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
                keyboard.addElements(
                    listOf(
                        InlineKeyboardButton().setText(text.editQuestions)
                            .setCallbackData("$SURVEY_QUESTIONS")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.editSurveyName)
                            .setCallbackData("$SURVEY_NAME")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.editSurveyDescription)
                            .setCallbackData("$SURVEY_DESCRIPTION")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.saveSurvey)
                            .setCallbackData("$SURVEY_SAVE")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyDelete)
                            .setCallbackData("$SURVEY_DELETE ${survey.id}")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.backSurvey)
                            .setCallbackData("$SURVEY_BACK")
                    )
                )
            }
        }
    })

    private fun showQuestions(survey: Survey, upd: Update) = editMessage(EditMessageText().also { msg ->
        msg.chatId = upd.callbackQuery.message.chatId.toString()
        msg.messageId = upd.callbackQuery.message.messageId
        msg.text = "${survey.name}\n${survey.description}\n${printQuestions(survey.questions)}"
        msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
            markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
                survey.questions.toList().sortedBy { it.sortPoints }.forEach {
                    keyboard.add(
                        listOf(
                            InlineKeyboardButton().setText(it.text.subStr(25, "..."))
                                .setCallbackData("$SURVEY_QUESTION_SELECT ${it.text.hashCode()}")
                        )
                    )
                }
                keyboard.addElements(
                    listOf(
                        InlineKeyboardButton().setText(text.surveyQuestionCreate)
                            .setCallbackData("$SURVEY_QUESTION_CREATE")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyQuestionSelectBack)
                            .setCallbackData("$SURVEY")
                    )
                )
            }
        }
    })

    private fun editQuestion(question: Question, upd: Update) = editMessage(EditMessageText().also { msg ->
        msg.chatId = upd.callbackQuery.message.chatId.toString()
        msg.messageId = upd.callbackQuery.message.messageId
        msg.text = "$question\n${printOptions(question.options)}"
        msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
            markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
                keyboard.addElements(
                    listOf(
                        InlineKeyboardButton().setText(text.surveyQuestionEditText)
                            .setCallbackData("$SURVEY_QUESTION_EDIT_TEXT")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyQuestionEditSort)
                            .setCallbackData("$SURVEY_QUESTION_EDIT_SORT")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyQuestionEditOptions)
                            .setCallbackData("$SURVEY_OPTIONS")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyQuestionDelete)
                            .setCallbackData("$SURVEY_QUESTION_DELETE")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyQuestionBack)
                            .setCallbackData("$SURVEY_QUESTIONS")
                    )
                )
            }
        }
    })

    private fun showOptions(question: Question, upd: Update) = editMessage(EditMessageText().also { msg ->
        msg.chatId = upd.callbackQuery.message.chatId.toString()
        msg.messageId = upd.callbackQuery.message.messageId
        msg.text = if (!question.options.isEmpty())
            printOptions(question.options)
        else
            "NULL"
        msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
            markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
                question.options.toList().sortedBy { it.sortPoints }.forEach {
                    keyboard.add(
                        listOf(
                            InlineKeyboardButton().setText(it.text.subStr(25, "..."))
                                .setCallbackData("$SURVEY_OPTION_SELECT ${it.text.hashCode()}")
                        )
                    )
                }
                keyboard.addElements(
                    listOf(
                        InlineKeyboardButton().setText(text.surveyOptionCreate)
                            .setCallbackData("$SURVEY_OPTION_CREATE")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyOptionSelectBack)
                            .setCallbackData("$SURVEY_OPTION_SELECT_BACK")
                    )
                )
            }
        }
    })

    private fun editOption(option: Option, upd: Update) = editMessage(EditMessageText().also { msg ->
        msg.chatId = upd.callbackQuery.message.chatId.toString()
        msg.messageId = upd.callbackQuery.message.messageId
        msg.text = printOptions(setOf(option))
        msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
            markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
                keyboard.addElements(
                    listOf(
                        InlineKeyboardButton().setText(text.surveyOptionEditText)
                            .setCallbackData("$SURVEY_OPTION_EDIT_TEXT")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyOptionEditSort)
                            .setCallbackData("$SURVEY_OPTION_EDIT_SORT")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyOptionEditValue)
                            .setCallbackData("$SURVEY_OPTION_EDIT_CORRECT")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyOptionDelete)
                            .setCallbackData("$SURVEY_OPTION_DELETE ${option.text.hashCode()}")
                    ),
                    listOf(
                        InlineKeyboardButton().setText(text.surveyOptionBack)
                            .setCallbackData("$SURVEY_OPTIONS")
                    )
                )
            }
        }
    })

    private fun sendTable(chatId: Long, excelEntities: Iterable<ExcelEntity>, entityName: String = "") = try {
        excelEntities.firstOrNull()?.let {
            val file = File(
                "tableFiles/${entityName}_" +
                        convertTime(System.currentTimeMillis(), SimpleDateFormat("yyyy_MM_dd__HH-mm-ss")) + ".xls"
            )

            writeIntoExcel(file, excelEntities)

            val dir = File("tmpDir")

            dir.listFiles().forEach {
                if (it.delete()) log.info("Remove file from ${dir.name}")
                else log.warn("File not remove from ${dir.name}")
            }

            val src = FileInputStream(file).channel
            val resultFile = File(
                "${dir.name}/${resourceText(
                    text.fileNameTextTmp,
                    "file.name" to entityName,
                    "file.time" to convertTime(System.currentTimeMillis(), SimpleDateFormat("yyyy_MM_dd__HH-mm-ss"))
                )}"
            )

            FileOutputStream(resultFile).channel.transferFrom(src, 0, src.size())

            val sendDocumentRequest = SendDocument()
            sendDocumentRequest.setChatId(chatId)
            sendDocumentRequest.setDocument(resultFile)
            execute(sendDocumentRequest)
        } ?: {
            sendMessage(
                resourceText(text.msgDataInTableNotExist, "table.name" to entityName), chatId
            )
        }.invoke()
    } catch (e: Exception) {
        log.warn("Can't send file with list of participants", e)
    }

    private fun enterText(message: Message, text: String, textBack: String, stateBack: UserState) =
        editMessage(EditMessageText().also { msg ->
            msg.chatId = message.chatId.toString()
            msg.messageId = message.messageId
            msg.text = text
            msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
                markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
                    keyboard.addElements(
                        listOf(
                            InlineKeyboardButton().setText(textBack)
                                .setCallbackData("$stateBack")
                        )
                    )
                }
            }
        })

    private fun editMessage(msg: EditMessageText) = try {
        execute(msg)
    } catch (e: Exception) {
        log.warn(e.message, e)
    }

    private fun editMessage(old: Message, new: SendMessage) = try {
        execute(EditMessageText().also { msg ->
            msg.chatId = old.chatId.toString()
            msg.messageId = old.messageId
            msg.text = new.text
            msg.replyMarkup = new.replyMarkup as InlineKeyboardMarkup
        })
    } catch (e: Exception) {
        log.warn(e.message, e)
    }

    private fun deleteMessage(msg: Message) = try {
        execute(DeleteMessage(msg.chatId, msg.messageId))
    } catch (e: Exception) {
        log.warn(e.message, e)
    }

    private fun setTextToMessage(text: String, msgId: Int, chatId: Long) = try {
        editMessage(
            EditMessageText().also { editMessage ->
                editMessage.chatId = chatId.toString()
                editMessage.messageId = msgId
                editMessage.text = text
            }
        )
    } catch (e: Exception) {
        log.error(e.message, e)
    }

    private fun end(upd: Update, msgTest: String = text.mainMenu, menu: (msg: Message, text: String) -> Unit) {
        userStates[upd.message?.from?.id ?: upd.callbackQuery.from.id]!!.state = NONE
        menu.invoke(upd.message ?: upd.callbackQuery.message, msgTest)
    }

    private fun getUser(chatId: Long, userId: Int) = execute(GetChatMember().setChatId(chatId).setUserId(userId))

    override fun getBotUsername(): String = botUsername

    override fun getBotToken(): String = botToken
}
