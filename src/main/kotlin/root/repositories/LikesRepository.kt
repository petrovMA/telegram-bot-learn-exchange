package root.repositories

import root.entity.Like
import org.springframework.data.repository.CrudRepository

interface LikesRepository : CrudRepository<Like, Long> {
    fun findLikeById(id: Int?): Like
    fun countAllByUrlAndAndLike(url: String, like: Boolean?): Int?
    fun findLikeByUserId(userId: Int?): List<Like>
    fun findLikeByUserIdAndMessageId(userId: Int?, messageId: Int?): List<Like>
    fun findLikeByUrl(url: String): List<Like>
    fun findLikesByUrl(url: String): List<Like>
    fun findLikesByUserName(name: String): List<Like>
    fun findLikesByUserId(userId: Int?): List<Like>
    fun deleteLikeById(id: Int?): Int?
}
