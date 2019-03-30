package root.bot.commands

import org.apache.log4j.Logger
import org.springframework.dao.DataIntegrityViolationException
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import root.bot.*
import root.data.Text
import root.data.UserData
import root.data.UserState.*
import root.data.entity.*
import root.data.entity.tasks.surveus.Option
import root.data.entity.tasks.surveus.Question
import root.data.entity.tasks.surveus.Survey
import root.libs.*
import root.service.Service
import java.time.OffsetDateTime.now

class SuperAdminUpdate

private val log = Logger.getLogger(SuperAdminUpdate::class.java)

fun UserData.doSuperAdminUpdate(
    upd: Update,
    text: Text,
    service: Service,
    send: (method: SendMessage) -> Any?,
    editMessage: (message: EditMessageText) -> Any?,
    sendTable: (chatId: Long, excelEntities: Iterable<ExcelEntity>, entityName: String) -> Any?
): Unit {
    val actionBack: () -> Unit = {
        when (state) {
            MAIN_MENU_ADD_MISSION, MAIN_MENU_ADD_TASK, MAIN_MENU_ADD_ADMIN, MAIN_MENU_ADD_CAMPAIGN,
            MAIN_MENU_ADD_COMMON_CAMPAIGN, MAIN_MENU_ADD_GROUP,
            CAMPAIGN_FOR_SURVEY -> {
                state = MAIN_MENU_ADD
                user = message(upd).from
                send.invoke(mainAdminAddMenu(text).apply {
                    chatId = chatId(upd).toString()
                    replyMarkup = (replyMarkup as ReplyKeyboardMarkup).apply {
                        keyboard = createKeyboard(keyboard.flatten().toArrayList().apply {
                            addElements(
                                0,
                                KeyboardButton(text.addMenuCampaign),
                                KeyboardButton(text.addMenuCommonCampaign),
                                KeyboardButton(text.addMenuGroup)
                            )
                        })
                    }
                })
            }
            MAIN_MENU_DELETE_MISSION, MAIN_MENU_DELETE_TASK, MAIN_MENU_DELETE_ADMIN, MAIN_MENU_DELETE_CAMPAIGN,
            MAIN_MENU_DELETE_COMMON_CAMPAIGN, MAIN_MENU_DELETE_GROUP -> {
                state = MAIN_MENU_DELETE
                user = message(upd).from
                send.invoke(mainAdminDeleteMenu(text).apply {
                    chatId = chatId(upd).toString()
                    replyMarkup = (replyMarkup as ReplyKeyboardMarkup).apply {
                        keyboard = createKeyboard(keyboard.flatten().toArrayList().apply {
                            addElements(
                                0,
                                KeyboardButton(text.deleteMenuCampaign),
                                KeyboardButton(text.deleteMenuCommonCampaign),
                                KeyboardButton(text.deleteMenuGroup)
                            )
                        })
                    }
                })
            }
            else -> {
                state = NONE
                send.invoke(mainAdminsMenu(text).apply { chatId = chatId(upd).toString() })
            }
        }
    }

    when (state) {
        MAIN_MENU_ADD -> {
            when (message(upd).text) {
                text.addMenuCampaign -> {
                    state = MAIN_MENU_ADD_CAMPAIGN
                    user = message(upd).from
                    send.invoke(msgBackMenu(text.msgCreateCampaign, text.back).apply {
                        chatId = chatId(upd).toString()
                    })
                }
                text.addMenuCommonCampaign -> {
                    state = MAIN_MENU_ADD_COMMON_CAMPAIGN
                    user = message(upd).from
                    send.invoke(msgBackMenu(text.msgCreateCommonCampaign, text.back).apply {
                        chatId = chatId(upd).toString()
                    })
                }
                text.addMenuGroup -> {
                    state = MAIN_MENU_ADD_GROUP
                    user = message(upd).from
                    send.invoke(msgAvailableCampaignsListDivideCommon(
                        text.msgGroupToCampaignSelectCamp,
                        MAIN_MENU_ADD_GROUP.toString(),
                        service.getAllCampaigns()
                    ).apply { chatId = chatId(upd).toString() })
                }
                text.addMenuMission -> {
                    // todo refactor it state = MAIN_MENU_ADD_MISSION
                    //  user = message(upd).from)
                    // send.invoke(msgBackMenu(text.msgSurvey, text.back).apply { chatId = chatId(upd).toString() })

                    val availableCampaigns = service.getAllCampaigns().toList()

                    if (availableCampaigns.isNotEmpty()) {
                        send.invoke(msgAvailableCampaignsListDivideCommon(
                            text.adminAvailableCampaignsSurveys,
                            CAMPAIGN_FOR_SURVEY.toString(),
                            availableCampaigns
                        ).apply { chatId = chatId(upd).toString() })
                        state = CAMPAIGN_FOR_SURVEY
                        user = message(upd).from
                    } else {
                        send.invoke(mainAdminsMenu(text, text.msgNoCampaign).apply { chatId = chatId(upd).toString() })
                        state = NONE
                    }
                }
                text.addMenuTask -> {
                    TODO("MAIN_MENU_ADD_TASK")
                    state = MAIN_MENU_ADD_TASK
                    user = message(upd).from
                }
                text.addMenuAdmin -> {
                    send.invoke(msgAvailableCampaignsListDivideCommon(
                        text.msgAdminToCampaignSelectCamp,
                        MAIN_MENU_ADD_ADMIN.toString(),
                        service.getAllCampaigns()
                    ).apply { chatId = chatId(upd).toString() })
                }
                text.back -> actionBack.invoke()
            }
        }
        MAIN_MENU_DELETE -> {
            when (message(upd).text) {
                text.deleteMenuCampaign -> {
                    state = MAIN_MENU_DELETE_CAMPAIGN
                    user = message(upd).from
                    send.invoke(msgBackMenu(text.msgRemoveCampaign, text.back).apply {
                        chatId = chatId(upd).toString()
                    })
                }
                text.deleteMenuCommonCampaign -> {
                    state = MAIN_MENU_DELETE_COMMON_CAMPAIGN
                    user = message(upd).from
                    send.invoke(msgBackMenu(text.msgRemoveCommonCampaign, text.back).apply {
                        chatId = chatId(upd).toString()
                    })
                }
                text.deleteMenuGroup -> {
                    state = MAIN_MENU_DELETE_GROUP
                    user = message(upd).from
                    send.invoke(msgAvailableCampaignsListDivideCommon(
                        text.msgRemoveGroupFromCampaign,
                        MAIN_MENU_DELETE_GROUP.toString(),
                        service.getAllCampaigns()
                    ).apply { chatId = chatId(upd).toString() })
                }
                text.deleteMenuMission -> {
                    TODO("MAIN_MENU_DELETE_MISSION")
                    state = MAIN_MENU_DELETE_MISSION
                    user = message(upd).from
                }
                text.deleteMenuTask -> {
                    TODO("MAIN_MENU_DELETE_TASK")
                    state = MAIN_MENU_DELETE_TASK
                    user = message(upd).from
                }
                text.deleteMenuAdmin -> {
                    state = MAIN_MENU_DELETE_ADMIN
                    user = message(upd).from
                    send.invoke(msgAvailableCampaignsListDivideCommon(
                        text.msgRemoveAdminFromCampaign,
                        MAIN_MENU_DELETE_ADMIN.toString(),
                        service.getAllCampaigns()
                    ).apply { chatId = chatId(upd).toString() })
                }
                text.back -> actionBack.invoke()
            }
        }
        MAIN_MENU_ADD_COMMON_CAMPAIGN -> {
            when (message(upd).text) {
                text.back -> actionBack.invoke()
                else -> try {
                    val newCampName = message(upd).text

                    service.createCampaign(
                        Campaign(
                            name = newCampName,
                            createDate = now(),
                            common = true,
                            groups = emptySet()
                        )
                    )

                    send.invoke(SendMessage().apply {
                        this.text = text.sucCreateCommonCampaign
                        chatId = chatId(upd).toString()
                    })
                } catch (e: DataIntegrityViolationException) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errCreateCommonCampaignAlreadyExist
                        chatId = chatId(upd).toString()
                    })
                    log.error("Campaign creating err.", e)
                } catch (t: Throwable) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errCreateCommonCampaign
                        chatId = chatId(upd).toString()
                    })
                    log.error("Campaign creating err.", t)
                }
            }
        }
        MAIN_MENU_DELETE_COMMON_CAMPAIGN -> {
            when (message(upd).text) {
                text.back -> actionBack.invoke()
                else -> try {
                    val newCampName = message(upd).text

                    service.deleteCampaignByName(newCampName)

                    send.invoke(SendMessage().apply {
                        this.text = text.sucRemoveCommonCampaign
                        chatId = chatId(upd).toString()
                    })
                } catch (t: Throwable) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errRemoveCommonCampaign
                        chatId = chatId(upd).toString()
                    })
                    log.error("Campaign remove err.", t)
                }
            }
        }
        MAIN_MENU_ADD_CAMPAIGN -> {
            when (message(upd).text) {
                text.back -> actionBack.invoke()
                else -> try {
                    val newCampName = message(upd).text

                    service.createCampaign(Campaign(name = newCampName, createDate = now(), groups = emptySet()))

                    send.invoke(SendMessage().apply {
                        this.text = text.sucCreateCampaign
                        chatId = chatId(upd).toString()
                    })
                } catch (e: DataIntegrityViolationException) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errCreateCampaignAlreadyExist
                        chatId = chatId(upd).toString()
                    })
                    log.error("Campaign creating err.", e)
                } catch (t: Throwable) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errCreateCampaign
                        chatId = chatId(upd).toString()
                    })
                    log.error("Campaign creating err.", t)
                }
            }
        }
        MAIN_MENU_DELETE_CAMPAIGN -> {
            when (message(upd).text) {
                text.back -> actionBack.invoke()
                else -> try {
                    val newCampName = message(upd).text

                    service.deleteCampaignByName(newCampName)

                    send.invoke(SendMessage().apply {
                        this.text = text.sucRemoveCampaign
                        chatId = chatId(upd).toString()
                    })
                } catch (t: Throwable) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errRemoveCampaign
                        chatId = chatId(upd).toString()
                    })
                    log.error("Campaign remove err.", t)
                }
            }
        }
        MAIN_MENU_ADD_GROUP -> {
            when (message(upd).text) {
                text.back -> actionBack.invoke()
                else -> try {
                    val params = message(upd).text.split("\\s+".toRegex())
                    val groupId = params[0].toLong()
                    val camp = campaign!!

                    val userId = fromId(upd)

                    val (addedGroup, campaign) = service.addGroup(
                        userId = userId,
                        groupId = groupId,
                        camp = camp
                    )

                    send.invoke(
                        msgBackMenu(
                            resourceText(
                                text.msgSuccessAddGroup,
                                "group.id" to "${addedGroup.groupId}",
                                "camp.desc" to "${campaign.id} ${campaign.name}"
                            ), text.back
                        ).apply { chatId = userId.toString() }
                    )

                } catch (e: NoAccessException) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errAddGroupAccessDenied
                        chatId = chatId(upd).toString()
                    })
                    log.error("Group creating err (access denied).", e)
                } catch (t: Throwable) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errAddGroup
                        chatId = chatId(upd).toString()
                    })
                    log.error("Group creating err.", t)
                }
            }
        }
        MAIN_MENU_DELETE_GROUP -> {
            when (message(upd).text) {
                text.back -> actionBack.invoke()
                else -> try {
                    val params = message(upd).text.split("\\s+".toRegex(), 2)
                    val groupId = params[0].toLong()
                    val camp = campaign!!

                    val userId = fromId(upd)

                    val (deletedGroup, campaign) = service.deleteGroup(
                        userId = userId,
                        groupId = groupId,
                        camp = camp
                    )

                    send.invoke(
                        msgBackMenu(
                            resourceText(
                                text.msgSuccessDeleteGroup,
                                "group.id" to "${deletedGroup.groupId}",
                                "camp.desc" to "${campaign.id} ${campaign.name}"
                            ), text.back
                        ).apply { chatId = userId.toString() }
                    )

                } catch (e: AdminNotFoundException) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errDeleteGroupNotFound
                        chatId = chatId(upd).toString()
                    })
                    log.error("Group deleting err (not found).", e)
                } catch (e: NoAccessException) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errDeleteGroupAccessDenied
                        chatId = chatId(upd).toString()
                    })
                    log.error("Group deleting err (access denied).", e)
                } catch (t: Throwable) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errDeleteGroup
                        chatId = chatId(upd).toString()
                    })
                    log.error("Group deleting err.", t)
                }
            }
        }
        MAIN_MENU_ADD_ADMIN -> {
            when (message(upd).text) {
                text.back -> actionBack.invoke()
                else -> try {
                    val params = message(upd).text.split("\\s+".toRegex())
                    val adminId = params[0].toInt()
                    val camp = campaign!!

                    val userId = fromId(upd)

                    val (addedAdmin, campaign) = service.addAdmin(
                        userId = userId,
                        adminId = adminId,
                        camp = camp
                    )

                    send.invoke(msgBackMenu(
                        resourceText(
                            text.msgSuccessAddAdmin,
                            "admin.desc" to "${addedAdmin.userId} ${addedAdmin.userName}",
                            "camp.desc" to "${campaign.id} ${campaign.name}"
                        ), text.back
                    ).apply { chatId = chatId(upd).toString() })

                } catch (e: NoAccessException) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errAddAdminAccessDenied
                        chatId = chatId(upd).toString()
                    })
                    log.error("AdminGroup creating err (access denied).", e)
                } catch (t: Throwable) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errAddAdmin
                        chatId = chatId(upd).toString()
                    })
                    log.error("AdminGroup creating err.", t)
                }
            }
        }
        MAIN_MENU_DELETE_ADMIN -> {
            when (message(upd).text) {
                text.back -> actionBack.invoke()
                else -> try {
                    val params = message(upd).text.split("\\s+".toRegex(), 2)
                    val adminId = params[0].toInt()
                    val camp = campaign!!

                    val userId = fromId(upd)

                    val (deletedAdmin, campaign) = service.deleteAdmin(
                        userId = userId,
                        adminId = adminId,
                        camp = camp
                    )

                    send.invoke(
                        msgBackMenu(
                            resourceText(
                                text.msgSuccessDeleteAdmin,
                                "admin.desc" to "${deletedAdmin.userId} ${deletedAdmin.userName}",
                                "camp.desc" to "${campaign.id} ${campaign.name}"
                            ), text.back
                        ).apply { chatId = userId.toString() }
                    )

                } catch (e: AdminNotFoundException) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errDeleteAdminNotFound
                        chatId = chatId(upd).toString()
                    })
                    log.error("AdminGroup deleting err (not found).", e)
                } catch (e: NoAccessException) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errDeleteAdminAccessDenied
                        chatId = chatId(upd).toString()
                    })
                    log.error("AdminGroup deleting err (access denied).", e)
                } catch (t: Throwable) {
                    send.invoke(SendMessage().apply {
                        this.text = text.errDeleteAdmin
                        chatId = chatId(upd).toString()
                    })
                    log.error("AdminGroup deleting err.", t)
                }
            }
        }
        MAIN_MENU_STATISTIC -> {
            when (message(upd).text) {
                text.back -> actionBack.invoke()
                else -> when (message(upd).text) {
                    text.sendCampaignsTable -> {
                        sendTable.invoke(message(upd).chatId, service.getAllCampaigns(), text.tableNameCampaigns)
                    }
                    text.sendSuperAdminTable -> {
                        sendTable.invoke(message(upd).chatId, service.getAllSuperAdmins(), text.tableNameSuperAdmins)
                    }
                    text.sendSurveysTable -> {
                        send.invoke(
                            msgAvailableCampaignsList(
                                text.msgSurveysTable,
                                "$GET_EXCEL_TABLE_SURVEY",
                                service.getAllCampaigns()
                            ).apply { chatId = chatId(upd).toString() }
                        )
                    }
                    text.sendAdminsTable -> {
                        send.invoke(
                            msgAvailableCampaignsList(
                                text.msgAdminsTable,
                                "$GET_EXCEL_TABLE_ADMINS",
                                service.getAllCampaigns()
                            ).apply { chatId = chatId(upd).toString() }
                        )
                    }
                    text.sendUsersInCampaign -> {
                        send.invoke(
                            msgAvailableCampaignsList(
                                text.msgUsersInCampaign,
                                "$GET_EXCEL_TABLE_USERS_IN_CAMPAIGN",
                                service.getAllCampaigns()
                            ).apply { chatId = chatId(upd).toString() }
                        )
                    }
                }
            }
        }
        SURVEY_CREATE -> {
            val survey = Survey(
                name = message(upd).text,
                createDate = now(),
                questions = HashSet(),
                campaign = campaign!!
            )

            this.state = NONE
            this.survey = survey

            editMessage.invoke(editSurvey(text, survey, updCallback!!))
        }
        SURVEY_NAME -> {
            val survey = survey?.also { it.name = message(upd).text }
                ?: Survey(
                    name = message(upd).text,
                    createDate = now(),
                    questions = HashSet(),
                    campaign = campaign!!
                )

            this.state = NONE
            this.survey = survey

            editMessage.invoke(editSurvey(text, survey, updCallback!!))
        }
        SURVEY_DESCRIPTION -> {
            val survey = survey!!.also { it.description = message(upd).text }

            this.state = NONE
            this.survey = survey

            editMessage.invoke(editSurvey(text, survey, updCallback!!))
        }
        SURVEY_QUESTION_CREATE -> {
            val question = Question(text = message(upd).text, options = HashSet())
            state = NONE
            this.question = question

            editMessage.invoke(editQuestion(text, question, updCallback!!))
        }
        SURVEY_QUESTION_EDIT_TEXT -> {
            val question = question!!.also { it.text = message(upd).text }
            this.state = NONE
            this.question = question

            editMessage.invoke(editQuestion(text, question, updCallback!!))
        }
        SURVEY_QUESTION_EDIT_SORT -> {
            try {
                val question = question!!.also { it.sortPoints = message(upd).text.toInt() }
                this.state = NONE
                this.question = question

                editMessage.invoke(editQuestion(text, question, updCallback!!))
            } catch (t: Throwable) {
                log.warn("error read sortPoints", t)

                editMessage.invoke(
                    enterText(
                        updCallback!!.callbackQuery!!.message,
                        text.errSurveyEnterNumber,
                        text.backToSurveyQuestionMenu,
                        SURVEY_QUESTION_SELECT
                    )
                )
            }
        }
        SURVEY_OPTION_CREATE -> {
            val option = Option(text = message(upd).text)
            this.state = NONE
            this.option = option

            editMessage.invoke(editOption(text, option, updCallback!!))
        }
        SURVEY_OPTION_EDIT_TEXT -> {
            val option = option!!.also { it.text = message(upd).text }
            this.state = NONE
            this.option = option

            editMessage.invoke(editOption(text, option, updCallback!!))
        }
        SURVEY_OPTION_EDIT_CORRECT -> {
            try {
                val option =
                    option!!.also { it.correct = message(upd).text.equals("true", ignoreCase = true) }

                this.state = NONE
                this.option = option

                editMessage.invoke(editOption(text, option, updCallback!!))
            } catch (t: Throwable) {
                log.warn("error read sortPoints", t)

                editMessage.invoke(
                    enterText(
                        updCallback!!.callbackQuery!!.message,
                        text.errSurveyEnterNumber,
                        text.backToSurveyQuestionMenu,
                        SURVEY_QUESTION_SELECT
                    )
                )
            }
        }
        SURVEY_OPTION_EDIT_SORT -> {
            try {
                val option = option!!.also { it.sortPoints = message(upd).text.toInt() }

                state = NONE
                survey!!.questions = survey!!.questions.toHashSet().apply { add(question!!) }
                this.option = option
                editMessage.invoke(editOption(text, option, updCallback!!))
            } catch (t: Throwable) {
                log.warn("error read sortPoints", t)

                editMessage.invoke(
                    enterText(
                        updCallback!!.callbackQuery!!.message,
                        text.errSurveyEnterNumber,
                        text.backToSurveyQuestionMenu,
                        SURVEY_QUESTION_SELECT
                    )
                )
            }
        }
        else -> {
            when (message(upd).text) {
                text.mainMenuAdd -> {
                    state = MAIN_MENU_ADD
                    user = message(upd).from
                    send.invoke(mainAdminAddMenu(text).apply {
                        replyMarkup = (replyMarkup as ReplyKeyboardMarkup).apply {
                            keyboard = createKeyboard(keyboard.flatten().toArrayList().apply {
                                addElements(
                                    0,
                                    KeyboardButton(text.addMenuCampaign),
                                    KeyboardButton(text.addMenuCommonCampaign),
                                    KeyboardButton(text.addMenuGroup)
                                )
                            })
                        }
                    }.apply { chatId = chatId(upd).toString() })
                }
                text.mainMenuDelete -> {
                    state = MAIN_MENU_DELETE
                    user = message(upd).from
                    send.invoke(mainAdminDeleteMenu(text).apply {
                        replyMarkup = (replyMarkup as ReplyKeyboardMarkup).apply {
                            keyboard = createKeyboard(keyboard.flatten().toArrayList().apply {
                                addElements(
                                    0,
                                    KeyboardButton(text.deleteMenuCampaign),
                                    KeyboardButton(text.deleteMenuCommonCampaign),
                                    KeyboardButton(text.deleteMenuGroup)
                                )
                            })
                        }
                    }.apply { chatId = chatId(upd).toString() })
                }
                text.mainMenuMessages -> {
                    send.invoke(msgBackMenu(text.msgSendToEveryGroup, text.reset).apply {
                        chatId = chatId(upd).toString()
                    })

                    service.getAllCampaigns().toList().run {
                        if (isNotEmpty()) {
                            send.invoke(
                                msgAvailableCampaignsListDivideCommon(
                                    text.adminAvailableCampaigns,
                                    CAMPAIGN_FOR_SEND_GROUP_MSG.toString(),
                                    this
                                ).apply { chatId = chatId(upd).toString() }
                            )
                            state = CAMPAIGN_FOR_SEND_GROUP_MSG
                            user = message(upd).from
                        }
                    }
                }
                text.mainMenuStatistic -> {
                    state = MAIN_MENU_STATISTIC
                    user = message(upd).from
                    send.invoke(mainAdminStatisticMenu(text).apply { chatId = chatId(upd).toString() })
                }
                text.back -> actionBack.invoke()
                else -> {
                    when (state) {
                        MAIN_MENU_STATISTIC -> send.invoke(mainAdminStatisticMenu(text).apply {
                            chatId = chatId(upd).toString()
                        })
                        MAIN_MENU_DELETE -> send.invoke(mainAdminDeleteMenu(text).apply {
                            chatId = chatId(upd).toString()
                        })
                        MAIN_MENU_ADD -> send.invoke(mainAdminAddMenu(text).apply { chatId = chatId(upd).toString() })
                        CAMPAIGN_FOR_SEND_GROUP_MSG -> send.invoke(mainAdminsMenu(text).apply {
                            chatId = chatId(upd).toString()
                        })
                        else -> {
                            send.invoke(mainAdminsMenu(text).apply { chatId = chatId(upd).toString() })
                            log.warn("Not supported action!\n${message(upd)}")
                        }
                    }
                }
            }
        }
    }
}