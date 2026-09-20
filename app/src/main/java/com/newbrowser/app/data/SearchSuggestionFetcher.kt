package com.newbrowser.app.data

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * DuckDuckGo's public autocomplete endpoint, used for as-you-type search suggestions in the
 * address bar. Response shape is ["query", ["suggestion1", "suggestion2", ...]].
 */
object SearchSuggestionFetcher {
    fun fetch(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val connection = try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            URL("https://duckduckgo.com/ac/?q=$encoded&type=list").openConnection() as HttpURLConnection
        } catch (e: Exception) {
            return emptyList()
        }
        connection.connectTimeout = 3_000
        connection.readTimeout = 3_000
        return try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val outer = JSONArray(body)
            val suggestionsArray = outer.optJSONArray(1) ?: return emptyList()
            (0 until suggestionsArray.length()).mapNotNull { i -> suggestionsArray.optString(i).takeIf { it.isNotBlank() } }
        } catch (e: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
