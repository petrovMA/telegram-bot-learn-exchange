package root

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.telegram.telegrambots.ApiContextInitializer

@SpringBootApplication
open class Main

fun main() {
    ApiContextInitializer.init()
    SpringApplication.run(Main::class.java)
}