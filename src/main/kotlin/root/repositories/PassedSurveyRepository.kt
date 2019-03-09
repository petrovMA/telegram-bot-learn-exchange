package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.PassedSurvey
import root.data.entity.UserInCampaign

interface PassedSurveyRepository : CrudRepository<PassedSurvey, Long> {
    fun findAllByUser(user: UserInCampaign):Iterable<PassedSurvey>
}
