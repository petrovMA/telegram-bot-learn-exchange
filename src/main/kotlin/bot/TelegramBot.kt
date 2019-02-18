package bot

import com.typesafe.config.Config
import notificator.libs.readConf
import tasks.types.*
import org.apache.log4j.Logger
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.util.ArrayList
import java.util.HashMap

class TelegramBot : TelegramLongPollingBot {

    private val userStates: HashMap<Long, UserData> = HashMap()

    companion object {
        private val log = Logger.getLogger(TelegramBot::class.java)!!
    }

    private val botUsername: String
    private val botToken: String
    private val tasks: Map<String, String>
    private val text:Text


    constructor(
        botUsername: String,
        botToken: String,
        tasks: Map<String, String>,
        text: Text,
        options: DefaultBotOptions?
    ) : super(options) {
        this.botUsername = botUsername
        this.botToken = botToken
        this.tasks = tasks
        this.text = text
    }

    constructor(
        botUsername: String,
        botToken: String,
        tasks: Map<String, String>,
        text: Text
    ) : super() {
        this.botUsername = botUsername
        this.botToken = botToken
        this.tasks = tasks
        this.text = text
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

        } else if (update.message.text == text.showTasksList) {
            showTaskList(update)
        } else {
            mainMenu(update.message)
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
            message.text = "tasks"

            sendMessage(message, upd.message.chatId)
        } catch (e: Exception) {
            log.error(e.message, e)
        }
    }

    private fun mainMenu(message: Message) {
//        userStates[message.chatId] = UserData(
//            BEGIN,
//            Lottery(),
//            message.from
//        )
        sendMessage(SendMessage().also { msg ->
            msg.text = "text.mainMenu"
            msg.enableMarkdown(true)
            msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
                markup.selective = true
                markup.resizeKeyboard = true
                markup.oneTimeKeyboard = false
                markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
                    keyboard.add(KeyboardRow().also {
                        it.add("text.tasks")
                    })

                }
            }
        }, message.chatId)
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

    override fun getBotUsername(): String = botUsername

    override fun getBotToken(): String = botToken
}
