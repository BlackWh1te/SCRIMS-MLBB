package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class NewsArticleTest {

    @Test
    fun `default NewsArticle has empty fields`() {
        val article = NewsArticle()
        assertEquals("", article.id)
        assertEquals("", article.title)
        assertEquals("", article.description)
        assertEquals("", article.content)
        assertEquals("", article.url)
        assertEquals("", article.imageUrl)
        assertEquals("", article.source)
    }

    @Test
    fun `default originalLanguage is en`() {
        assertEquals("en", NewsArticle().originalLanguage)
    }

    @Test
    fun `default isTranslated is false`() {
        assertFalse(NewsArticle().isTranslated)
    }

    @Test
    fun `default publishedAt is near current time`() {
        val before = System.currentTimeMillis()
        val article = NewsArticle()
        val after = System.currentTimeMillis()
        assertTrue(article.publishedAt in before..after)
    }

    @Test
    fun `copy creates independent instance`() {
        val original = NewsArticle(id = "1", title = "Original")
        val copy = original.copy(title = "Copy")
        assertEquals("Original", original.title)
        assertEquals("Copy", copy.title)
        assertEquals("1", copy.id)
    }
}
