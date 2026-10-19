package com.example.data.remote

import com.example.data.model.SearchSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class WebSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun performSearch(query: String): Result<List<SearchSource>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            // Use DuckDuckGo Instant Answer API + Search fallback
            val ddgUrl = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"

            val request = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val sources = mutableListOf<SearchSource>()

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val jsonObject = org.json.JSONObject(responseBody)
                
                // Abstract Text
                val abstractText = jsonObject.optString("AbstractText")
                val abstractUrl = jsonObject.optString("AbstractURL")
                val abstractSource = jsonObject.optString("AbstractSource", "Web Search")
                if (abstractText.isNotBlank() && abstractUrl.isNotBlank()) {
                    val domain = extractDomain(abstractUrl)
                    sources.add(SearchSource(
                        title = if (abstractSource.isNotBlank()) "$abstractSource Summary" else query,
                        domain = domain,
                        snippet = abstractText,
                        url = abstractUrl
                    ))
                }

                // Related Topics
                val relatedTopics = jsonObject.optJSONArray("RelatedTopics") ?: JSONArray()
                for (i in 0 until minOf(relatedTopics.length(), 6)) {
                    val item = relatedTopics.optJSONObject(i) ?: continue
                    val text = item.optString("Text")
                    val firstUrl = item.optString("FirstURL")
                    if (text.isNotBlank() && firstUrl.isNotBlank()) {
                        val domain = extractDomain(firstUrl)
                        val title = text.take(60).substringBefore(" - ").substringBefore(" | ")
                        sources.add(SearchSource(
                            title = title,
                            domain = domain,
                            snippet = text,
                            url = firstUrl
                        ))
                    }
                }
            }

            // Fallback HTML search if Instant Answer returned fewer than 2 results
            if (sources.size < 2) {
                val htmlSources = performHtmlFallbackSearch(encodedQuery)
                sources.addAll(htmlSources)
            }

            val distinctSources = sources.distinctBy { it.url }.take(5)
            Result.success(distinctSources)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Web search request failed."))
        }
    }

    private fun performHtmlFallbackSearch(encodedQuery: String): List<SearchSource> {
        val sources = mutableListOf<SearchSource>()
        try {
            val htmlUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            val request = Request.Builder()
                .url(htmlUrl)
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""

            val resultPattern = Pattern.compile("<a class=\"result__url\" href=\"([^\"]+)\"[^>]*>\\s*([^<]+)</a>.*?<a class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.DOTALL)
            val matcher = resultPattern.matcher(html)

            var count = 0
            while (matcher.find() && count < 5) {
                var rawUrl = matcher.group(1) ?: ""
                val rawDomain = matcher.group(2)?.trim() ?: ""
                val rawSnippet = matcher.group(3)?.replace(Regex("<[^>]+>"), "")?.trim() ?: ""

                if (rawUrl.contains("uddg=")) {
                    rawUrl = java.net.URLDecoder.decode(rawUrl.substringAfter("uddg=").substringBefore("&"), "UTF-8")
                }

                if (rawUrl.startsWith("http")) {
                    val domain = extractDomain(rawUrl)
                    val title = if (rawDomain.isNotBlank()) rawDomain else domain
                    sources.add(SearchSource(
                        title = title,
                        domain = domain,
                        snippet = rawSnippet,
                        url = rawUrl
                    ))
                    count++
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }
        return sources
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val host = uri.host ?: url
            host.removePrefix("www.")
        } catch (e: Exception) {
            url
        }
    }
}
