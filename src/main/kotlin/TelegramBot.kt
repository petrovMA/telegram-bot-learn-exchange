import org.apache.log4j.Logger
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

class TelegramBot : TelegramLongPollingBot {

    companion object {
        private val log = Logger.getLogger(TelegramBot::class.java)!!
    }

    private val chatId: Long
    private val botUsername: String
    private val botToken: String


    constructor(chatId: Long, botUsername: String, botToken: String, options: DefaultBotOptions?) : super(options) {
        this.chatId = chatId
        this.botUsername = botUsername
        this.botToken = botToken
    }

    override fun onUpdateReceived(update: Update) {
        log.info(update)
        sendMessage(update.message.text)
    }

    override fun getBotUsername(): String = botUsername

    override fun getBotToken(): String = botToken

    fun sendMessage(messageText: String, chatId: Long = this.chatId) {
        try {
            val message = SendMessage().setChatId(chatId)
            log.debug("Send to chatId = $chatId\nMessage: \"$messageText\"")
            message.text = messageText
            execute(message)
        } catch (e: Exception) {
            log.warn(e.message, e)
        }
    }
}
