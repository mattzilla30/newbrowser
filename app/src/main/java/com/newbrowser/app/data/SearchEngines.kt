package com.newbrowser.app.data

data class SearchEngine(val key: String, val label: String, val urlTemplate: String)

val SEARCH_ENGINES: List<SearchEngine> = listOf(
    SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/html/?q=%s"),
    SearchEngine("google", "Google", "https://www.google.com/search?q=%s"),
    SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s"),
    SearchEngine("startpage", "Startpage", "https://www.startpage.com/sp/search?query=%s"),
)

fun searchEngineFor(key: String): SearchEngine = SEARCH_ENGINES.find { it.key == key } ?: SEARCH_ENGINES.first()
