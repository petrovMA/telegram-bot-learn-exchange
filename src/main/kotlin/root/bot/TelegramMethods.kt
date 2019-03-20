package root.bot

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
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
import org.apache.log4j.Logger
import root.data.UserState
import root.data.UserState.*

class TelegramMethods

private val log = Logger.getLogger(TelegramMethods::class.java)

fun msgUserInfo(passedSurveys: Iterable<PassedSurvey>, text: String) = SendMessage().also { msg ->
    val value = passedSurveys.map { survey -> survey.value }.sum()
    msg.text = resourceText(
        text,
        "account.value" to "$value",
        "pass.surveys" to passedSurveys.joinToString("\n") { "\n${it.value}\n${it.description}\n${convertTime(it.passDate)}" }
    )
}

fun msgAvailableCampaignsListDivideCommon(
    text: String,
    command: String,
    campaigns: Iterable<Campaign>
) = SendMessage().apply {
    this.text = text
    replyMarkup = InlineKeyboardMarkup().apply {
        keyboard = ArrayList<List<InlineKeyboardButton>>().apply {
            campaigns.forEach {
                if (!it.common)
                    add(listOf(InlineKeyboardButton().setText(it.name).setCallbackData("$command ${it.id}")))
                else
                    add(listOf(InlineKeyboardButton().setText("${it.name} [common]").setCallbackData("$command ${it.id} common")))
            }
        }
    }
}

fun msgAvailableCampaignsList(
    text: String,
    command: String,
    campaigns: Iterable<Campaign>
) = SendMessage().apply {
    this.text = text
    replyMarkup = InlineKeyboardMarkup().apply {
        keyboard = ArrayList<List<InlineKeyboardButton>>().apply {
            campaigns.forEach {
                add(listOf(InlineKeyboardButton().setText(it.name).setCallbackData("$command ${it.id}")))
            }
        }
    }
}

fun msgTaskList(text: String, command: String, surveys: Iterable<Survey>) = SendMessage().apply {
    this.text = text
    replyMarkup = InlineKeyboardMarkup().apply {
        keyboard = ArrayList<List<InlineKeyboardButton>>().apply {
            surveys.forEach {
                add(listOf(InlineKeyboardButton().setText(it.name).setCallbackData("$command ${it.id}")))
            }
        }
    }
}

fun msgQuestion(text: Text, survey: SurveyDAO, command: String) = SendMessage().also { msg ->
    val question = survey.questions[survey.state]
    msg.text = question.text
    msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
        markup.keyboard = ArrayList<List<InlineKeyboardButton>>().apply {
            question.options.toList().sortedBy { it.sortPoints }.forEach {
                add(listOf(InlineKeyboardButton().setText(it.text).setCallbackData("$command ${it.id}")))
            }
            add(listOf(InlineKeyboardButton().setText(text.reset).setCallbackData("$RESET")))
        }
    }
}

fun msgBackMenu(msgText: String, textBack: String) = SendMessage().also { msg ->
    msg.text = msgText
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.add(KeyboardRow().also {
                it.add(textBack)
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

fun mainUsersMenu(text: Text, textMsg: String = text.userMainMenu) = SendMessage().also { msg ->
    msg.text = textMsg
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.addElements(KeyboardRow().also {
                it.add(text.userMainMenuCampaigns)
                it.add(text.userMainMenuStatus)
            }, KeyboardRow().also {
                it.add(text.userMainMenuAccount)
                it.add(text.joinToCampaign)
            })
        }
    }
}

fun userCampaignsMenu(text: Text, campaigns: Iterable<Campaign>, textMsg: String = text.msgUserMainMenuCampaigns) =
    SendMessage().also { msg ->
        msg.text = textMsg
        msg.enableMarkdown(true)
        msg.replyMarkup = InlineKeyboardMarkup().apply {
            keyboard = ArrayList<List<InlineKeyboardButton>>().apply {
                campaigns.forEach {
                    if (!it.common)
                        add(
                            listOf(
                                InlineKeyboardButton()
                                    .setText(it.name)
                                    .setCallbackData("$USER_MENU_ACTIVE_CAMPAIGN_SELECT ${it.id}")
                            )
                        )
                    else
                        log.warn("Common campaign found:\n$it")
                }
                add(
                    listOf(
                        InlineKeyboardButton()
                            .setText(text.msgUserMainMenuCommonCampaignTasks)
                            .setCallbackData("$USER_MENU_ACTIVE_COMMON_CAMPAIGN_SELECT")
                    )
                )
                add(listOf(InlineKeyboardButton().setText(text.reset).setCallbackData("$RESET")))
            }
        }
    }

fun sendTableSuperAdmin(text: Text) = SendMessage().also { msg ->
    msg.text = text.msgGetStatisticTables
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = false
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.addElements(KeyboardRow().also {
                it.add(text.sendCampaignsTable)
                it.add(text.sendSuperAdminTable)
            }, KeyboardRow().also {
                it.add(text.sendUsersInCampaign)
                it.add(text.sendAdminsTable)
                it.add(text.sendSurveysTable)
            }, KeyboardRow().also {
                it.add(text.reset)
            })
        }
    }
}

fun userStatusMenu(text: Text, surveys: Iterable<PassedSurvey>) = SendMessage().also { msg ->
    var value = 0
    var passedCamps = 0
    var passedTasks = 0
    var awardsCount = 0
    var level = 0
    var refferals = 0
    var awardList = emptyList<String>()
    var passedMissions = surveys.count()
    surveys.forEach {
        value += it.value
    }

    msg.text = resourceText(
        text.msgUserMainMenuStatus,
        "user.passed.camp" to "$passedCamps",
        "user.passed.missions" to "$passedMissions",
        "user.passed.tasks" to "$passedTasks",
        "user.value" to "$value",
        "user.awards.count" to "$awardsCount",
        "user.awards" to awardList.joinToString(),
        "user.level" to "$level",
        "user.refferals" to "$refferals"
    )

    msg.enableMarkdown(true)
    msg.replyMarkup = InlineKeyboardMarkup().apply {
        keyboard = ArrayList<List<InlineKeyboardButton>>().apply {
            add(listOf(InlineKeyboardButton().setText(text.btnUserMainMenuStatus).setCallbackData("$RESET")))
        }
    }
}

fun userAccountMenu(text: Text) = SendMessage().also { msg ->
    msg.text = text.msgUserMainMenuAccount
    msg.enableMarkdown(true)
    msg.replyMarkup = InlineKeyboardMarkup().apply {
        keyboard = ArrayList<List<InlineKeyboardButton>>().apply {
            add(listOf(InlineKeyboardButton().setText(text.btnUserMainMenuAccountRegistration).setUrl(text.btnUserMainMenuAccountRegistrationUrl)))
            add(listOf(InlineKeyboardButton().setText(text.btnUserMainMenuAccountFriends).setCallbackData("$RESET")))
        }
    }
}

fun createKeyboard(buttons: List<KeyboardButton>, buttonsCountInRow: Int = 2) = ArrayList<KeyboardRow>().apply {
    add(KeyboardRow())
    buttons.forEach {
        if (last().size < buttonsCountInRow) last().add(it)
        else add(KeyboardRow().apply { add(it) })
    }
}

fun fixSurvey(survey: Survey) = survey.apply {
    questions.forEach { it.options.forEach { opt -> opt.value = 0 } }
    questions.last().options.forEach { opt -> if (opt.correct) opt.value = 1000 }
}