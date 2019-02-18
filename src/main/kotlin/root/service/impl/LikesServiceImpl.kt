package root.service.impl

import root.entity.Like
import root.repositories.LikesRepository
import root.service.LikesService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class LikesServiceImpl(@Autowired open var likeRepository: LikesRepository? = null) :
    LikesService {

    override fun saveLike(like: Like): Like? = likeRepository!!.save(like)

    override fun deleteLikeById(id: Int): Int? = likeRepository!!.deleteLikeById(id)

    override fun getAllLikes(): List<Like> = likeRepository!!.findAll().toList()

    override fun getLikeById(id: Int): Like? = likeRepository!!.findLikeById(id)

    override fun getLikesByUserName(name: String): List<Like> = likeRepository!!.findLikesByUserName(name).toList()

    override fun getLikesByUrl(url: String): List<Like> = likeRepository!!.findLikesByUrl(url).toList()

    override fun getLikesByUserId(userId: Int): List<Like> = likeRepository!!.findLikesByUserId(userId).toList()

    override fun getLikeByUserId(userId: Int): List<Like> = likeRepository!!.findLikeByUserId(userId).toList()

    override fun getLikeByUserIdAndMessageId(userId: Int, messageId: Int): List<Like> =
            likeRepository!!.findLikeByUserIdAndMessageId(userId, messageId)

    override fun getLikeByUrl(url: String): List<Like> = likeRepository!!.findLikeByUrl(url).toList()

    override fun countLikes(url: String, like: Boolean): Int = likeRepository!!.countAllByUrlAndAndLike(url, like) ?: 0
}
