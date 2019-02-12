import com.typesafe.config.ConfigFactory
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.ApiContext
import org.telegram.telegrambots.meta.TelegramBotsApi
import java.io.File
import java.io.IOException

object Main

private val log = Logger.getLogger(Main::class.java)

fun main() {

    PropertyConfigurator.configure("log4j.properties")

    val conf = try {
        ConfigFactory.parseFile(File("settings.conf"))
    } catch (e: IOException) {
        log.error(e.message, e)
        return
    }
    val proxyHost: String
    val proxyPort: Int
    val useProxy = conf.getBoolean("bot-settings.telegram.proxy.enable")
    val botUsername = conf.getString("bot-settings.telegram.bot-name")!!
    val botToken = conf.getString("bot-settings.telegram.bot-token")!!
    val chatId = conf.getLong("bot-settings.telegram.chat-id")

    ApiContextInitializer.init()
    try {
        val bot: TelegramBot
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
                chatId = chatId,
                botUsername = botUsername,
                botToken = botToken,
                options = botOptions
            )
            TelegramBotsApi().registerBot(bot)
        }

    } catch (e: Exception) {
        e.printStackTrace()
        log.error(e.message, e)
    }
}