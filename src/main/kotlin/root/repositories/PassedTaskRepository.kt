package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.tasks.PassedTask
import root.data.entity.UserInCampaign

interface PassedTaskRepository : CrudRepository<PassedTask, Long> {
    fun findAllByUser(user: UserInCampaign):Iterable<PassedTask>
}
