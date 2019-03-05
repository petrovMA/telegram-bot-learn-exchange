package root.libs

import org.apache.log4j.Logger
import root.data.entity.Option
import root.data.entity.Question
import root.data.entity.Survey

class ProjectUtill

private val log = Logger.getLogger(ProjectUtill::class.java)

fun printSurveys(surveys: Iterable<Survey>) = surveys
    .toList()
    .sortedBy { it.name }
    .joinToString("\n\n", "\n") { "$it" }

fun printQuestions(questions: Set<Question>) = questions
    .toList()
    .sortedBy { it.sortPoints }
    .joinToString("\n\n", "\n") { "$it" }

fun printOptions(options: Set<Option>) = options
    .toList()
    .sortedBy { it.sortPoints }
    .joinToString("\n\n", "\n") { "$it" }

fun String.subStr(max: Int, endPart: String = "") =
    if (this.length < max) this else this.substring(0 until max) + endPart