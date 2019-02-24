package root.bot

import org.apache.log4j.Logger
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import root.data.SuperUser
import root.data.Text
import root.data.UserData
import root.data.entity.Admin
import root.data.entity.Group
import root.data.entity.UserInGroup
import root.service.AdminService
import java.time.OffsetDateTime.now
import java.util.ArrayList
import java.util.HashMap

import root.data.UserState.*
import root.data.entity.Campaign

class TelegramBot : TelegramLongPollingBot {

    private val userStates: HashMap<Int, UserData> = HashMap()

    companion object {
        private val log = Logger.getLogger(TelegramBot::class.java)!!
    }

    private val botUsername: String
    private val botToken: String
    private val tasks: Map<String, String>
    private val text: Text
    private val superUsers: List<SuperUser>
    private val service: AdminService


    constructor(
        botUsername: String,
        botToken: String,
        tasks: Map<String, String>,
        text: Text,
        service: AdminService,
        superUsers: List<SuperUser>,
        options: DefaultBotOptions?
    ) : super(options) {
        this.botUsername = botUsername
        this.botToken = botToken
        this.tasks = tasks
        this.text = text
        this.service = service
        this.superUsers = superUsers
    }

    constructor(
        botUsername: String,
        botToken: String,
        tasks: Map<String, String>,
        text: Text,
        service: AdminService,
        superUsers: List<SuperUser>
    ) : super() {
        this.botUsername = botUsername
        this.botToken = botToken
        this.tasks = tasks
        this.text = text
        this.service = service
        this.superUsers = superUsers
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
//        log.info(service.getAllUserIdInCampaigns(setOf(Campaign(-252798409L, now()))))
        try {
            val admin = service.getAdminById(update.message.from.id)
            if (superUsers.contains(SuperUser(update.message.from.id, update.message.from.userName))) {
                when (userStates[update.message.from.id]?.state) {
                    CREATE_CAMPAIGN -> {
                        try {
                            val name = update.message.text

                            service.createCampaign(
                                Campaign(
                                    name = name,
                                    createDate = now(),
                                    groups = emptySet(),
                                    users = emptySet()
                                )
                            )
                        } catch (t: Throwable) {
                            sendMessage(text.errCreateCampaign, update.message.chatId)
                            log.error("Campaign creating err.", t)
                        }

                        userStates[update.message.from.id]!!.state = NONE
                    }
                    ADD_ADMIN_TO_CAMPAIGN -> {
                        try {
                            val ids = update.message.text.split("\\s+".toRegex())
                            val adminId = ids[0].toInt()
                            val groupId = ids[1].toLong()

                            service.saveAdmin(
                                Admin(
                                    userId = adminId,
                                    createDate = now(),
                                    campaigns = setOf()
                                ),
                                groupId
                            )
                        } catch (t: Throwable) {
                            sendMessage(text.errAdminToCampaign, update.message.chatId)
                            log.error("AdminGroup creating err.", t)
                        }

                        userStates[update.message.from.id]!!.state = NONE
                    }
                    MSG_TO_USERS -> {
                        admin?.let { msgToUsers(admin, update) } ?: sendMessage(
                            text.msgNotAdmin,
                            update.message.chatId
                        )
                        userStates[update.message.from.id]!!.state = NONE
                    }
                    MSG_TO_CAMPAIGN -> {
                        admin?.let { msgToGroup(admin, update) } ?: sendMessage(
                            text.msgNotAdmin,
                            update.message.chatId
                        )
                        userStates[update.message.from.id]!!.state = NONE
                    }
                    else -> {
                        when (update.message.text) {
                            text.addAdminToCampaign -> {
                                sendMessage(text.msgAdminToCampaign, update.message.chatId)
                                userStates[update.message.from.id] =
                                    UserData(ADD_ADMIN_TO_CAMPAIGN, update.message.from)
                            }
                            text.addCreateCampaign -> {
                                sendMessage(text.msgCreateCampaign, update.message.chatId)
                                userStates[update.message.from.id] =
                                    UserData(CREATE_CAMPAIGN, update.message.from)
                            }
                            text.sendToEveryUser -> {
                                sendMessage(text.msgSendToEveryUser, update.message.chatId)
                                userStates[update.message.from.id] =
                                    UserData(MSG_TO_USERS, update.message.from)
                            }
                            text.sendToEveryGroup -> {
                                sendMessage(text.msgSendToEveryCampaign, update.message.chatId)
                                userStates[update.message.from.id] =
                                    UserData(MSG_TO_CAMPAIGN, update.message.from)
                            }
                            text.reset -> {
                                userStates.remove(update.message.from.id)
                                superMenu(update.message)
                            }
                            else -> {
                                superMenu(update.message)
                            }
                        }
                    }
                }

                // todo save admin to local storage
            } else if (admin != null) {
                when (userStates[admin.userId]?.state) {
                    MSG_TO_USERS -> {
                        msgToUsers(admin, update)
                        userStates[update.message.from.id]!!.state = NONE
                    }
                    MSG_TO_CAMPAIGN -> {
                        msgToGroup(admin, update)
                        userStates[update.message.from.id]!!.state = NONE
                    }
                    ADD_ADMIN_TO_CAMPAIGN -> {
                        try {
                            val ids = update.message.text.split("\\s+".toRegex())
                            val adminId = ids[0].toInt()
                            val groupId = ids[1].toLong()

                            service.saveAdmin(
                                Admin(
                                    userId = adminId,
                                    createDate = now(),
                                    campaigns = setOf()
                                ),
                                groupId
                            )
                        } catch (t: Throwable) {
                            sendMessage(text.errAdminToCampaign, update.message.chatId)
                            log.error("AdminGroup creating err.", t)
                        }

                        userStates[update.message.from.id]!!.state = NONE
                    }
                    else -> {
                        when (update.message.text) {
                            text.sendToEveryUser -> {
                                sendMessage(text.msgSendToEveryUser, update.message.chatId)
                                userStates[update.message.from.id] =
                                    UserData(MSG_TO_USERS, update.message.from)
                            }
                            text.sendToEveryGroup -> {
                                sendMessage(text.msgSendToEveryCampaign, update.message.chatId)
                                userStates[update.message.from.id] =
                                    UserData(MSG_TO_CAMPAIGN, update.message.from)
                            }
                            text.reset -> {
                                userStates.remove(update.message.from.id)
                                adminMenu(update.message)
                            }
                            else -> {
                                adminMenu(update.message)
                            }
                        }
                    }
                }

            } else {
                // todo save users to local storage
                val userChats = getAllUserChats(
                    service.getAllGroups().toList(),
                    update.message.from.id
                )

                if (userChats.isNotEmpty()) {
                    // todo save users to local storage
                    val user = service.createOrUpdateGroupUser(
                        UserInGroup(
                            update.message.from.id,
                            createDate = now()
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            log.error("error in onUpdateReceived() method", t)
            userStates.remove(update.message.from.id)
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
                    it.add(text.addAdminToCampaign)
                    it.add(text.addCreateCampaign)
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
        getUser(it.groupId, userId).status == "member"
    }

    private fun msgToGroup(admin: Admin, upd: Update) = admin.campaigns.forEach {
        execute(
            ForwardMessage(
                it.id,
                upd.message.chatId,
                upd.message.messageId
            )
        )
    }

    private fun msgToUsers(admin: Admin, upd: Update) {
        TODO()
    }
//        service.getAllUserIdInCampaigns(admin.campaigns).forEach {
//        execute(
//            ForwardMessage(
//                it.toLong(),
//                upd.message.chatId,
//                upd.message.messageId
//            )
//        )
//    }

    private fun getUser(chatId: Long, userId: Int) = execute(GetChatMember().setChatId(chatId).setUserId(userId))

    override fun getBotUsername(): String = botUsername

    override fun getBotToken(): String = botToken
}
