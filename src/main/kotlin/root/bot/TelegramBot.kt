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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import root.bot.commands.doMainAdminUpdate
import root.data.*
import root.service.Service
import java.time.OffsetDateTime.now
import java.util.ArrayList
import java.util.HashMap

import root.data.UserState.*
import root.data.dao.SurveyDAO
import root.data.entity.*
import root.data.entity.tasks.PassedTask
import root.data.entity.tasks.surveus.Question
import root.data.entity.tasks.surveus.Survey
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
        mainAdmins: List<MainAdmin>,
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
                    mainAdmins.contains(MainAdmin(sender.id, sender.userName)) -> doMainAdminCallback(update)
                    else -> service.getSuperAdminById(sender.id)?.let { doSuperAdminCallback(update) }
                        ?: service.getAdminById(sender.id)?.let { doAdminCallback(update) } ?: doUserCallback(update)
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
                    }.invoke()
                }
                else -> service.getSuperAdminById(sender.id)?.let { doSuperAdminUpdate(update, it) }
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
        }
    }

    private fun doSuperAdminUpdate(upd: Update, admin: SuperAdmin) {
        if (!admin.equals(message(upd).from)) {
            admin.update(message(upd).from)
            service.saveSuperAdmin(admin)
        }
        when (userStates[fromId(upd)]?.state) {
            CREATE_CAMPAIGN -> {
                when (message(upd).text) {
                    text.reset -> {
                        userStates.remove(fromId(upd))
                        superAdminMenu(message(upd))
                    }
                    else -> try {
                        val name = message(upd).text

                        service.createCampaign(
                            Campaign(
                                name = name,
                                createDate = now(),
                                groups = HashSet()
                            )
                        )

                        end(upd, text.sucCreateCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                    } catch (t: Throwable) {
                        sendMessage(text.errCreateCampaign, chatId(upd))
                        log.error("Campaign creating err.", t)
                    }
                }
            }
            MAIN_MENU_ADD_ADMIN -> {
                when (message(upd).text) {
                    text.reset -> {
                        userStates.remove(fromId(upd))
                        superAdminMenu(message(upd))
                    }
                    else -> try {
                        val ids = message(upd).text.split("\\s+".toRegex(), 2)
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
                            sendMessage(text.errCampaignNotFound, chatId(upd))
                        }.invoke()

                    } catch (t: Throwable) {
                        sendMessage(text.errAdminToCampaign, chatId(upd))
                        log.error("AdminGroup creating err.", t)
                    }
                }
            }
            ADD_GROUP_TO_CAMPAIGN -> {
                when (message(upd).text) {
                    text.reset -> {
                        userStates.remove(fromId(upd))
                        superAdminMenu(message(upd))
                    }
                    else -> try {
                        val ids = message(upd).text.split("\\s+".toRegex(), 2)
                        val groupId = ids[0].toLong()
                        val name = ids[1]

                        service.getCampaignByName(name)?.let {
                            val group = service.createGroup(Group(groupId, now()))
                            it.groups = it.groups.toHashSet().also { gr -> gr.add(group) }
                            service.updateCampaign(it)
                        } ?: {
                            sendMessage(text.errCampaignNotFound, chatId(upd))
                        }.invoke()

                        end(upd, text.sucGroupToCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                    } catch (t: Throwable) {
                        sendMessage(text.errGroupToCampaign, chatId(upd))
                        log.error("AdminGroup creating err.", t)
                    }
                }
            }
            REMOVE_CAMPAIGN -> {
                when (message(upd).text) {
                    text.reset -> {
                        userStates.remove(fromId(upd))
                        superAdminMenu(message(upd))
                    }
                    else -> try {
                        service.deleteCampaignByName(message(upd).text)
                        end(upd, text.sucRemoveCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                    } catch (t: Throwable) {
                        sendMessage(text.errRemoveCampaign, chatId(upd))
                        log.error("Campaign creating err.", t)
                    }
                }
            }
            REMOVE_ADMIN_FROM_CAMPAIGN -> {
                when (message(upd).text) {
                    text.reset -> {
                        userStates.remove(fromId(upd))
                        superAdminMenu(message(upd))
                    }
                    else -> try {
                        val params = message(upd).text.split("\\s+".toRegex(), 2)
                        val adminForDelete = service.getAdminById(params[0].toInt())
                        adminForDelete!!.campaigns =
                            adminForDelete.campaigns.filter { it.name != params[1] }.toHashSet()

                        service.saveAdmin(adminForDelete)
                        end(upd, text.sucRemoveAdminFromCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                    } catch (t: Throwable) {
                        sendMessage(text.errRemoveAdminFromCampaign, chatId(upd))
                        log.error("AdminGroup deleting err.", t)
                    }
                }
            }
            REMOVE_GROUP_FROM_CAMPAIGN -> {
                when (message(upd).text) {
                    text.reset -> {
                        userStates.remove(fromId(upd))
                        superAdminMenu(message(upd))
                    }
                    else -> try {
                        val params = message(upd).text.split("\\s+".toRegex(), 2)
                        val groupId = params[0].toLong()
                        val campaign = service.getCampaignByName(params[1])
                        campaign!!.groups = campaign.groups.filter { it.groupId != groupId }.toHashSet()

                        service.updateCampaign(campaign)
                        end(upd, text.sucRemoveGroupFromCampaign) { msg: Message, _: String -> superAdminMenu(msg) }
                    } catch (t: Throwable) {
                        sendMessage(text.errRemoveGroupFromCampaign, chatId(upd))
                        log.error("AdminGroup creating err.", t)
                    }
                }
            }
            MSG_TO_USERS -> {
                when (message(upd).text) {
                    text.reset -> {
                        userStates.remove(fromId(upd))
                        superAdminMenu(message(upd))
                    }
                    else -> try {
                        val users = userStates[fromId(upd)]?.users
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
                        sendMessage(text.errMsgToUsers, chatId(upd))
                        log.error("error msgToUsers", t)
                    }
                }
            }
            MSG_TO_CAMPAIGN -> {
                when (message(upd).text) {
                    text.reset -> {
                        userStates.remove(fromId(upd))
                        superAdminMenu(message(upd))
                    }
                    else -> try {
                        val groups = userStates[fromId(upd)]?.groups
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
                        sendMessage(text.errMsgToCampaign, chatId(upd))
                        log.error("error msgToUsers", t)
                    }
                }
            }
            else -> {
                when (message(upd).text) {
                    text.removeCampaign -> {
                        sendMessage(msgBackMenu(text.msgRemoveCampaign, text.reset), chatId(upd))
                        userStates[fromId(upd)] =
                            UserData(REMOVE_CAMPAIGN, message(upd).from)
                    }
                    text.removeAdminFromCampaign -> {
                        sendMessage(msgBackMenu(text.msgRemoveAdminFromCampaign, text.reset), chatId(upd))
                        userStates[fromId(upd)] =
                            UserData(REMOVE_ADMIN_FROM_CAMPAIGN, message(upd).from)
                    }
                    text.removeGroupFromCampaign -> {
                        sendMessage(msgBackMenu(text.msgRemoveGroupFromCampaign, text.reset), chatId(upd))
                        userStates[fromId(upd)] =
                            UserData(REMOVE_GROUP_FROM_CAMPAIGN, message(upd).from)
                    }
                    text.addAdminToCampaign -> {
                        sendMessage(msgBackMenu(text.msgAdminToCampaignAdminId, text.reset), chatId(upd))
                        userStates[fromId(upd)] =
                            UserData(MAIN_MENU_ADD_ADMIN, message(upd).from)
                    }
                    text.addGroupToCampaign -> {
                        sendMessage(msgBackMenu(text.msgGroupToCampaign, text.reset), chatId(upd))
                        userStates[fromId(upd)] =
                            UserData(ADD_GROUP_TO_CAMPAIGN, message(upd).from)
                    }
                    text.createCampaign -> {
                        sendMessage(msgBackMenu(text.msgCreateCampaign, text.reset), chatId(upd))
                        userStates[fromId(upd)] =
                            UserData(CREATE_CAMPAIGN, message(upd).from)
                    }
                    text.sendToEveryUser -> {
                        sendMessage(msgBackMenu(text.msgSendToEveryUser, text.reset), chatId(upd))

                        val availableCampaigns = service.getAllCampaigns().toList()

                        if (availableCampaigns.isNotEmpty()) {
                            sendMessage(
                                msgAvailableCampaignsListDivideCommon(
                                    text.adminAvailableCampaigns,
                                    CAMPAIGN_FOR_SEND_USERS_MSG.toString(),
                                    availableCampaigns
                                ), chatId(upd)
                            )
                            userStates[fromId(upd)] =
                                UserData(CAMPAIGN_FOR_SEND_USERS_MSG, message(upd).from)
                        }
                    }
                    text.sendToEveryGroup -> {
                        sendMessage(msgBackMenu(text.msgSendToEveryGroup, text.reset), chatId(upd))

                        val availableCampaigns = service.getAllCampaigns().toList()

                        if (availableCampaigns.isNotEmpty()) {
                            sendMessage(
                                msgAvailableCampaignsListDivideCommon(
                                    text.adminAvailableCampaigns,
                                    CAMPAIGN_FOR_SEND_GROUP_MSG.toString(),
                                    availableCampaigns
                                ), chatId(upd)
                            )
                            userStates[fromId(upd)] =
                                UserData(CAMPAIGN_FOR_SEND_GROUP_MSG, message(upd).from)
                        }
                    }
                    text.reset -> {
                        userStates.remove(fromId(upd))
                        superAdminMenu(message(upd))
                    }
                    else -> {
                        superAdminMenu(message(upd))
                    }
                }
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
                    text.back -> actionBack.invoke()
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
                    text.back -> actionBack.invoke()
                }
            }
            MAIN_MENU_ADD_ADMIN -> {
                when (message(upd).text) {
                    text.back -> actionBack.invoke()
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
                    text.back -> actionBack.invoke()
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
                    text.back -> actionBack.invoke()
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
                }.invoke()
            }
            text.userMainMenuStatus -> {
                userStates[fromId(upd)] = UserData(USER_MENU_STATUS, message(upd).from)
                sendMessage(
                    userStatusMenu(
                        text,
                        service.getAllPassedSurveysByUser(stubUserInCampaign(userId = fromId(upd)))
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

    private fun doMainAdminCallback(upd: Update) {
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

        val errorAnswer = {
            setTextToMessage(
                resourceText(text.errCommon),
                upd.callbackQuery.message.messageId,
                chatId(upd)
            )
            userStates.remove(upd.callbackQuery.from.id)
            clbExecute(callbackAnswer.also { it.text = text.errClbCommon })
        }

        when (callBackCommand) {
            SURVEY_CREATE -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
                    this.state = callBackCommand
                    this.updCallback = upd
                }
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

                execute(
                    editMessage(
                        upd.callbackQuery.message,
                        msgAvailableCampaignsListDivideCommon(
                            text.clbSurveySave,
                            CAMPAIGN_FOR_SURVEY.toString(),
                            service.getAllCampaigns().toList()
                        )
                    )
                )
            }
            SURVEY_EDIT -> {
                userStates[upd.callbackQuery.from.id]!!.survey = service.getSurveyById(params[1].toLong())
                editMessage(editSurvey(text, userStates[upd.callbackQuery.from.id]!!.survey!!, upd))
                clbExecute(callbackAnswer.also { it.text = text.clbEditSurvey })
            }
            SURVEY -> {
                editMessage(editSurvey(text, userStates[upd.callbackQuery.from.id]!!.survey!!, upd))
                clbExecute(callbackAnswer.also { it.text = text.clbEditSurvey })
            }
            SURVEY_NAME -> {
                userStates[upd.callbackQuery.from.id]!!.apply {
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
                userStates[upd.callbackQuery.from.id]!!.apply {
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
                userStates[upd.callbackQuery.from.id]!!.apply {
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
                userStates[upd.callbackQuery.from.id]!!.apply {
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
                userStates[upd.callbackQuery.from.id]!!.apply {
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
                userStates[upd.callbackQuery.from.id]!!.apply {
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
                userStates[upd.callbackQuery.from.id]!!.apply {
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
                userStates[upd.callbackQuery.from.id]!!.apply {
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
                userStates[upd.callbackQuery.from.id]!!.apply {
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
                sendMessage(mainAdminsMenu(text), chatId(upd))
            }
            SURVEY_OPTION_SELECT_BACK -> {
                editMessage(editQuestion(text, userStates[upd.callbackQuery.from.id]!!.question!!, upd))
                clbExecute(callbackAnswer.also { it.text = text.clbSurvey })
            }
            SURVEY_QUESTIONS -> {
                userStates[upd.callbackQuery.from.id]!!.question?.let {
                    userStates[upd.callbackQuery.from.id]!!.survey!!.questions =
                        userStates[upd.callbackQuery.from.id]!!.survey!!.questions.toHashSet().apply { add(it) }
                    userStates[upd.callbackQuery.from.id]!!.question = null
                    showQuestions(userStates[upd.callbackQuery.from.id]!!.survey!!, upd)
                } ?: {
                    showQuestions(userStates[upd.callbackQuery.from.id]!!.survey!!, upd)
                    clbExecute(callbackAnswer.also { it.text = text.clbSurveyQuestions })
                }.invoke()
            }
            SURVEY_QUESTION_SELECT -> {
                userStates[upd.callbackQuery.from.id]!!.question =
                    userStates[upd.callbackQuery.from.id]!!.survey!!.questions.first { it.text.hashCode() == params[1].toInt() }
                editMessage(editQuestion(text, userStates[upd.callbackQuery.from.id]!!.question!!, upd))
                clbExecute(callbackAnswer.also { it.text = text.clbSurveyQuestionEdit })
            }
            SURVEY_QUESTION_DELETE -> {
                val survey = userStates[upd.callbackQuery.from.id]!!.survey!!
                survey.questions =
                    survey.questions.toHashSet().apply { remove(userStates[upd.callbackQuery.from.id]!!.question) }
                userStates[upd.callbackQuery.from.id]!!.question = null

                showQuestions(userStates[upd.callbackQuery.from.id]!!.survey!!, upd)
                clbExecute(callbackAnswer.also { it.text = text.clbSurveyQuestionDeleted })
            }
            SURVEY_OPTION_DELETE -> {
                val question = userStates[upd.callbackQuery.from.id]!!.question!!
                question.options =
                    question.options.toHashSet().apply { remove(userStates[upd.callbackQuery.from.id]!!.option) }
                userStates[upd.callbackQuery.from.id]!!.option = null

                showOptions(userStates[upd.callbackQuery.from.id]!!.question!!, upd)
                clbExecute(callbackAnswer.also { it.text = text.clbSurveyOptionDeleted })
            }
            SURVEY_OPTIONS -> {
                userStates[upd.callbackQuery.from.id]!!.option?.let {
                    userStates[upd.callbackQuery.from.id]!!.question!!.options =
                        userStates[upd.callbackQuery.from.id]!!.question!!.options.toHashSet().apply { add(it) }
                    userStates[upd.callbackQuery.from.id]!!.option = null
                    showOptions(userStates[upd.callbackQuery.from.id]!!.question!!, upd)
                } ?: {
                    showOptions(userStates[upd.callbackQuery.from.id]!!.question!!, upd)
                    clbExecute(callbackAnswer.also { it.text = text.clbSurveyOptions })
                }.invoke()
            }
            SURVEY_OPTION_SELECT -> {
                userStates[upd.callbackQuery.from.id]!!.option =
                    userStates[upd.callbackQuery.from.id]!!.question!!.options.first { it.text.hashCode() == params[1].toInt() }
                editMessage(editOption(text, userStates[upd.callbackQuery.from.id]!!.option!!, upd))
                clbExecute(callbackAnswer.also { it.text = text.clbSurveyOptions })
            }
            SURVEY_OPTION_EDIT_BACK -> {
                editMessage(editOption(text, userStates[upd.callbackQuery.from.id]!!.option!!, upd))
                clbExecute(callbackAnswer.also { it.text = text.clbSurveyOptions })
            }

            GET_EXCEL_TABLE_SURVEY -> {
                deleteMessage(upd.callbackQuery.message)
                sendTable(
                    chatId(upd),
                    service.getSurveyByCampaignId(params[1].toLong())
                )
            }
            GET_EXCEL_TABLE_USERS_IN_CAMPAIGN -> {
                deleteMessage(upd.callbackQuery.message)
                sendTable(
                    chatId(upd),
                    service.getUsersByCampaignId(params[1].toLong())
                )
            }
            GET_EXCEL_TABLE_ADMINS -> {
                deleteMessage(upd.callbackQuery.message)
                sendTable(
                    chatId(upd),
                    service.getAdminsByCampaigns(setOf(stubCampaign(id = params[1].toLong())))
                )
            }

            CAMPAIGN_FOR_SEND_GROUP_MSG -> if (userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SEND_GROUP_MSG) try {

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
            else errorAnswer.invoke()
            CAMPAIGN_FOR_SEND_USERS_MSG -> if (userStates[upd.callbackQuery.from.id]?.state == CAMPAIGN_FOR_SEND_USERS_MSG) try {

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
                clbExecute(callbackAnswer.also { it.text = text.errClbSendMessageToEveryUsers })
                throw t
            }
            else errorAnswer.invoke()
            MAIN_MENU_ADD_ADMIN, MAIN_MENU_ADD_GROUP, MAIN_MENU_DELETE_ADMIN, MAIN_MENU_DELETE_GROUP -> try {

                val campaign = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()

                userStates[upd.callbackQuery.from.id] =
                    UserData(callBackCommand, upd.callbackQuery.from, campaign = campaign)
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
                sendMessage(
                    msgBackMenu(
                        when (callBackCommand) {
                            MAIN_MENU_ADD_ADMIN -> text.msgAdminToCampaignAdminId
                            MAIN_MENU_DELETE_ADMIN -> text.msgAdminDeleteFromCampaignAdminId
                            MAIN_MENU_ADD_GROUP -> text.msgGroupToCampaignGroupId
                            MAIN_MENU_DELETE_GROUP -> text.msgGroupDeleteFromCampaignGroupId
                            else -> throw CommandNotFoundException()
                        }, text.back
                    ), chatId(upd)
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

    private fun doSuperAdminCallback(upd: Update) {
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
                }.invoke()
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
                } else timeOutBack.invoke()
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
                } else timeOutBack.invoke()
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
                } else timeOutBack.invoke()
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
                            }.invoke()
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
                } else timeOutBack.invoke()
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

    private fun showSurveys(surveys: List<Survey>, upd: Update) = editMessage(EditMessageText().also { msg ->
        msg.chatId = fromId(upd).toString()
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

    private fun showQuestions(survey: Survey, upd: Update) = editMessage(EditMessageText().also { msg ->
        msg.chatId = fromId(upd).toString()
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

    private fun showOptions(question: Question, upd: Update) = editMessage(EditMessageText().also { msg ->
        msg.chatId = fromId(upd).toString()
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

    private fun end(upd: Update, msgTest: String = text.mainMenu, menu: (msg: Message, text: String) -> Unit) {
        userStates[message(upd).from?.id ?: upd.callbackQuery.from.id]!!.state = NONE
        menu.invoke(message(upd), msgTest)
    }

    private fun getUser(chatId: Long, userId: Int) = super.execute(GetChatMember().setChatId(chatId).setUserId(userId))

    override fun getBotUsername(): String = botUsername

    override fun getBotToken(): String = botToken
}
