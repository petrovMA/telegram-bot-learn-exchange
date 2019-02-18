package root.service

import root.entity.Like
import org.springframework.transaction.annotation.Transactional

open interface LikesService {

    fun getAllLikes(): List<Like>

    fun saveLike(like: Like): Like?

    @Transactional
    fun deleteLikeById(id: Int): Int?

    fun getLikeById(id: Int): Like?

    fun getLikeByUserIdAndMessageId(userId: Int, messageId: Int): List<Like>

    fun getLikesByUserName(name: String): List<Like>

    fun getLikesByUrl(url: String): List<Like>

    fun getLikesByUserId(userId: Int): List<Like>

    fun getLikeByUserId(userId: Int): List<Like>

    fun getLikeByUrl(url: String): List<Like>

    fun countLikes(url: String, like: Boolean): Int
}
