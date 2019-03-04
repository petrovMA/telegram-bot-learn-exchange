package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.Survey

interface SurveyRepository : CrudRepository<Survey, Long>
