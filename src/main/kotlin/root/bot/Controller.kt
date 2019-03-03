package root.bot

import com.typesafe.config.Config
import notificator.libs.readConf
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.ApiContext
import org.telegram.telegrambots.meta.TelegramBotsApi
import root.data.MainAdmin
import root.data.Text
import root.service.AdminService
import javax.annotation.PostConstruct

/**
 * Created by knekrasov on 10/15/2018.
 */

@Controller
open class Controller @Autowired constructor(open val accountService: AdminService) {

    private val log = Logger.getLogger(Controller::class.java)
    private var bot: TelegramLongPollingBot? = null

    @PostConstruct
    fun telegram() {
        PropertyConfigurator.configure("log4j.properties")

        val conf = readConf("settings.conf")!!

        val tasks = HashMap<String, String>()

        val proxyHost: String
        val proxyPort: Int
        val useProxy = conf.getBoolean("bot-settings.telegram.proxy.enable")
        val botUsername = conf.getString("bot-settings.telegram.bot-name")!!
        val botToken = conf.getString("bot-settings.telegram.bot-token")!!
        val mainAdmins = conf.getConfigList("bot-settings.super-admins")!!.map {
            MainAdmin(it.getInt("user-id"), it.getString("user-name"))
        }

        ApiContextInitializer.init()
        try {
            if (useProxy) {
                val portHost = conf.getString("bot-settings.telegram.proxy.port-host")?.split(':')
                if (portHost == null) {
                    proxyHost = conf.getString("bot-settings.telegram.proxy.host")
                    proxyPort = conf.getInt("bot-settings.telegram.proxy.port")
                } else {
                    proxyHost = portHost[0]
                    proxyPort = Integer.parseInt(portHost[1])
                }
                // Set up Http proxy
                val botOptions = ApiContext.getInstance(DefaultBotOptions::class.java)
                val httpHost = HttpHost(proxyHost, proxyPort)

                val requestConfig = RequestConfig.custom().setProxy(httpHost).setAuthenticationEnabled(true).build()
                botOptions.requestConfig = requestConfig
                botOptions.proxyHost = proxyHost
                botOptions.proxyPort = proxyPort
                botOptions.requestConfig = requestConfig
                bot = TelegramBot(
                    botUsername = botUsername,
                    botToken = botToken,
                    tasks = tasks,
                    text = readTexts(conf.getConfig("bot-settings.tesxs")),
                    service = accountService,
                    mainAdmins = mainAdmins,
                    options = botOptions
                )
                TelegramBotsApi().registerBot(bot)
            } else {
                bot = TelegramBot(
                    botUsername = botUsername,
                    botToken = botToken,
                    tasks = tasks,
                    text = readTexts(conf.getConfig("bot-settings.tesxs")),
                    service = accountService,
                    mainAdmins = mainAdmins
                )
                TelegramBotsApi().registerBot(bot)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            log.error(e.message, e)
            throw e
        }
    }

    private fun readTexts(conf: Config) = Text(
        mainMenu = conf.getString("main-menu"),
        msgAddAdmin = conf.getString("msg-add-admin"),
        msgAddGroup = conf.getString("msg-add-group"),
        msgNoAdmin = conf.getString("msg-no-admin"),
        msgNoCampaign = conf.getString("msg-no-campaign"),
        addAdmin = conf.getString("add-admin"),
        addGroup = conf.getString("add-group"),
        sendToEveryUser = conf.getString("send-to-every-user"),
        sendToEveryGroup = conf.getString("send-to-every-group"),
        msgSendToEveryUser = conf.getString("msg-send-to-every-user"),
        msgSendToEveryGroup = conf.getString("msg-send-to-every-group"),
        msgNotAdmin = conf.getString("msg-not-admin"),
        addAdminToCampaign = conf.getString("add-admin-to-campaign"),
        msgAdminToCampaign = conf.getString("msg-admin-to-campaign"),
        errAdminToCampaign = conf.getString("err-admin-to-campaign"),
        addGroupToCampaign = conf.getString("add-group-to-campaign"),
        errGroupToCampaign = conf.getString("err-group-to-campaign"),
        msgGroupToCampaign = conf.getString("msg-group-to-campaign"),
        createCampaign = conf.getString("add-create-campaign"),
        errCreateCampaign = conf.getString("err-create-campaign"),
        msgCreateCampaign = conf.getString("msg-create-campaign"),
        removeCampaign = conf.getString("remove-campaign"),
        errRemoveCampaign = conf.getString("err-remove-campaign"),
        msgRemoveCampaign = conf.getString("msg-remove-campaign"),
        timeOutTask = conf.getString("task-time-out"),
        showTasksList = conf.getString("task-time-out"),
        taskNotFound = conf.getString("task-time-out"),
        inviteText = conf.getString("invite-text"),
        removeAdminFromCampaign = conf.getString("remove-admin-from-campaign"),
        msgRemoveAdminFromCampaign = conf.getString("err-remove-admin-from-campaign"),
        errRemoveAdminFromCampaign = conf.getString("msg-remove-admin-from-campaign"),
        removeGroupFromCampaign = conf.getString("remove-group-from-campaign"),
        msgRemoveGroupFromCampaign = conf.getString("msg-remove-group-from-campaign"),
        errRemoveGroupFromCampaign = conf.getString("err-remove-group-from-campaign"),
        joinToCampaign = conf.getString("join-to-campaign"),
        showUserCampaigns = conf.getString("show-user-campaigns"),
        userAvailableCampaigns = conf.getString("user-available-campaigns"),
        msgUserAvailableCampaignsNotFound = conf.getString("msg-user-available-campaigns-not-found"),
        userMainMenu = conf.getString("user-main-menu"),
        errClbUserAddedToCampaign = conf.getString("err-clb-user-added-to-campaign"),
        clbUserAddedToCampaign = conf.getString("clb-user-added-to-campaign"),
        userAddedToCampaign = conf.getString("user-added-to-campaign"),
        errUserAddedToCampaign = conf.getString("err-user-added-to-campaign"),
        sucCreateCampaign = conf.getString("suc-create-campaign"),
        sucAdminToCampaign = conf.getString("suc-admin-to-campaign"),
        sucGroupToCampaign = conf.getString("suc-group-to-campaign"),
        sucRemoveCampaign = conf.getString("suc-remove-campaign"),
        sucRemoveAdminFromCampaign = conf.getString("suc-remove-admin-from-campaign"),
        sucRemoveGroupFromCampaign = conf.getString("suc-remove-group-from-campaign"),
        sucMsgToUsers = conf.getString("suc-msg-to-users"),
        errMsgToUsersNotFound = conf.getString("err-msg-to-users-not-found"),
        errMsgToUsers = conf.getString("err-msg-to-users"),
        sucMsgToCampaign = conf.getString("suc-msg-to-campaign"),
        errMsgToCampaignNotFound = conf.getString("err-msg-to-campaign-not-found"),
        errMsgToCampaign = conf.getString("err-msg-to-campaign"),
        adminAvailableCampaigns = conf.getString("admin-available-campaigns"),
        errClbSendMessageToEveryGroup = conf.getString("err-clb-send-message-to-every-group"),
        clbSendMessageToEveryGroup = conf.getString("clb-send-message-to-every-group"),
        sucSendMessageToEveryGroup = conf.getString("suc-send-message-to-every-group"),
        errSendMessageToEveryGroup = conf.getString("err-send-message-to-every-group"),
        clbSendMessageToEveryUsers = conf.getString("clb-send-message-to-every-users"),
        sucSendMessageToEveryUsers = conf.getString("suc-send-message-to-every-users"),
        errClbSendMessageToEveryUsers = conf.getString("err-clb-send-message-to-every-users"),
        errCommon = conf.getString("err-common"),
        errClbCommon = conf.getString("err-clb-common"),
        reset = conf.getString("reset")
    )
}
