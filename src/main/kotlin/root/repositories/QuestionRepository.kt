package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.Question

interface QuestionRepository : CrudRepository<Question, Long>
