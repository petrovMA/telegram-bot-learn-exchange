package root.bot

import org.apache.log4j.Logger
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import root.data.MainAdmin
import root.data.Text
import root.data.UserData
import root.data.UserState
import root.data.entity.Admin
import root.data.entity.Group
import root.data.entity.UserInGroup
import root.service.AdminService
import java.time.OffsetDateTime.now
import java.util.ArrayList
import java.util.HashMap

import root.data.UserState.*
import root.data.entity.Campaign
import root.libs.CampaignNotFoundException

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
    private val service: AdminService


    constructor(
        botUsername: String,
        botToken: String,
        tasks: Map<String, String>,
        text: Text,
        service: AdminService,
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
        service: AdminService,
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
//            if (update.message.isGroupMessage || update.message.isChannelMessage || update.message.isSuperGroupMessage) {
                val admin = service.getAdminById(sender.id)
                when {
                    mainAdmins.contains(MainAdmin(sender.id, sender.userName)) -> doSuperUserUpdate(update)
                    admin != null -> doAdminUpdate(update, admin)
                    else -> doUserCallback(update)
                }

            } catch (t: Throwable) {
                t.printStackTrace()
                log.error("error when try response to callback: ${update.callbackQuery.data}", t)
                userStates.remove(sender.id)
            }
        } else if (update.message.isUserMessage) {
            val sender = update.message.from
            try {
//            if (update.message.isGroupMessage || update.message.isChannelMessage || update.message.isSuperGroupMessage) {
                val admin = service.getAdminById(sender.id)
                when {
                    mainAdmins.contains(MainAdmin(sender.id, sender.userName)) -> doSuperUserUpdate(update)
                    admin != null -> doAdminUpdate(update, admin)
                    else -> doUserUpdate(update)
                }

            } catch (t: Throwable) {
                t.printStackTrace()
                log.error("error when try response to message: ${update.message.text}", t)
                userStates.remove(sender.id)
            }
        }
    }

    private fun doSuperUserUpdate(upd: Update) {
        when (userStates[upd.message.from.id]?.state) {
            CREATE_CAMPAIGN -> {
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superMenu(upd.message)
                    }
                    else -> try {
                        val name = upd.message.text

                        service.createCampaign(
                            Campaign(
                                name = name,
                                createDate = now(),
                                groups = emptySet()
                            )
                        )

                        successEnd(upd, text.sucCreateCampaign)
                    } catch (t: Throwable) {
                        sendMessage(text.errCreateCampaign, upd.message.chatId)
                        log.error("Campaign creating err.", t)
                    }
                }
            }
            ADD_ADMIN_TO_CAMPAIGN -> {

                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superMenu(upd.message)
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
                                    campaigns = setOf(camp)
                                )
                            )

                            successEnd(upd, text.sucAdminToCampaign)
                        } ?: {
                            sendMessage("кампания не найдена", upd.message.chatId)
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
                        superMenu(upd.message)
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
                            sendMessage("кампания не найдена", upd.message.chatId)
                        }.invoke()

                        successEnd(upd, text.sucGroupToCampaign)
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
                        superMenu(upd.message)
                    }
                    else -> try {
                        service.deleteCampaignByName(upd.message.text)
                        successEnd(upd, text.sucRemoveCampaign)
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
                        superMenu(upd.message)
                    }
                    else -> try {
                        val params = upd.message.text.split("\\s+".toRegex(), 2)
                        val adminForDelete = service.getAdminById(params[0].toInt())
                        adminForDelete!!.campaigns =
                            adminForDelete.campaigns.filter { it.name != params[1] }.toHashSet()

                        service.saveAdmin(adminForDelete)
                        successEnd(upd, text.sucRemoveAdminFromCampaign)
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
                        superMenu(upd.message)
                    }
                    else -> try {
                        val params = upd.message.text.split("\\s+".toRegex(), 2)
                        val groupId = params[0].toLong()
                        val campaign = service.getCampaignByName(params[1])
                        campaign!!.groups = campaign.groups.filter { it.groupId != groupId }.toHashSet()

                        service.updateCampaign(campaign)
                        successEnd(upd, text.sucRemoveGroupFromCampaign)
                    } catch (t: Throwable) {
                        sendMessage(text.errRemoveGroupFromCampaign, upd.message.chatId)
                        log.error("AdminGroup creating err.", t)
                    }
                }
            }
            MSG_TO_USERS -> {
                // todo and add "choose which campaign for SUPERUSER"
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superMenu(upd.message)
                    }
                    else -> try {
                        val users = service.getAllUsers()
                        if (users.count() > 0) {
                            msgToUsers(users, upd)
                            successEnd(upd, text.sucMsgToUsers)
                        } else
                            successEnd(upd, text.errMsgToUsersNotFound)
                    } catch (t: Throwable) {
                        sendMessage(text.errMsgToUsers, upd.message.chatId)
                        log.error("error msgToUsers", t)
                    }
                }
            }
            MSG_TO_CAMPAIGN -> {

                // todo and add "choose campaign for SUPERUSER"
                when (upd.message.text) {
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superMenu(upd.message)
                    }
                    else -> try {
                        service.getAllCampaigns().firstOrNull()?.groups?.let{
                            msgToCampaign(it, upd)
                            successEnd(upd, text.sucMsgToCampaign)
                        } ?: {
                            successEnd(upd, text.errMsgToCampaignNotFound)
                        }.invoke()
                    } catch (t: Throwable) {
                        sendMessage(text.errMsgToCampaign, upd.message.chatId)
                        log.error("error msgToUsers", t)
                    }
                }
            }
            else -> {
                when (upd.message.text) {
                    text.removeCampaign -> {
                        resetMenu(upd.message, text.msgRemoveCampaign)
                        userStates[upd.message.from.id] =
                            UserData(REMOVE_CAMPAIGN, upd.message.from)
                    }
                    text.removeAdminFromCampaign -> {
                        resetMenu(upd.message, text.msgRemoveAdminFromCampaign)
                        userStates[upd.message.from.id] =
                            UserData(REMOVE_ADMIN_FROM_CAMPAIGN, upd.message.from)
                    }
                    text.removeGroupFromCampaign -> {
                        resetMenu(upd.message, text.msgRemoveGroupFromCampaign)
                        userStates[upd.message.from.id] =
                            UserData(REMOVE_GROUP_FROM_CAMPAIGN, upd.message.from)
                    }
                    text.addAdminToCampaign -> {
                        resetMenu(upd.message, text.msgAdminToCampaign)
                        userStates[upd.message.from.id] =
                            UserData(ADD_ADMIN_TO_CAMPAIGN, upd.message.from)
                    }
                    text.addGroupToCampaign -> {
                        resetMenu(upd.message, text.msgGroupToCampaign)
                        userStates[upd.message.from.id] =
                            UserData(ADD_GROUP_TO_CAMPAIGN, upd.message.from)
                    }
                    text.createCampaign -> {
                        resetMenu(upd.message, text.msgCreateCampaign)
                        userStates[upd.message.from.id] =
                            UserData(CREATE_CAMPAIGN, upd.message.from)
                    }
                    text.sendToEveryUser -> {
                        resetMenu(upd.message, text.msgSendToEveryUser)
                        userStates[upd.message.from.id] =
                            UserData(MSG_TO_USERS, upd.message.from)
                    }
                    text.sendToEveryGroup -> {
                        resetMenu(upd.message, text.msgSendToEveryCampaign)
                        userStates[upd.message.from.id] =
                            UserData(MSG_TO_CAMPAIGN, upd.message.from)
                    }
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        superMenu(upd.message)
                    }
                    else -> {
                        superMenu(upd.message)
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
        when (userStates[admin.userId]?.state) {
            MSG_TO_USERS -> {

                // todo and add "choose campaign"
                val campId = admin.campaigns.first().id ?: TODO("campaign not found")

                msgToUsers(service.getUsersByCampaignId(campId), upd)
                userStates[upd.message.from.id]!!.state = NONE
            }
            MSG_TO_CAMPAIGN -> {

                // todo and add "choose campaign for ADMIN"
                val groups = admin.campaigns.firstOrNull()?.groups ?: TODO("campaign not found")

                msgToCampaign(groups, upd)
                userStates[upd.message.from.id]!!.state = NONE
            }
            else -> {
                when (upd.message.text) {
                    text.sendToEveryUser -> {
                        sendMessage(text.msgSendToEveryUser, upd.message.chatId)
                        userStates[upd.message.from.id] =
                            UserData(MSG_TO_USERS, upd.message.from)
                    }
                    text.sendToEveryGroup -> {
                        sendMessage(text.msgSendToEveryCampaign, upd.message.chatId)
                        userStates[upd.message.from.id] =
                            UserData(MSG_TO_CAMPAIGN, upd.message.from)
                    }
                    text.reset -> {
                        userStates.remove(upd.message.from.id)
                        adminMenu(upd.message)
                    }
                    else -> {
                        adminMenu(upd.message)
                    }
                }
            }
        }
    }

    private fun doUserUpdate(upd: Update) {
        when (userStates[upd.message.from.id]?.state) {
            JOIN_TO_CAMPAIGN -> {
                val userChats = getAllUserChats(
                    service.getAllGroups().toList(),
                    upd.message.from.id
                )
            }
            USER_MENU -> {

            }
            else -> {
                when (upd.message.text) {
                    text.joinToCampaign -> {
                        val userChats = getAllUserChats(
                            service.getAllGroups().toList(),
                            upd.message.from.id
                        )

                        if (userChats.isNotEmpty()) {
                            val availableCampaigns = service.getAllCampaignsByChatListNotContainsUser(
                                userChats.map { it.groupId },
                                upd.message.from.id
                            ).toList()

                            if (availableCampaigns.isNotEmpty()) {
                                sendAvailableCampaignsList(upd.message.chatId, availableCampaigns)
                                userStates[upd.message.from.id] =
                                    UserData(USER_CAMPAIGN_MENU, upd.message.from)
                            } else
                                sendMessage(
                                    text.msgUserAvailableCampaignsNotFound,
                                    upd.message.chatId
                                )
                        } else {
                            sendMessage(text.inviteText, upd.message.chatId)
                        }
                    }
                    text.showUserCampaigns -> {

                    }
                    else -> {
                        userMenu(upd.message)
                    }
                }
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
            execute(callbackAnswer.also { it.text = text.errClbUserAddedToCampaign })
            throw e
        }

        val campaignForAdd = service.getCampaignById(params[1].toLong()) ?: throw CampaignNotFoundException()

        when (callBackCommand) {
            USER_CAMPAIGN_MENU -> {
                service.getUserById(upd.callbackQuery.from.id)?.let {
                    service.createOrUpdateGroupUser(
                        UserInGroup(
                            upd.callbackQuery.from.id,
                            createDate = now(),
                            firstName = upd.callbackQuery.from.firstName,
                            lastName = upd.callbackQuery.from.lastName,
                            userName = upd.callbackQuery.from.userName,
                            campaigns = it.campaigns + campaignForAdd
                        )
                    )
                } ?: {
                    service.createOrUpdateGroupUser(
                        UserInGroup(
                            upd.callbackQuery.from.id,
                            createDate = now(),
                            firstName = upd.callbackQuery.from.firstName,
                            lastName = upd.callbackQuery.from.lastName,
                            userName = upd.callbackQuery.from.userName,
                            campaigns = setOf(campaignForAdd)
                        )
                    )
                }.invoke()
                setTextToMessage(
                    text.userAddedToCampaign,
                    upd.callbackQuery.message.messageId,
                    upd.callbackQuery.message.chatId
                )
                execute(callbackAnswer.also { it.text = text.clbUserAddedToCampaign })
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


    private fun showTaskList(upd: Update) {
        try {

            val message = SendMessage()

            val markupInline = InlineKeyboardMarkup()
            val rowsInline = ArrayList<List<InlineKeyboardButton>>()
            val rowInline = ArrayList<InlineKeyboardButton>()
            tasks.forEach { name, _ ->
                rowInline.add(
                    InlineKeyboardButton()
                        .setText(name)
                        .setCallbackData(name)
                )
            }
            // Set the keyboard to the markup
            rowsInline.add(rowInline)
            // Add it to the message
            markupInline.keyboard = rowsInline
            message.replyMarkup = markupInline
            message.text = "root/groups"

            sendMessage(message, upd.message.chatId)
        } catch (e: Exception) {
            log.error(e.message, e)
        }
    }

    private fun superMenu(message: Message) = sendMessage(SendMessage().also { msg ->
        msg.text = text.mainMenu
        msg.enableMarkdown(true)
        msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
            markup.selective = true
            markup.resizeKeyboard = true
            markup.oneTimeKeyboard = false
            markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
                keyboard.add(KeyboardRow().also {
                    it.add(text.sendToEveryUser)
                    it.add(text.sendToEveryGroup)
                })
                keyboard.add(KeyboardRow().also {
                    it.add(text.addGroupToCampaign)
                    it.add(text.addAdminToCampaign)
                    it.add(text.createCampaign)
                })
                keyboard.add(KeyboardRow().also {
                    it.add(text.removeGroupFromCampaign)
                    it.add(text.removeAdminFromCampaign)
                    it.add(text.removeCampaign)
                })
            }
        }
    }, message.chatId)

    private fun adminMenu(message: Message) = sendMessage(SendMessage().also { msg ->
        msg.text = text.mainMenu
        msg.enableMarkdown(true)
        msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
            markup.selective = true
            markup.resizeKeyboard = true
            markup.oneTimeKeyboard = false
            markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
                keyboard.add(KeyboardRow().also {
                    it.add(text.sendToEveryUser)
                    it.add(text.sendToEveryGroup)
                })
            }
        }
    }, message.chatId)

    private fun userMenu(message: Message) = sendMessage(SendMessage().also { msg ->
        msg.text = text.userMainMenu
        msg.enableMarkdown(true)
        msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
            markup.selective = true
            markup.resizeKeyboard = true
            markup.oneTimeKeyboard = false
            markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
                keyboard.add(KeyboardRow().also {
                    it.add(text.joinToCampaign)
                })
            }
        }
    }, message.chatId)

    private fun resetMenu(message: Message, text: String) = sendMessage(SendMessage().also { msg ->
        msg.text = text
        msg.enableMarkdown(true)
        msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
            markup.selective = true
            markup.resizeKeyboard = true
            markup.oneTimeKeyboard = false
            markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
                keyboard.add(KeyboardRow().also {
                    it.add(this.text.reset)
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
            // fixme check other user statuses (NOT ONLY MEMBER)
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

    private fun msgToUsers(users: Iterable<UserInGroup>, upd: Update) = users.forEach {
        execute(
            ForwardMessage(
                it.userId.toLong(),
                upd.message.chatId,
                upd.message.messageId
            )
        )
    }

    private fun sendAvailableCampaignsList(chatId: Long, campaigns: Iterable<Campaign>) =
        sendMessage(SendMessage().also { msg ->
            msg.text = text.userAvailableCampaigns
            msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
                markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
                    keyboard.add(
                        campaigns.map {
                            InlineKeyboardButton().setText(it.name).setCallbackData("USER_CAMPAIGN_MENU ${it.id}")
                        }
                    )
                }
            }
        }, chatId)

    private fun editMessage(msg: EditMessageText) = try {
        execute(msg)
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

    private fun successEnd(upd: Update, msgTest: String) {
        sendMessage(msgTest, upd.message.chatId)
        userStates[upd.message.from.id]!!.state = NONE
        superMenu(upd.message)
    }


    private fun getUser(chatId: Long, userId: Int) = execute(GetChatMember().setChatId(chatId).setUserId(userId))

    override fun getBotUsername(): String = botUsername

    override fun getBotToken(): String = botToken
}
