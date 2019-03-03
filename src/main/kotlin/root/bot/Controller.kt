package root.bot

import com.typesafe.config.Config
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.parse
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
import java.io.File
import javax.annotation.PostConstruct

/**
 * Created by knekrasov on 10/15/2018.
 */

@Controller
open class Controller @Autowired constructor(open val accountService: AdminService) {

    private val log = Logger.getLogger(Controller::class.java)
    private var bot: TelegramLongPollingBot? = null

    @ImplicitReflectionSerializer
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
        val text = serialize(File(conf.getString("bot-settings.texts")))
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
                    text = text,
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
                    text = text,
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

    @ImplicitReflectionSerializer
    private fun serialize(file: File) = Json.parse<Text>(file.readText())
}
