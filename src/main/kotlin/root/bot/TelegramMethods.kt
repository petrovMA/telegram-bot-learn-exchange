package root.bot

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import root.data.entity.PassedSurvey
import root.libs.resourceText
import java.util.ArrayList


fun sendUserInfo(passedSurveys: Iterable<PassedSurvey>, text: String, value: Int) = SendMessage().also {
    it.text = resourceText(
        text,
        "account.value" to "$value",
        "pass.surveys" to passedSurveys.toString()
    )
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