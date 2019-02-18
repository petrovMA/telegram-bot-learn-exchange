package root.service

import root.entity.Article

open interface ArticleService {

    fun getAllArticles(): List<Article>

    fun saveArticle(article: Article) : Article?

    fun getArticle(article: Article) : Article?

    fun getArticleByUrl(url: String) : Article?

    fun getArticle(id: Int, userName: String, userId: Int, theme: String, url: String): Article?

    fun getArticleById(id: Int) : Article?
}
