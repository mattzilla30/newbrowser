package com.newbrowser.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Bookmark(val id: Long, val url: String, val title: String, val createdAt: Long)
data class HistoryEntry(val id: Long, val url: String, val title: String, val visitedAt: Long)
data class DownloadRecord(
    val id: Long,
    val downloadManagerId: Long,
    val url: String,
    val fileName: String,
    val startedAt: Long,
)

/** A URL+title suggestion surfaced while typing in the address bar. */
data class Suggestion(val url: String, val title: String)

private const val DB_NAME = "newbrowser.db"
private const val DB_VERSION = 1

private const val TABLE_BOOKMARKS = "bookmarks"
private const val TABLE_HISTORY = "history"
private const val TABLE_DOWNLOADS = "downloads"

class BrowserDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_BOOKMARKS (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT NOT NULL UNIQUE, " +
                "title TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE $TABLE_HISTORY (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "visited_at INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE $TABLE_DOWNLOADS (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "download_manager_id INTEGER NOT NULL, " +
                "url TEXT NOT NULL, " +
                "file_name TEXT NOT NULL, " +
                "started_at INTEGER NOT NULL)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKMARKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOWNLOADS")
        onCreate(db)
    }

    fun addBookmark(url: String, title: String) {
        val values = ContentValues().apply {
            put("url", url)
            put("title", title)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(TABLE_BOOKMARKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeBookmark(url: String) {
        writableDatabase.delete(TABLE_BOOKMARKS, "url = ?", arrayOf(url))
    }

    fun isBookmarked(url: String): Boolean {
        readableDatabase.query(
            TABLE_BOOKMARKS,
            arrayOf("id"),
            "url = ?",
            arrayOf(url),
            null,
            null,
            null,
        ).use { cursor -> return cursor.moveToFirst() }
    }

    fun getBookmarks(): List<Bookmark> {
        val result = mutableListOf<Bookmark>()
        readableDatabase.query(
            TABLE_BOOKMARKS,
            null,
            null,
            null,
            null,
            null,
            "created_at DESC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    Bookmark(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                    ),
                )
            }
        }
        return result
    }

    fun addHistoryEntry(url: String, title: String) {
        val values = ContentValues().apply {
            put("url", url)
            put("title", title)
            put("visited_at", System.currentTimeMillis())
        }
        writableDatabase.insert(TABLE_HISTORY, null, values)
    }

    fun getHistory(limit: Int = 500): List<HistoryEntry> {
        val result = mutableListOf<HistoryEntry>()
        readableDatabase.query(
            TABLE_HISTORY,
            null,
            null,
            null,
            null,
            null,
            "visited_at DESC",
            limit.toString(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    HistoryEntry(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        visitedAt = cursor.getLong(cursor.getColumnIndexOrThrow("visited_at")),
                    ),
                )
            }
        }
        return result
    }

    fun clearHistory() {
        writableDatabase.delete(TABLE_HISTORY, null, null)
    }

    fun searchSuggestions(query: String, limit: Int = 5): List<Suggestion> {
        if (query.isBlank()) return emptyList()
        val like = "%${query.trim()}%"
        val seen = LinkedHashMap<String, Suggestion>()

        readableDatabase.query(
            TABLE_BOOKMARKS,
            arrayOf("url", "title"),
            "url LIKE ? OR title LIKE ?",
            arrayOf(like, like),
            null,
            null,
            "created_at DESC",
            limit.toString(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val url = cursor.getString(0)
                seen[url] = Suggestion(url, cursor.getString(1))
            }
        }

        if (seen.size < limit) {
            readableDatabase.query(
                TABLE_HISTORY,
                arrayOf("url", "title"),
                "url LIKE ? OR title LIKE ?",
                arrayOf(like, like),
                null,
                null,
                "visited_at DESC",
                (limit - seen.size).toString(),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val url = cursor.getString(0)
                    if (url !in seen) {
                        seen[url] = Suggestion(url, cursor.getString(1))
                    }
                }
            }
        }

        return seen.values.take(limit)
    }

    fun addDownload(downloadManagerId: Long, url: String, fileName: String) {
        val values = ContentValues().apply {
            put("download_manager_id", downloadManagerId)
            put("url", url)
            put("file_name", fileName)
            put("started_at", System.currentTimeMillis())
        }
        writableDatabase.insert(TABLE_DOWNLOADS, null, values)
    }

    fun getDownloads(): List<DownloadRecord> {
        val result = mutableListOf<DownloadRecord>()
        readableDatabase.query(
            TABLE_DOWNLOADS,
            null,
            null,
            null,
            null,
            null,
            "started_at DESC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    DownloadRecord(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        downloadManagerId = cursor.getLong(cursor.getColumnIndexOrThrow("download_manager_id")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
                        fileName = cursor.getString(cursor.getColumnIndexOrThrow("file_name")),
                        startedAt = cursor.getLong(cursor.getColumnIndexOrThrow("started_at")),
                    ),
                )
            }
        }
        return result
    }
}
