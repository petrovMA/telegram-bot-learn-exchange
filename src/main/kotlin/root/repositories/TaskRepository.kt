package root.repositories

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import root.data.entity.Campaign
import root.data.entity.tasks.Task
import root.data.entity.tasks.surveus.Survey

interface TaskRepository : CrudRepository<Task, Long> {

    @Query(value = "SELECT * from task where campaign_id = ?1 and id not in (SELECT task_id from passed_tasks where user_user_id = ?2)", nativeQuery = true)
    fun findAllForUser(campaignId: Long, userId: Int) : Iterable<Task>

    @Query(value = "SELECT * from task where id not in (SELECT task_id from passed_tasks where user_user_id = ?1) and campaign_id in (SELECT id from campaign where id = campaign_id and common = ?2)", nativeQuery = true)
    fun findAllTasksByUserFromCampaigns(userId: Int, common: Boolean): Iterable<Task>
}
