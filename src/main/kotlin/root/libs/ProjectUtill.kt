package root.libs

import org.apache.log4j.Logger
import root.data.entity.Option
import root.data.entity.Question

class ProjectUtill

private val log = Logger.getLogger(ProjectUtill::class.java)

fun printQuestions(questions: Set<Question>) = questions
    .toList()
    .sortedBy { it.sortPoints }
    .joinToString { "\n\t${it.text}\n${printOptions(it.options)}" }

fun printOptions(questions: Set<Option>) = questions
    .toList()
    .sortedBy { it.sortPoints }
    .joinToString { "\n\t\t${it.text}\n\t\t${it.value}" }