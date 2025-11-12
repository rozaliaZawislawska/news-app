package com.basic.newsapp.util


import com.basic.newsapp.model.Article
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
suspend fun parseRss(inputStream: InputStream): List<Article> {
    val articles = mutableListOf<Article>()
    var currentArticle: Article? = null
    var text: String? = null
    var inItem = false

    try {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        inItem = true
                        currentArticle = Article()
                    }
                }
                XmlPullParser.TEXT -> {
                    text = parser.text
                }
                XmlPullParser.END_TAG -> {
                    if (inItem && currentArticle != null) {
                        when {
                            tagName.equals("title", ignoreCase = true) ->
                                currentArticle = currentArticle.copy(title = text ?: "")
                            tagName.equals("link", ignoreCase = true) ->
                                currentArticle = currentArticle.copy(link = text ?: "")
                            tagName.equals("guid", ignoreCase = true) ->
                                currentArticle = currentArticle.copy(guid = text ?: "")
                            tagName.equals("description", ignoreCase = true) ->
                                currentArticle = currentArticle.copy(content = text ?: "")
                            tagName.equals("enclosure", ignoreCase = true) -> {
                                val imageUrl = parser.getAttributeValue(null, "url")
                                currentArticle = currentArticle.copy(imageUrl = imageUrl)
                            }
                            tagName.equals("item", ignoreCase = true) -> {
                                articles.add(currentArticle)
                                inItem = false
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
    return articles
}