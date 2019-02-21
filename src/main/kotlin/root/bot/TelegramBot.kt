package root.bot

import notificator.libs.readConf
import org.apache.log4j.Logger
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
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
import root.data.UserState
import root.data.entity.Admin
import root.data.entity.Group
import root.data.entity.GroupUser
//import root.data.entity.User
import root.groups.task.types.TaskInquirer
import root.service.AdminService
import java.time.OffsetDateTime.now
import java.util.ArrayList
import java.util.HashMap

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
        try {
            if (update.hasCallbackQuery()) {
                readConf(tasks[update.callbackQuery.data])?.run {
                    val type = this.getString("type")
                    when (type) {
                        "inquirer" -> {
                            val inquirer = TaskInquirer(this)
                            inquirer.showQuestion(update.callbackQuery.message.chatId)

                        }
                        else -> {
                            sendMessage(text.taskNotFound, update.callbackQuery.message.chatId)
                        }
                    }
                }
                    ?: sendMessage(text.taskNotFound, update.callbackQuery.message.chatId)

            } else if (superUsers.contains(SuperUser(update.message.from.id, update.message.from.userName))) {
                when (userStates[update.message.from.id]?.state) {
//                    UserState.ADD_ADMIN -> {
//
//                        userStates[update.message.from.id]!!.state = UserState.NONE
//                    }
//                    UserState.ADD_GROUP -> {
//
//                        userStates[update.message.from.id]!!.state = UserState.NONE
//                    }
                    UserState.ADD_ADMIN_TO_GROUP -> {
                        try {
                            val ids = update.message.text.split("\\s+".toRegex())
                            val adminId = ids[0].toInt()
                            val groupId = ids[1].toLong()

                            service.saveAdmin(
                                Admin(
                                    userId = adminId,
                                    createDate = now(),
                                    groups = listOf()
                                ),
                                groupId
                            )
                        } catch (t: Throwable) {
                            sendMessage(text.errAdminToGroup, update.message.chatId)
                            log.error("AdminGroup creating err.", t)
                        }

                        userStates[update.message.from.id]!!.state = UserState.NONE
                    }
                    else -> {
                        when (update.message.text) {
//                            text.addAdmin -> {
//                                sendMessage(text.msgAddAdmin, update.message.chatId)
//                                userStates[update.message.from.id]!!.state = UserState.ADD_ADMIN
//                            }
//                            text.addGroup -> {
//                                sendMessage(text.msgAddGroup, update.message.chatId)
//                                userStates[update.message.from.id]!!.state = UserState.ADD_GROUP
//                            }
                            text.addAdminToGroup -> {
                                sendMessage(text.msgAdminToGroup, update.message.chatId)
                                userStates[update.message.from.id] =
                                    UserData(UserState.ADD_ADMIN_TO_GROUP, update.message.from)
                            }
                            else -> {
                                mainMenu(update.message)
                            }
                        }
                    }
                }

//                execute(ForwardMessage(update.message.chatId, update.message.chatId, update.message.messageId))
            } else {
                // todo save users to local storage
                val userChats = getAllUserChats(
                    service.getAllGroups(),
                    update.message.from.id
                )

                if(userChats.isNotEmpty()) {
                    // todo save users to local storage
                    val user = service.createOrUpdateGroupUser(
                        GroupUser(
                            update.message.from.id,
                            createDate = now(),
                            groups = userChats
                        )
                    )
                }

            }
        } catch (t: Throwable) {
            t.printStackTrace()
            log.error(t)
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

    private fun mainMenu(message: Message) = sendMessage(SendMessage().also { msg ->
        msg.text = text.mainMenu
        msg.enableMarkdown(true)
        msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
            markup.selective = true
            markup.resizeKeyboard = true
            markup.oneTimeKeyboard = false
            markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
                //                keyboard.add(KeyboardRow().also {
//                    it.add(text.addAdmin)
//                    it.add(text.addGroup)
//                })
                keyboard.add(KeyboardRow().also {
                    it.add(text.addAdminToGroup)
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

    private fun getUser(chatId: Long, userId: Int) = execute(GetChatMember().setChatId(chatId).setUserId(userId))

    override fun getBotUsername(): String = botUsername

    override fun getBotToken(): String = botToken
}
