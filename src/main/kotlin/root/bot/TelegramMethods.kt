package root.bot

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import root.data.Text
import root.data.dao.SurveyDAO
import root.data.entity.Campaign
import root.data.entity.PassedSurvey
import root.data.entity.Survey
import root.libs.addElements
import root.libs.convertTime
import root.libs.resourceText
import java.util.ArrayList


fun msgUserInfo(passedSurveys: Iterable<PassedSurvey>, text: String) = SendMessage().also { msg ->
    val value = passedSurveys.map { survey -> survey.value }.sum()
    msg.text = resourceText(
        text,
        "account.value" to "$value",
        "pass.surveys" to passedSurveys.joinToString("\n") { "\n${it.value}\n${it.description}\n${convertTime(it.passDate)}" }
    )
}

fun msgAvailableCampaignsList(text: String, command: String, campaigns: Iterable<Campaign>) =
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

fun msgTaskList(text: String, command: String, surveys: Iterable<Survey>) = SendMessage().also { msg ->
    msg.text = text
    msg.replyMarkup = InlineKeyboardMarkup().also { markup ->

        markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
            surveys.forEach {
                keyboard.add(listOf(InlineKeyboardButton().setText(it.name).setCallbackData("$command ${it.id}")))
            }
        }
    }
}

fun msgQuestion(survey: SurveyDAO, command: String) = SendMessage().also { msg ->
    val question = survey.questions[survey.state]
    msg.text = question.text
    msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
        markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
            question.options.toList().sortedBy { it.sortPoints }.forEach {
                keyboard.add(listOf(InlineKeyboardButton().setText(it.text).setCallbackData("$command ${it.id}")))
            }
        }
    }
}

fun msgResetMenu(text: String, textReset: String) = SendMessage().also { msg ->
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


fun mainAdminsMenu(text: Text, textMsg: String = text.mainMenu) = SendMessage().also { msg ->
    msg.text = textMsg
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.addElements(KeyboardRow().also {
                it.add(text.mainMenuAdd)
                it.add(text.mainMenuDelete)
            }, KeyboardRow().also {
                it.add(text.mainMenuMessages)
                it.add(text.mainMenuStatistic)
            })
        }
    }
}

fun mainSuperAddMenu(text: Text, textMsg: String = text.addMenu) = SendMessage().also { msg ->
    msg.text = textMsg
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.addElements(KeyboardRow().also {
                it.add(text.addMenuCampaign)
                it.add(text.addMenuGroup)
                it.add(text.addMenuSurvey)
            }, KeyboardRow().also {
                it.add(text.addMenuSuperAdmin)
                it.add(text.addMenuAdmin)
                it.add(text.back)
            })
        }
    }
}

fun mainSuperDeleteMenu(text: Text, textMsg: String = text.deleteMenu) = SendMessage().also { msg ->
    msg.text = textMsg
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.addElements(KeyboardRow().also {
                it.add(text.deleteMenuCampaign)
                it.add(text.deleteMenuGroup)
                it.add(text.deleteMenuSurvey)
            }, KeyboardRow().also {
                it.add(text.deleteMenuSuperAdmin)
                it.add(text.deleteMenuAdmin)
                it.add(text.back)
            })
        }
    }
}

fun mainAdminAddMenu(text: Text, textMsg: String = text.addMenu) = SendMessage().also { msg ->
    msg.text = textMsg
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.addElements(KeyboardRow().also {
                it.add(text.addMenuMission)
                it.add(text.addMenuTask)
            }, KeyboardRow().also {
                it.add(text.addMenuAdmin)
                it.add(text.back)
            })
        }
    }
}

fun mainAdminDeleteMenu(text: Text, textMsg: String = text.deleteMenu) = SendMessage().also { msg ->
    msg.text = textMsg
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.addElements(KeyboardRow().also {
                it.add(text.deleteMenuMission)
                it.add(text.deleteMenuTask)
            }, KeyboardRow().also {
                it.add(text.deleteMenuAdmin)
                it.add(text.back)
            })
        }
    }
}

fun mainAdminMessageMenu(text: Text, textMsg: String = text.deleteMenu) = SendMessage().also { msg ->
    msg.text = textMsg
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.addElements(KeyboardRow().also {
                it.add(text.deleteMenuCampaign)
                it.add(text.deleteMenuGroup)
                it.add(text.deleteMenuSurvey)
            }, KeyboardRow().also {
                it.add(text.deleteMenuSuperAdmin)
                it.add(text.deleteMenuAdmin)
                it.add(text.back)
            })
        }
    }
}

fun mainAdminStatisticMenu(text: Text) = SendMessage().also { msg ->
    msg.text = text.mainMenu
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.addElements(KeyboardRow().also {
                it.add(text.sendCampaignsTable)
                it.add(text.sendUsersInCampaign)
            }, KeyboardRow().also {
                it.add(text.sendAdminsTable)
                it.add(text.sendSurveysTable)
            }, KeyboardRow().also {
                it.add(text.back)
            })
        }
    }
}

fun mainUsersMenu(text: Text, textMsg: String = text.userMainMenu) =
    SendMessage().also { msg ->
        msg.text = textMsg
        msg.enableMarkdown(true)
        msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
            markup.selective = true
            markup.resizeKeyboard = true
            markup.oneTimeKeyboard = false
            markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
                keyboard.addElements(KeyboardRow().also {
                    it.add(text.joinToCampaign)
                    it.add(text.msgUserInfo)
                }, KeyboardRow().also {
                    it.add(text.showUserCampaigns)
                })
            }
        }
    }