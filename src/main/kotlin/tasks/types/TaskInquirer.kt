package tasks.types

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import java.io.File
import java.io.IOException

class TaskInquirer(val conf: Config) {
    private val log = Logger.getLogger(TaskInquirer::class.java)
    init {

    }
}