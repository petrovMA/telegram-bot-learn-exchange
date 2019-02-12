package tasks.task_01

import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import java.io.File
import java.io.IOException

object Task01 {
    private val log = Logger.getLogger(Task01::class.java)
    val conf = try {
        ConfigFactory.parseFile(File("tasks/task_01/task.conf"))
    } catch (e: IOException) {
        log.error(e.message, e)
        throw e
    }
    init {

    }
}