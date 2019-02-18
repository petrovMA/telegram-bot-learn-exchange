package root.service.impl

import root.entity.Article
import root.repositories.ArticleRepository
import root.service.ArticleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class ArticleServiceImpl(@Autowired open var articleRepository: ArticleRepository? = null) :
    ArticleService {

    override fun saveArticle(article: Article): Article? = articleRepository!!.save(article)

    override fun getArticle(article: Article) : Article?  = articleRepository!!.findArticleById(article.id)

    override fun getArticle(id: Int, userName: String, userId: Int, theme: String, url: String) : Article? =
            articleRepository!!.findArticleByIdAndUserNameAndUserIdAndThemeAndUrl(id, userName, userId, theme, url)

    override fun getArticleById(id: Int) : Article? = articleRepository!!.findArticleById(id)

    override fun getArticleByUrl(url : String) : Article? = articleRepository!!.findArticleByUrl(url)

    override fun getAllArticles(): List<Article> = articleRepository!!.findAll().toList()
}
