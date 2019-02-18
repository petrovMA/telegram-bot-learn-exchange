package tasks.types

import bot.TelegramBot
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import tasks.Task
import java.io.File
import java.io.IOException
import java.util.ArrayList

class TaskInquirer(conf: Config):Task(conf) {
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
                .getConfigList("task.settings.questions")
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
            message.text = conf.getConfigList("task.settings.questions").first().getString("text")

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
                .getConfigList("task.settings.questions")
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
            message.text = conf.getConfigList("task.settings.questions").first().getString("text")

            message
        } catch (e: Exception) {
            log.error(e.message, e)
            throw e
        }
}