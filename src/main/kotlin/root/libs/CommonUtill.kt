package root.libs

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import root.data.entity.ExcelEntity
import java.io.File
import java.io.FileOutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

val format = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
val formatWithoutSeconds = SimpleDateFormat("yyyy.MM.dd HH:mm")
val fileFormat = SimpleDateFormat("yyyy_MM_dd HH_mm_ss")
private val formatMinutes = SimpleDateFormat("mm:ss")
typealias ResourceParameter = Pair<String, String>

class CommonUtill

private val log = Logger.getLogger(CommonUtill::class.java)

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
fun convertTime(
    time: OffsetDateTime,
    format_: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
): String = format_.format(time)

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

fun <E> ArrayList<E>.addElements(vararg elements: E) = this.addAll(elements)
fun <E> ArrayList<E>.addElements(index: Int = 0, vararg elements: E) = elements.reversed().forEach { add(index, it) }

fun writeIntoExcel(file: File, lines: Iterable<ExcelEntity>) = HSSFWorkbook().let {
    it.createSheet("sheet").let { sheet ->

        val writeRov = { array: Array<String>, s: HSSFSheet, rowNum: Int ->
            val row = s.createRow(rowNum)
            for (j in 0 until array.size) {
                row.createCell(j).setCellValue(array[j])
            }
        }

        writeRov(lines.first().toHead(), sheet, sheet.lastRowNum)

        for (i in lines.iterator())
            writeRov(i.toRow(), sheet, sheet.lastRowNum + 1)

        it.write(FileOutputStream(file))
        it.close()
    }
}
