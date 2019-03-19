package root.data.dao

import root.data.entity.Question
import java.time.OffsetDateTime

data class SurveyDAO(
    var id: Long,
    var name: String,
    var description: String? = null,
    var currentValue: Int = 0,
    var createDate: OffsetDateTime,
    var questions: List<Question>,
    var state : Int,
    var correct : Boolean = true
)