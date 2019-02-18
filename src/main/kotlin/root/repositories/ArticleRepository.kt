package root.repositories

import root.entity.Article
import org.springframework.data.repository.CrudRepository

interface ArticleRepository : CrudRepository<Article, Long> {
    fun findArticleById(id: Int?): Article
    fun findArticleByUrl(url: String): Article
    fun findArticleByIdAndUserNameAndUserIdAndThemeAndUrl(id: Int?, userName: String, userId: Int?, theme: String, url: String): Article
    fun findArticleByIdAndUrl(id: Int?, url: String): Article
    fun findArticleByUserId(userId: Long?): Article
    fun findArticleByUserName(username: String): Article
    fun findArticleByFirstName(firstName: String): Article
    fun findArticleByLastName(lastName: String): Article
}
