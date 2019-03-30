package root.bot

import org.apache.log4j.Logger
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import root.bot.commands.doSuperAdminCallback
import root.bot.commands.doMainAdminUpdate
import root.bot.commands.doSuperAdminUpdate
import root.data.*
import root.service.Service
import java.time.OffsetDateTime.now
import java.util.ArrayList
import java.util.HashMap

import root.data.UserState.*
import root.data.dao.SurveyDAO
import root.data.entity.*
import root.data.entity.tasks.PassedTask
import root.libs.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.Serializable
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
    private val stickers: Map<String, SquadSticker>


    constructor(
        botUsername: String,
        botToken: String,
        tasks: Map<String, String>,
        text: Text,
        service: Service,
        mainAdmins: List<MainAdmin> = emptyList(),
        stickers: Map<String, SquadSticker>,
        options: DefaultBotOptions?
    ) : super(options) {
        this.botUsername = botUsername
        this.botToken = botToken
        this.tasks = tasks
        this.text = text
        this.service = service
        this.stickers = stickers
        this.mainAdmins = mainAdmins
    }

    constructor(
        botUsername: String,
        botToken: String,
        tasks: Map<String, String>,
        text: Text,
        service: Service,
        stickers: Map<String, SquadSticker>,
        mainAdmins: List<MainAdmin>
    ) : super() {
        this.botUsername = botUsername
        this.botToken = botToken
        this.tasks = tasks
        this.text = text
        this.service = service
        this.stickers = stickers
        this.mainAdmins = mainAdmins
    }

    override fun onUpdateReceived(update: Update) {
        log.info(
            "\nMessage: " + update.message?.text +
                    "\nFromMsg: " + update.message?.from +
                    "\nChat: " + update.message?.chat +
                    "\nCallbackQuery: " + update.callbackQuery?.data +
                    "\nFromCallBck: " + update.callbackQuery?.from +
                    "\nChatId: " + update.message?.chatId +
                    "\nSticker: " + update.message?.sticker
        )
        if (update.hasCallbackQuery()) {
            val sender = update.callbackQuery.from
            try {
//            todo SPLIT COMMAND HERE TO THREADS (TEST IF SOME PART WAS DELETED (what will happen with PASSED_SURVEY if SURVEY will be deleted))
//            todo if (update.message.isGroupMessage || update.message.isChannelMessage || update.message.isSuperGroupMessage) {
                when {
                    mainAdmins.contains(MainAdmin(sender.id, sender.userName))
                            || service.getSuperAdminById(sender.id) != null -> {
                        val errorAnswer: () -> Unit = {
                            setTextToMessage(
                                resourceText(text.errCommon),
                                update.callbackQuery.message.messageId,
                                chatId(update)
                            )

                            userStates.remove(update.callbackQuery.from.id)
                            clbExecute(AnswerCallbackQuery().apply {
                                callbackQueryId = update.callbackQuery.id
                                text = this@TelegramBot.text.errClbCommon
                            })
                        }

                        userStates[fromId(update)]?.apply {
                            doSuperAdminCallback(
                                upd = update,
                                text = text,
                                service = service,
                                send = { method: SendMessage -> execute(method) },
                                editMessage = { message: EditMessageText -> editMessage(message) },
                                sendTable = { chatId: Long, excelEntities: Iterable<ExcelEntity>, entityName: String ->
                                    sendTable(
                                        chatId,
                                        excelEntities,
                                        entityName
                                    )
                                },
                                clbExecute = { callback: AnswerCallbackQuery -> clbExecute(callback) },
                                executeEdit = { message: EditMessageText -> execute(message) },
                                deleteMessage = { message: Message -> deleteMessage(message) },
                                errorAnswer = errorAnswer
                            )
                        } ?: {
                            errorAnswer()
                            userStates[fromId(update)] = UserData(NONE, message(update).from)
                        }()
                    }
                    else -> service.getAdminById(sender.id)?.let { doAdminCallback(update) } ?: doUserCallback(update)
                }

            } catch (t: Throwable) {
                t.printStackTrace()
                log.error("error when try response to callback: ${update.callbackQuery.data}", t)
                clbExecute(AnswerCallbackQuery().also {
                    it.callbackQueryId = update.callbackQuery.id
                    it.text = text.errCallback
                })
                deleteMessage(update.callbackQuery.message)
                userStates.remove(sender.id)
            }
        } else if (update.message.isUserMessage) {
            val sender = update.message.from
//            todo if (update.message.isGroupMessage || update.message.isChannelMessage || update.message.isSuperGroupMessage) {
            try {
                when {
                    mainAdmins.contains(MainAdmin(sender.id, sender.userName)) -> {
                        userStates[fromId(update)]?.apply {
                            doMainAdminUpdate(
                                upd = update,
                                text = text,
                                service = service,
                                mainAdmins = mainAdmins,
                                send = { method: SendMessage -> execute(method) },
                                editMessage = { message: EditMessageText -> editMessage(message) },
                                sendTable = { chatId: Long, excelEntities: Iterable<ExcelEntity>, entityName: String ->
                                    sendTable(
                                        chatId,
                                        excelEntities,
                                        entityName
                                    )
                                })
                        } ?: {
                            sendMessage(
                                mainAdminsMenu(text),
                                chatId(update)
                            )
                            userStates[fromId(update)] = UserData(NONE, message(update).from)
                        }()
                    }
                    else -> service.getSuperAdminById(sender.id)?.let {
                        if (!it.equals(message(update).from)) {
                            it.update(message(update).from)
                            service.saveSuperAdmin(it)
                        }
                        userStates[fromId(update)]?.apply {
                            doSuperAdminUpdate(
                                upd = update,
                                text = text,
                                service = service,
                                send = { method: SendMessage -> execute(method) },
                                editMessage = { message: EditMessageText -> editMessage(message) },
                                sendTable = { chatId: Long, excelEntities: Iterable<ExcelEntity>, entityName: String ->
                                    sendTable(
                                        chatId,
                                        excelEntities,
                                        entityName
                                    )
                                })
                        } ?: {
                            sendMessage(
                                mainAdminsMenu(text),
                                chatId(update)
                            )
                            userStates[fromId(update)] = UserData(NONE, message(update).from)
                        }()
                    }
                        ?: service.getAdminById(sender.id)?.let {
                            try {
                                doAdminUpdate(update, it)
                            } catch (t: Throwable) {
                                t.printStackTrace()
                                log.error("error when try response to message: ${update.message.text}", t)
                                userStates.remove(sender.id)
                                sendMessage(mainAdminsMenu(text, text.errAdmins), chatId(update))
                            }
                        } ?: try {
                            doUserUpdate(update)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            log.error("error when try response to message: ${update.message.text}", t)
                            userStates.remove(sender.id)
                            sendMessage(mainUsersMenu(text, text.errUsers), chatId(update))
                        }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                log.error("error when try response to message:\n${update.message}", t)
                userStates.remove(sender.id)
            }
        }
    }

    private fun doAdminUpdate(upd: Update, admin: Admin) {
        if (!admin.equals(message(upd).from)) {
            admin.update(message(upd).from)
            service.saveAdmin(admin)
        }
        val actionBack: () -> Unit = {
            when (userStates[fromId(upd)]?.state) {
                MAIN_MENU_ADD_MISSION, MAIN_MENU_ADD_TASK, MAIN_MENU_ADD_ADMIN -> {
                    userStates[fromId(upd)] = UserData(MAIN_MENU_ADD, message(upd).from)
                    sendMessage(mainAdminAddMenu(text), chatId(upd))
                }
                MAIN_MENU_DELETE_MISSION, MAIN_MENU_DELETE_TASK, MAIN_MENU_DELETE_ADMIN -> {
                    userStates[fromId(upd)] = UserData(MAIN_MENU_DELETE, message(upd).from)
                    sendMessage(mainAdminDeleteMenu(text), chatId(upd))
                }
                else -> {
                    userStates.remove(fromId(upd))
                    sendMessage(mainAdminsMenu(text, text.infoForAdmin), chatId(upd))
                }
            }
        }

        when (userStates[admin.userId]?.state) {
            MAIN_MENU_ADD -> {
                when (message(upd).text) {
                    text.addMenuMission -> {
                        userStates[fromId(upd)] = UserData(MAIN_MENU_ADD_MISSION, message(upd).from)
                        TODO("MAIN_MENU_ADD_MISSION")
                    }
                    text.addMenuTask -> {
                        userStates[fromId(upd)] = UserData(MAIN_MENU_ADD_TASK, message(upd).from)
                        TODO("MAIN_MENU_ADD_TASK")
                    }
                    text.addMenuAdmin -> {
                        sendMessage(
                            msgAvailableCampaignsListDivideCommon(
                                text.msgAdminToCampaignSelectCamp,
                                MAIN_MENU_ADD_ADMIN.toString(),
                                admin.campaigns
                            ), chatId(upd)
                        )
                    }
                    text.back -> actionBack()
                }
            }
            MAIN_MENU_DELETE -> {
                when (message(upd).text) {
                    text.deleteMenuMission -> {
                        userStates[fromId(upd)] = UserData(MAIN_MENU_DELETE_MISSION, message(upd).from)
                        TODO("MAIN_MENU_DELETE_MISSION")
                    }
                    text.deleteMenuTask -> {
                        userStates[fromId(upd)] = UserData(MAIN_MENU_DELETE_TASK, message(upd).from)
                        TODO("MAIN_MENU_DELETE_TASK")
                    }
                    text.deleteMenuAdmin -> {
                        userStates[fromId(upd)] = UserData(MAIN_MENU_DELETE_ADMIN, message(upd).from)
                        sendMessage(
                            msgAvailableCampaignsListDivideCommon(
                                text.msgRemoveAdminFromCampaign,
                                MAIN_MENU_DELETE_ADMIN.toString(),
                                service.getAllCampaigns()
                            ), chatId(upd)
                        )
                    }
                    text.back -> actionBack()
                }
            }
            MAIN_MENU_ADD_ADMIN -> {
                when (message(upd).text) {
                    text.back -> actionBack()
                    else -> try {
                        val params = message(upd).text.split("\\s+".toRegex())
                        val adminId = params[0].toInt()
                        val camp = userStates[fromId(upd)]!!.campaign!!

                        val userId = fromId(upd)

                        val (addedAdmin, campaign) = service.addAdmin(
                            userId = userId,
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
                            ), userId.toLong()
                        )

                    } catch (e: NoAccessException) {
                        sendMessage(text.errAddAdminAccessDenied, chatId(upd))
                        log.error("AdminGroup creating err (access denied).", e)
                    } catch (t: Throwable) {
                        sendMessage(text.errAddAdmin, chatId(upd))
                        log.error("AdminGroup creating err.", t)
                    }
                }
            }
            MAIN_MENU_DELETE_ADMIN -> {
                when (message(upd).text) {
                    text.back -> actionBack()
                    else -> try {
                        val params = message(upd).text.split("\\s+".toRegex(), 2)
                        val adminId = params[0].toInt()
                        val camp = userStates[fromId(upd)]!!.campaign!!

                        val userId = fromId(upd)

                        val (deletedAdmin, campaign) = service.deleteAdmin(
                            userId = userId,
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
                            ), userId.toLong()
                        )

                    } catch (e: AdminNotFoundException) {
                        sendMessage(text.errDeleteAdminNotFound, chatId(upd))
                        log.error("AdminGroup deleting err (not found).", e)
                    } catch (e: NoAccessException) {
                        sendMessage(text.errDeleteAdminAccessDenied, chatId(upd))
                        log.error("AdminGroup deleting err (access denied).", e)
                    } catch (t: Throwable) {
                        sendMessage(text.errDeleteAdmin, chatId(upd))
                        log.error("AdminGroup deleting err.", t)
                    }
                }
            }
            else -> {
                when (message(upd).text) {
                    text.mainMenuAdd -> {
                        userStates[fromId(upd)] = UserData(MAIN_MENU_ADD, message(upd).from)
                        sendMessage(mainAdminAddMenu(text), chatId(upd))
                    }
                    text.mainMenuDelete -> {
                        userStates[fromId(upd)] = UserData(MAIN_MENU_DELETE, message(upd).from)
                        sendMessage(mainAdminDeleteMenu(text), chatId(upd))
                    }
                    text.mainMenuMessages -> {
                        sendMessage(msgBackMenu(text.msgSendToEveryGroup, text.reset), chatId(upd))

                        if (admin.campaigns.isNotEmpty()) {
                            sendMessage(
                                msgAvailableCampaignsListDivideCommon(
                                    text.adminAvailableCampaigns,
                                    CAMPAIGN_FOR_SEND_GROUP_MSG.toString(),
                                    admin.campaigns
                                ), chatId(upd)
                            )
                            userStates[fromId(upd)] =
                                UserData(CAMPAIGN_FOR_SEND_GROUP_MSG, message(upd).from)
                        }
                    }
                    text.mainMenuStatistic -> {
                        userStates[fromId(upd)] =
                            UserData(MAIN_MENU_STATISTIC, message(upd).from)
                        sendMessage(mainAdminStatisticMenu(text), chatId(upd))
                    }
                    text.back -> actionBack()
                    else -> {
                        when (userStates[fromId(upd)]?.state) {
                            MAIN_MENU_STATISTIC -> sendMessage(mainAdminStatisticMenu(text), chatId(upd))
                            MAIN_MENU_DELETE -> sendMessage(mainAdminDeleteMenu(text), chatId(upd))
                            MAIN_MENU_ADD -> sendMessage(mainAdminAddMenu(text), chatId(upd))
                            CAMPAIGN_FOR_SEND_GROUP_MSG -> sendMessage(mainAdminsMenu(text), chatId(upd))
                            else -> {
                                sendMessage(mainAdminsMenu(text, text.infoForAdmin), chatId(upd))
                                log.warn("Not supported action!\n${message(upd)}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun doUserUpdate(upd: Update) = when (userStates[fromId(upd)]?.state) {
        RESET -> sendMessage(mainUsersMenu(text), chatId(upd))
        USER_ENTER_EMAIL -> {

            service.saveRegistered(email = message(upd).text, user = message(upd).from)

            userStates[fromId(upd)] = UserData(USER_MENU_ACTIVE_CAMPAIGN, message(upd).from)
            sendMessage(
                userCampaignsMenu(text, service.getAllCampaignByUserId(fromId(upd)), text.msgEmailSaved),
                chatId(upd)
            )
        }
        else -> when (message(upd).text) {
            text.userMainMenuCampaigns -> {
                service.getRegistered(stubUserInCampaign(fromId(upd)))?.let {
                    userStates[fromId(upd)] = UserData(USER_MENU_ACTIVE_CAMPAIGN, message(upd).from)
                    sendMessage(
                        userCampaignsMenu(text, service.getAllCampaignByUserId(fromId(upd))),
                        chatId(upd)
                    )
                } ?: {
                    userStates[fromId(upd)] = UserData(USER_ENTER_EMAIL, message(upd).from)
                    sendMessage(userAccountRegister(text), chatId(upd))
                }()
            }
            text.userMainMenuStatus -> {
                userStates[fromId(upd)] = UserData(USER_MENU_STATUS, message(upd).from)
                sendMessage(
                    userStatusMenu(
                        text,
                        service.getUserById(userId = fromId(upd))
                    ), chatId(upd)
                )
            }
            text.userMainMenuAccount -> {
                userStates[fromId(upd)] = UserData(USER_MENU_MY_ACCOUNT, message(upd).from)
                sendMessage(userAccountMenu(text), chatId(upd))
            }
            else -> {
                userStates[fromId(upd)]?.let { sendMessage(mainUsersMenu(text), chatId(upd)) }
                    ?: stickers[text.stickerHello]?.let {
                        sendSticker(it, chatId(upd))
                        sendMessage(mainUsersMenu(text), chatId(upd))
                    } ?: sendMessage(mainUsersMenu(text), chatId(upd))

                userStates[fromId(upd)] = UserData(NONE, message(upd).from)
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
            clbExecute(callbackAnswer.also { it.text = text.errClbCommon })
            throw e
        }

        when {
            CAMPAIGN_FOR_SEND_GROUP_MSG == callBackCommand &&
                    userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SEND_GROUP_MSG -> try {

                val campaign = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()
                val groups = campaign.groups

                userStates[upd.callbackQuery.from.id] =
                    UserData(MSG_TO_CAMPAIGN, upd.callbackQuery.from, groups = groups)
                clbExecute(callbackAnswer.also { it.text = text.clbSendMessageToEveryGroup })
                setTextToMessage(
                    resourceText(text.sucSendMessageToEveryGroup, "campaign.name" to campaign.name),
                    upd.callbackQuery.message.messageId,
                    chatId(upd)
                )
            } catch (t: Throwable) {
                log.error("CAMPAIGN_FOR_SEND_GROUP_MSG execute error", t)
                clbExecute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryGroup })
                throw t
            }
            CAMPAIGN_FOR_SEND_USERS_MSG == callBackCommand &&
                    userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SEND_USERS_MSG -> try {

                val users = service.getUsersByCampaignId(params[1].toLong())

                userStates[upd.callbackQuery.from.id] =
                    UserData(MSG_TO_USERS, upd.callbackQuery.from, users = users)
                clbExecute(callbackAnswer.also { it.text = text.clbSendMessageToEveryUsers })
                setTextToMessage(
                    resourceText("${text.sucSendMessageToEveryUsers} id = ", "campaign.name" to params[1]),
                    upd.callbackQuery.message.messageId,
                    chatId(upd)
                )
            } catch (t: Throwable) {
                log.error("CAMPAIGN_FOR_SEND_USERS_MSG execute error", t)
                clbExecute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryUsers })
                throw t
            }
            MAIN_MENU_ADD_ADMIN == callBackCommand || MAIN_MENU_DELETE_ADMIN == callBackCommand -> try {

                val campaign = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()

                userStates[upd.callbackQuery.from.id] =
                    UserData(callBackCommand, upd.callbackQuery.from, campaign = campaign)
                clbExecute(callbackAnswer.also {
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
                    ), chatId(upd)
                )
            } catch (t: Throwable) {
                log.error("$callBackCommand execute error", t)
                clbExecute(callbackAnswer.also {
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
                    chatId(upd)
                )
                userStates.remove(upd.callbackQuery.from.id)
                clbExecute(callbackAnswer.also { it.text = text.errClbCommon })
            }
        }
    }

    private fun doUserCallback(upd: Update) {
        val params = upd.callbackQuery.data.split("\\s+".toRegex())
        val callbackAnswer = AnswerCallbackQuery()
        val callBackCommand: UserState
        callbackAnswer.callbackQueryId = upd.callbackQuery.id

        try {
            callBackCommand = UserState.valueOf(params[0])
        } catch (e: Exception) {
            log.error("UserState = \"${upd.callbackQuery.data}\", not found", e)
            clbExecute(callbackAnswer.also { it.text = text.errClbUser })
            throw e
        }

        val timeOutBack = {
            clbExecute(callbackAnswer.also { it.text = text.clbSurveyTimeOut })
            deleteMessage(upd.callbackQuery.message)
            sendMessage(mainUsersMenu(text, text.errUsers), chatId(upd))
        }


        when (callBackCommand) {
            JOIN_TO_CAMPAIGN -> {
                val userChats = getAllUserChats(service.getAllGroups().toList(), fromId(upd))

                clbExecute(callbackAnswer.also { it.text = text.clbSearchCampForUser })

                if (userChats.isNotEmpty()) {
                    val availableCampaigns = service.getAllCampaignsByChatListNotContainsUser(
                        userChats.map { it.groupId },
                        fromId(upd)
                    ).toList()

                    if (availableCampaigns.isNotEmpty()) {
                        execute(
                            editMessage(
                                message(upd),
                                userJoinToCampaigns(text, availableCampaigns, text.userAvailableCampaigns)
                            )
                        )
                        userStates[fromId(upd)] = UserData(JOIN_TO_CAMPAIGN_MENU, message(upd).from)
                    } else execute(
                        editMessage(
                            message(upd),
                            userCampaignsMenu(
                                text,
                                service.getAllCampaignByUserId(fromId(upd)),
                                text.msgUserAvailableCampaignsNotFound
                            )
                        )
                    )
                } else execute(
                    editMessage(
                        message(upd),
                        userJoinToCampaigns(text, emptyList(), text.inviteText)
                    )
                )
            }
            JOIN_TO_CAMPAIGN_BACK -> execute(
                editMessage(
                    message(upd),
                    userCampaignsMenu(text, service.getAllCampaignByUserId(fromId(upd)))
                )
            )
            JOIN_TO_CAMPAIGN_MENU -> {
                val campaignForAdd = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()
                clbExecute(callbackAnswer.also { it.text = text.clbUserAddedToCampaign })
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
                }()
                execute(
                    editMessage(
                        message(upd),
                        userCampaignsMenu(text, service.getAllCampaignByUserId(fromId(upd)), text.userAddedToCampaign)
                    )
                )
            }
            USER_MENU_ACTIVE_CAMPAIGN_SELECT -> {
                if (userStates[upd.callbackQuery.from.id]?.state == USER_MENU_ACTIVE_CAMPAIGN) {
                    clbExecute(callbackAnswer.also { it.text = text.clbSurveyCollectProcess })
                    val tasks = service.getAllSurveyForUser(params[1].toLong(), upd.callbackQuery.from.id).toList()
                    if (tasks.isNotEmpty())
                        execute(
                            editMessage(
                                upd.callbackQuery.message,
                                msgTaskList(
                                    text.sendChooseTask,
                                    CHOOSE_TASK.toString(),
                                    tasks
                                )
                            )
                        )
                    else sendMessage(mainUsersMenu(text, text.userTaskNotFound), chatId(upd))
                } else timeOutBack()
            }
            USER_MENU_ACTIVE_COMMON_CAMPAIGN_SELECT -> {
                if (userStates[upd.callbackQuery.from.id]?.state == USER_MENU_ACTIVE_CAMPAIGN) {
                    clbExecute(callbackAnswer.also { it.text = text.clbSurveyCollectProcess })

                    val tasks = service.getAllTasksByUserFromCampaigns(upd.callbackQuery.from.id, true).toList()

                    if (tasks.isNotEmpty())
                        execute(
                            editMessage(
                                upd.callbackQuery.message,
                                msgTaskList(
                                    text.sendChooseTask,
                                    CHOOSE_TASK.toString(),
                                    tasks
                                )
                            )
                        )
                    else sendMessage(mainUsersMenu(text, text.userTaskNotFound), chatId(upd))
                } else timeOutBack()
            }
            CHOOSE_TASK -> {
                if (userStates[upd.callbackQuery.from.id]?.state == USER_MENU_ACTIVE_CAMPAIGN) {
                    val survey = service.getSurveyById(params[1].toLong()) ?: throw SurveyNotFoundException()

                    userStates[upd.callbackQuery.from.id]!!.apply {
                        this.survey = survey
                        this.surveyInProgress = SurveyDAO(
                            id = survey.id!!,
                            name = survey.name,
                            description = survey.description,
                            createDate = survey.createDate,
                            questions = survey.questions.toList().sortedBy { it.sortPoints },
                            state = 0
                        )
                    }

                    execute(
                        editMessage(
                            upd.callbackQuery.message,
                            msgQuestion(
                                text,
                                userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!,
                                "$SURVEY_USERS_ANSWER"
                            )
                        )
                    )
                } else timeOutBack()
            }
            SURVEY_USERS_ANSWER -> {
                if (userStates[upd.callbackQuery.from.id]?.state == USER_MENU_ACTIVE_CAMPAIGN) {
                    val prevState = userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.state
                    val question = userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.questions[prevState]
                    val prevAnswer = question.options.first { it.id == params[1].toLong() }

                    userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.apply {
                        if (correct) correct = prevAnswer.correct
                        currentValue += prevAnswer.value
                        state++
                    }

                    if (userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.let { it.state < it.questions.size })
                        execute(
                            editMessage(
                                upd.callbackQuery.message,
                                msgQuestion(
                                    text,
                                    userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!,
                                    "$SURVEY_USERS_ANSWER"
                                )
                            )
                        )
                    else {
                        if (userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.correct) {
                            service.getUserById(upd.callbackQuery.from.id)?.let { oldUser ->
                                val oldLevel = oldUser.level

                                service.savePassedTaskAndUpdateUser(
                                    PassedTask(
                                        value = userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.currentValue,
                                        description = userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.description,
                                        passDate = now(),
                                        task = userStates[upd.callbackQuery.from.id]!!.survey!!,
                                        user = oldUser
                                    )
                                ).also { newUser ->
                                    if (oldLevel != newUser.level) {
                                        when (newUser.level) {
                                            1 -> {
                                                sendSticker(stickers.getValue("recruit"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            2 -> {
                                                sendSticker(stickers.getValue("kadet"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            3 -> {
                                                sendSticker(stickers.getValue("explorer"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            4 -> {
                                                sendSticker(stickers.getValue("engineer"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            5 -> {
                                                sendSticker(stickers.getValue("professor"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            6 -> {
                                                sendSticker(stickers.getValue("captain"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            7 -> {
                                                sendSticker(stickers.getValue("ranger"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                        }
                                    }
                                }
                            } ?: {
                                val user = service.createOrUpdateGroupUser(
                                    UserInCampaign(
                                        userId = upd.callbackQuery.from.id,
                                        firstName = upd.callbackQuery.from.firstName,
                                        lastName = upd.callbackQuery.from.lastName,
                                        userName = upd.callbackQuery.from.userName,
                                        createDate = now(),
                                        campaigns = emptySet()
                                    )
                                )
                                service.savePassedTaskAndUpdateUser(
                                    PassedTask(
                                        value = userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.currentValue,
                                        description = userStates[upd.callbackQuery.from.id]!!.surveyInProgress!!.description,
                                        passDate = now(),
                                        task = userStates[upd.callbackQuery.from.id]!!.survey!!,
                                        user = user
                                    )
                                ).also { newUser ->
                                    if (1000 <= newUser.value)
                                        when (newUser.level) {
                                            1 -> {
                                                sendSticker(stickers.getValue("recruit"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            2 -> {
                                                sendSticker(stickers.getValue("kadet"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            3 -> {
                                                sendSticker(stickers.getValue("explorer"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            4 -> {
                                                sendSticker(stickers.getValue("engineer"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            5 -> {
                                                sendSticker(stickers.getValue("professor"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            6 -> {
                                                sendSticker(stickers.getValue("captain"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                            7 -> {
                                                sendSticker(stickers.getValue("ranger"), fromId(upd).toLong())
                                                sendSticker(stickers.getValue("2_rank"), fromId(upd).toLong())
                                            }
                                        }
                                }
                            }()
                            execute(
                                editMessage(
                                    upd.callbackQuery.message, userCampaignsMenu(
                                        text,
                                        service.getAllCampaignByUserId(upd.callbackQuery.from.id),
                                        text.msgUserTaskPassed
                                    )
                                )
                            )
                        } else
                            execute(
                                editMessage(
                                    upd.callbackQuery.message, userCampaignsMenu(
                                        text,
                                        service.getAllCampaignByUserId(upd.callbackQuery.from.id),
                                        text.msgUserTaskFailed
                                    )
                                )
                            )
                    }
                } else timeOutBack()
            }
            RESET -> {
                deleteMessage(upd.callbackQuery.message)
                sendMessage(mainUsersMenu(text), chatId(upd))
            }
            else -> {
                setTextToMessage(
                    text.errUserUnknownCommand,
                    upd.callbackQuery.message.messageId,
                    chatId(upd)
                )
                userStates.remove(upd.callbackQuery.from.id)
                clbExecute(callbackAnswer.also { it.text = text.clbUserUnknownCommand })
            }
        }
    }

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

    private fun sendSticker(sticker: SquadSticker, chatId: Long) = try {
        log.debug("Send sticker to chatId = $chatId\nSticker: \"${sticker.path}\"")
        execute(sticker.getSticker().apply { setChatId(chatId) })
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
                chatId(upd),
                message(upd).messageId
            )
        )
    }

    private fun msgToUsers(users: Iterable<UserInCampaign>, upd: Update) = users.forEach {
        execute(
            ForwardMessage(
                it.userId.toLong(),
                chatId(upd),
                message(upd).messageId
            )
        )
    }

    private fun sendTable(chatId: Long, excelEntities: Iterable<ExcelEntity>, entityName: String) = try {
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
        }()
    } catch (e: Exception) {
        log.warn("Can't send file with list of participants", e)
    }

    private fun editMessage(msg: EditMessageText) = try {
        execute(msg)
    } catch (e: Exception) {
        log.warn(e.message, e)
    }

    override fun <T : Serializable, M : BotApiMethod<T>> execute(method: M): T? = try {
        log.debug("Execute method:\n$method")
        super.execute(method)
    } catch (t: Throwable) {
        log.error("Error execute method:\n$method\n", t)
        null
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

    private fun clbExecute(callback: AnswerCallbackQuery) = try {
        execute(callback)
    } catch (t: Throwable) {
        log.error("CallbackQuery execution Error. \nAnswerCallbackQuery:\n$callback\n", t)
    }

    private fun getUser(chatId: Long, userId: Int) = super.execute(GetChatMember().setChatId(chatId).setUserId(userId))

    override fun getBotUsername(): String = botUsername

    override fun getBotToken(): String = botToken
}
