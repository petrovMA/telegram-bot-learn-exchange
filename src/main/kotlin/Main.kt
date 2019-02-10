import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.ApiContext
import org.telegram.telegrambots.meta.TelegramBotsApi
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.*

private val log = Logger.getLogger(TelegramBot::class.java)

fun main() {
    var properties = Properties()
    val proxyHost: String
    val proxyPort: Int
    val useProxy: Boolean
    val botUsername: String
    val botToken: String
    val chatId: Long

    PropertyConfigurator.configure("log4j.properties")

    try {
        properties = load(File("common.properties"), properties)
    } catch (e: IOException) {
        log.error(e.message, e)
    }

    useProxy = java.lang.Boolean.parseBoolean(properties.getProperty("use_proxy"))

    botUsername = properties.getProperty("bot_name")
    botToken = properties.getProperty("bot_token")
    chatId = properties.getProperty("chat_id").toLong()

    ApiContextInitializer.init()
    try {
        val bot: TelegramBot
        if (useProxy) {
            val portHost = properties.getProperty("port_host")?.split(':')
            if (portHost == null) {
                proxyHost = properties.getProperty("host")
                proxyPort = Integer.parseInt(properties.getProperty("port"))
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

fun load(propertyFile: File, properties: Properties = Properties()): Properties = properties
    .also {
        InputStreamReader(
            FileInputStream(propertyFile),
            Charset.forName("UTF-8")).use { fis ->
            it.load(fis)
            log.debug("properties loaded")
        }
    }