package root

import org.apache.log4j.Logger
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.telegram.telegrambots.ApiContextInitializer

@SpringBootApplication
open class Main

private val log = Logger.getLogger(Main::class.java)

fun main() {
    ApiContextInitializer.init()

    SpringApplication.run(Main::class.java)
}