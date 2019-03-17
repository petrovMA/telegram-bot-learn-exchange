package root.libs

import org.apache.log4j.Logger
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import root.data.entity.*
import java.time.OffsetDateTime.now

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

fun stubCampaign(id: Long = 0, name: String = "", groups: HashSet<Group> = HashSet()) =
    Campaign(id = id, name = name, createDate = now(), groups = groups)

fun stubUserInCampaign(
    userId: Int = 0,
    firstName: String? = null,
    lastName: String? = null,
    userName: String? = null,
    campaigns: HashSet<Campaign> = HashSet()
) = UserInCampaign(
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    userName = userName,
    createDate = now(),
    campaigns = campaigns
)