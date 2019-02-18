package root.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update
import root.service.ArticleService

/**
 * Created by knekrasov on 10/15/2018.
 */
@Component
open class AccountController @Autowired constructor(open val accountService: ArticleService) : TelegramLongPollingBot(){

    override fun getBotUsername(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUpdateReceived(update: Update?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBotToken(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
