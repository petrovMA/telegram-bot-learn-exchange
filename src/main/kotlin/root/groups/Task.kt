package root.groups

import com.typesafe.config.Config

abstract class Task(
    val conf: Config,
    val type: String = conf.getString("type")
    )