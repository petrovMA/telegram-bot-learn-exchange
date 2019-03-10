package root.bot

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import root.data.entity.Campaign
import root.data.entity.PassedSurvey
import root.data.entity.Survey
import root.libs.resourceText
import java.util.ArrayList


fun sendUserInfo(passedSurveys: Iterable<PassedSurvey>, text: String) = SendMessage().also { msg ->
    val value = passedSurveys.map { survey -> survey.value }.sum()
    msg.text = resourceText(
        text,
        "account.value" to "$value",
        "pass.surveys" to passedSurveys.toString()
    )
}

fun sendAvailableCampaignsList(text: String, command: String, campaigns: Iterable<Campaign>) =
    SendMessage().also { msg ->
        msg.text = text
        msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
            markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
                campaigns.forEach {
                    keyboard.add(listOf(InlineKeyboardButton().setText(it.name).setCallbackData("$command ${it.id}")))
                }
            }
        }
    }

fun showTaskList(text: String, command: String, surveys: Iterable<Survey>) = SendMessage().also { msg ->
    msg.text = text
    msg.replyMarkup = InlineKeyboardMarkup().also { markup ->

        markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
            surveys.forEach {
                keyboard.add(listOf(InlineKeyboardButton().setText(it.name).setCallbackData("$command ${it.id}")))
            }
        }
    }
}

fun resetMenu(text: String, textReset: String) = SendMessage().also { msg ->
    msg.text = text
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.add(KeyboardRow().also {
                it.add(textReset)
            })
        }
    }
}