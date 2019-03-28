package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.tasks.surveus.Question

interface QuestionRepository : CrudRepository<Question, Long>
