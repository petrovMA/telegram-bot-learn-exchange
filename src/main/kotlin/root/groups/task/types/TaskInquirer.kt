package root.groups.task.types

import com.typesafe.config.Config
import org.apache.log4j.Logger
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import root.groups.Task
import java.util.ArrayList

class TaskInquirer(conf: Config): Task(conf) {
    private val log = Logger.getLogger(TaskInquirer::class.java)
    init {
        
    }


    fun showQuestion(chatId: Long) =
        try {
            val message = SendMessage()

            val markupInline = InlineKeyboardMarkup()
            val rowsInline = ArrayList<List<InlineKeyboardButton>>()
            val rowInline = ArrayList<InlineKeyboardButton>()

            conf
                .getConfigList("tasks.settings.questions")
                .first()
                .getConfigList("answer-options")
                .forEach {
                    rowInline.add(
                        InlineKeyboardButton()
                            .setText(it.getString("option"))
                            .setCallbackData("option")
                    )
                }

            // Set the keyboard to the markup
            rowsInline.add(rowInline)
            // Add it to the message
            markupInline.keyboard = rowsInline
            message.replyMarkup = markupInline
            message.text = conf.getConfigList("tasks.settings.questions").first().getString("text")

            message
        } catch (e: Exception) {
            log.error(e.message, e)
            throw e
        }


    private fun updateMessage(chatId: Long) =
        try {
            val message = SendMessage()

            val markupInline = InlineKeyboardMarkup()
            val rowsInline = ArrayList<List<InlineKeyboardButton>>()
            val rowInline = ArrayList<InlineKeyboardButton>()

            conf
                .getConfigList("tasks.settings.questions")
                .first()
                .getConfigList("answer-options")
                .forEach {
                    rowInline.add(
                        InlineKeyboardButton()
                            .setText(it.getString("option"))
                            .setCallbackData("option")
                    )
                }

            // Set the keyboard to the markup
            rowsInline.add(rowInline)
            // Add it to the message
            markupInline.keyboard = rowsInline
            message.replyMarkup = markupInline
            message.text = conf.getConfigList("tasks.settings.questions").first().getString("text")

            message
        } catch (e: Exception) {
            log.error(e.message, e)
            throw e
        }
}