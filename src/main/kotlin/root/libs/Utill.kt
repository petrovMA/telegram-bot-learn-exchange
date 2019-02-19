package notificator.libs

import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import java.io.IOException
import java.nio.file.Files
import java.util.HashMap
import java.util.regex.Pattern


val format = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
val formatWithoutSeconds = SimpleDateFormat("yyyy.MM.dd HH:mm")
val fileFormat = SimpleDateFormat("yyyy_MM_dd HH_mm_ss")
private val formatMinutes = SimpleDateFormat("mm:ss")
private val LOGGER = Logger.getLogger(Utill::class.java)!!
typealias ResourceParameter = Pair<String, String>

class Utill

private val log = Logger.getLogger(Utill::class.java)

fun Int.ms(): Duration = Duration.ofMillis(this.toLong())
fun Long.ms(): Duration = Duration.ofMillis(this)

fun Int.s(): Duration = Duration.ofSeconds(this.toLong())
fun Long.s(): Duration = Duration.ofSeconds(this)

fun Int.m(): Duration = Duration.ofMinutes(this.toLong())
fun Long.m(): Duration = Duration.ofMinutes(this)

fun Int.h(): Duration = Duration.ofHours(this.toLong())
fun Long.h(): Duration = Duration.ofHours(this)

fun Int.d(): Duration = Duration.ofDays(this.toLong())
fun Long.d(): Duration = Duration.ofDays(this)

fun <T> List<T>.toArrayList(): ArrayList<T> = ArrayList(this)

fun convertTime(time: Long, format_: SimpleDateFormat = format): String = format_.format(Date(time))

@Throws(ParseException::class)
fun convertLongTime(dateTime: String, format: SimpleDateFormat): Long = format.parse(dateTime).time

fun parseTime(strTime: String): Duration =
    strTime.split("[^\\dsmhd]+".toRegex())
        .asSequence()
        .filter { !it.isBlank() }
        .map {
            if (it.drop(it.length - 2) == "ms") {
                it.dropLast(2).toLong().ms()
            } else when (it.drop(it.length - 1)) {
                "s" -> it.dropLast(1).toLong().s()
                "m" -> it.dropLast(1).toLong().m()
                "h" -> it.dropLast(1).toLong().h()
                "d" -> it.dropLast(1).toLong().d()
                else -> throw Exception("Unsupported time.")
            }
        }.sum()

fun Sequence<Duration>.sum(): Duration {
    var sum = Duration.ZERO
    for (element in this) {
        sum += element
    }
    return sum
}

fun readConf(path: String?): Config? = try {
    path?.run { ConfigFactory.parseFile(File(this)) }
} catch (t: Throwable) {
    log.error("Can't read config file: '$path'", t)
    null
}

fun resourceText(text: String, vararg params: ResourceParameter = emptyArray()): String {
    val parameters = params.asIterable()
    var resourceText = text

    parameters.forEach {
        val afterReplacement = resourceText.replace('$' + it.first, it.second)
        if (afterReplacement === resourceText) {
            println("Warning! Parameter '${it.first}' not found in text '$text'")
        }
        resourceText = afterReplacement
    }

    return resourceText
}