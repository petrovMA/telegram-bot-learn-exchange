package root.bot

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import root.data.Text
import root.data.dao.SurveyDAO
import java.util.ArrayList
import org.apache.log4j.Logger
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import root.data.UserState
import root.data.UserState.*
import root.data.entity.*
import root.data.entity.tasks.PassedTask
import root.data.entity.tasks.Task
import root.data.entity.tasks.surveus.Option
import root.data.entity.tasks.surveus.Question
import root.data.entity.tasks.surveus.Survey
import root.libs.*

class TelegramMethods

private val log = Logger.getLogger(TelegramMethods::class.java)

fun msgUserInfo(passedSurveys: Iterable<PassedTask>, text: String) = SendMessage().also { msg ->
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

fun msgTaskList(text: String, command: String, surveys: Iterable<Task>) = SendMessage().apply {
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

fun mainUsersMenu(text: Text, textMsg: String = text.userMainMenu) = SendMessage().also { msg ->
    msg.text = textMsg
    msg.enableMarkdown(true)
    msg.replyMarkup = ReplyKeyboardMarkup().also { markup ->
        markup.selective = true
        markup.resizeKeyboard = true
        markup.oneTimeKeyboard = true
        markup.keyboard = ArrayList<KeyboardRow>().also { keyboard ->
            keyboard.addElements(KeyboardRow().also {
                it.add(text.userMainMenuCampaigns)
                it.add(text.userMainMenuStatus)
            }, KeyboardRow().also {
                it.add(text.userMainMenuAccount)
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
                add(listOf(InlineKeyboardButton().setText(text.joinToCampaign).setCallbackData("$JOIN_TO_CAMPAIGN")))
                add(listOf(InlineKeyboardButton().setText(text.reset).setCallbackData("$RESET")))
            }
        }
    }

fun userJoinToCampaigns(text: Text, campaigns: Iterable<Campaign>, textMsg: String = text.userAvailableCampaigns) =
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
                                    .setCallbackData("$JOIN_TO_CAMPAIGN_MENU ${it.id}")
                            )
                        )
                    else
                        log.warn("Common campaign found:\n$it")
                }
                add(listOf(InlineKeyboardButton().setText(text.back).setCallbackData("$JOIN_TO_CAMPAIGN_BACK")))
            }
        }
    }

fun mainAdminStatisticMenu(text: Text) = SendMessage().also { msg ->
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
                it.add(text.sendUsersInCampaign)
            }, KeyboardRow().also {
                it.add(text.sendAdminsTable)
                it.add(text.sendSurveysTable)
                it.add(text.back)
            })
        }
    }
}

fun userStatusMenu(text: Text, surveys: Iterable<PassedTask>) = SendMessage().also { msg ->
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

fun editMessage(old: Message, new: SendMessage) = EditMessageText().also { msg ->
    msg.chatId = old.chatId.toString()
    msg.messageId = old.messageId
    msg.text = new.text
    msg.replyMarkup = new.replyMarkup as InlineKeyboardMarkup
}

fun editSurvey(text: Text, survey: Survey, upd: Update) = EditMessageText().also { msg ->
    msg.chatId = fromId(upd).toString()
    msg.messageId = upd.callbackQuery.message.messageId
    msg.text = "$survey\n${printQuestions(survey.questions)}"
    msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
        markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
            keyboard.addElements(
                listOf(
                    InlineKeyboardButton().setText(text.editQuestions)
                        .setCallbackData("$SURVEY_QUESTIONS")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.editSurveyName)
                        .setCallbackData("$SURVEY_NAME")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.editSurveyDescription)
                        .setCallbackData("$SURVEY_DESCRIPTION")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.saveSurvey)
                        .setCallbackData("$SURVEY_SAVE")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.surveyDelete)
                        .setCallbackData("$SURVEY_DELETE ${survey.id}")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.backSurvey)
                        .setCallbackData("$SURVEY_BACK")
                )
            )
        }
    }
}

fun editQuestion(text: Text, question: Question, upd: Update) = EditMessageText().also { msg ->
    msg.chatId = fromId(upd).toString()
    msg.messageId = upd.callbackQuery.message.messageId
    msg.text = "$question\n${printOptions(question.options)}"
    msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
        markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
            keyboard.addElements(
                listOf(
                    InlineKeyboardButton().setText(text.surveyQuestionEditText)
                        .setCallbackData("$SURVEY_QUESTION_EDIT_TEXT")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.surveyQuestionEditSort)
                        .setCallbackData("$SURVEY_QUESTION_EDIT_SORT")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.surveyQuestionEditOptions)
                        .setCallbackData("$SURVEY_OPTIONS")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.surveyQuestionDelete)
                        .setCallbackData("$SURVEY_QUESTION_DELETE")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.surveyQuestionBack)
                        .setCallbackData("$SURVEY_QUESTIONS")
                )
            )
        }
    }
}

fun editOption(text: Text, option: Option, upd: Update) = EditMessageText().also { msg ->
    msg.chatId = fromId(upd).toString()
    msg.messageId = upd.callbackQuery.message.messageId
    msg.text = printOptions(setOf(option))
    msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
        markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
            keyboard.addElements(
                listOf(
                    InlineKeyboardButton().setText(text.surveyOptionEditText)
                        .setCallbackData("$SURVEY_OPTION_EDIT_TEXT")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.surveyOptionEditSort)
                        .setCallbackData("$SURVEY_OPTION_EDIT_SORT")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.surveyOptionEditValue)
                        .setCallbackData("$SURVEY_OPTION_EDIT_CORRECT")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.surveyOptionDelete)
                        .setCallbackData("$SURVEY_OPTION_DELETE ${option.text.hashCode()}")
                ),
                listOf(
                    InlineKeyboardButton().setText(text.surveyOptionBack)
                        .setCallbackData("$SURVEY_OPTIONS")
                )
            )
        }
    }
}

fun enterText(message: Message, text: String, textBack: String, stateBack: UserState) = EditMessageText().also { msg ->
    msg.chatId = message.chatId.toString()
    msg.messageId = message.messageId
    msg.text = text
    msg.replyMarkup = InlineKeyboardMarkup().also { markup ->
        markup.keyboard = ArrayList<List<InlineKeyboardButton>>().also { keyboard ->
            keyboard.addElements(
                listOf(
                    InlineKeyboardButton().setText(textBack)
                        .setCallbackData("$stateBack")
                )
            )
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

fun userAccountRegister(text: Text) = SendMessage().also { msg ->
    msg.text = text.msgEnterYourEmail
    msg.enableMarkdown(true)
    msg.replyMarkup = InlineKeyboardMarkup().apply {
        keyboard = ArrayList<List<InlineKeyboardButton>>().apply {
            add(listOf(InlineKeyboardButton().setText(text.btnUserMainMenuAccountRegistration).setUrl(text.btnUserMainMenuAccountRegistrationUrl)))
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

fun fromId(upd: Update): Int = upd.message?.from?.id ?: upd.callbackQuery!!.from!!.id
fun chatId(upd: Update): Long = upd.message?.chatId ?: upd.callbackQuery!!.message!!.chatId
fun message(upd: Update) = upd.message ?: upd.callbackQuery!!.message!!

fun fixSurvey(survey: Survey) = survey.apply {
    questions.forEach { it.options.forEach { opt -> opt.value = 0 } }
    questions.last().options.forEach { opt -> if (opt.correct) opt.value = 1000 }
}