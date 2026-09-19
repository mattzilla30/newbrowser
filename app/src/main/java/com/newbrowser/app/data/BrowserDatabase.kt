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

/**
 * An open, non-incognito tab persisted so it can be restored after the app restarts.
 * Group membership rides along on the same row rather than a separate table, since a tab's
 * position in this list is how it's matched back up on restore; there's no other stable id.
 */
data class PersistedTab(
    val url: String,
    val title: String,
    val isActive: Boolean,
    val groupId: String? = null,
    val groupName: String? = null,
    val groupColorIndex: Int = 0,
)

/** A remembered allow/deny decision for one origin + permission ("media" or "location"). */
data class SitePermission(val origin: String, val permission: String, val granted: Boolean)

/** A page saved for later, distinct from bookmarks by having a read/unread state. */
data class ReadingListEntry(val id: Long, val url: String, val title: String, val addedAt: Long, val isRead: Boolean)

/** An offline MHTML snapshot of a page, saved via WebView.saveWebArchive. */
data class SavedPage(val id: Long, val url: String, val title: String, val filePath: String, val savedAt: Long)

private const val DB_NAME = "newbrowser.db"
private const val DB_VERSION = 5

private const val TABLE_BOOKMARKS = "bookmarks"
private const val TABLE_HISTORY = "history"
private const val TABLE_DOWNLOADS = "downloads"
private const val TABLE_OPEN_TABS = "open_tabs"
private const val TABLE_SITE_PERMISSIONS = "site_permissions"
private const val TABLE_READING_LIST = "reading_list"
private const val TABLE_SAVED_PAGES = "saved_pages"

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
        db.execSQL(
            "CREATE TABLE $TABLE_OPEN_TABS (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "is_active INTEGER NOT NULL, " +
                "position INTEGER NOT NULL, " +
                "group_id TEXT, " +
                "group_name TEXT, " +
                "group_color INTEGER NOT NULL DEFAULT 0)",
        )
        db.execSQL(
            "CREATE TABLE $TABLE_SITE_PERMISSIONS (" +
                "origin TEXT NOT NULL, " +
                "permission TEXT NOT NULL, " +
                "granted INTEGER NOT NULL, " +
                "PRIMARY KEY (origin, permission))",
        )
        db.execSQL(
            "CREATE TABLE $TABLE_READING_LIST (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT NOT NULL UNIQUE, " +
                "title TEXT NOT NULL, " +
                "added_at INTEGER NOT NULL, " +
                "is_read INTEGER NOT NULL DEFAULT 0)",
        )
        db.execSQL(
            "CREATE TABLE $TABLE_SAVED_PAGES (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "file_path TEXT NOT NULL, " +
                "saved_at INTEGER NOT NULL)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKMARKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOWNLOADS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OPEN_TABS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SITE_PERMISSIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_READING_LIST")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAVED_PAGES")
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

    fun saveOpenTabs(tabs: List<PersistedTab>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_OPEN_TABS, null, null)
            tabs.forEachIndexed { index, tab ->
                val values = ContentValues().apply {
                    put("url", tab.url)
                    put("title", tab.title)
                    put("is_active", if (tab.isActive) 1 else 0)
                    put("position", index)
                    put("group_id", tab.groupId)
                    put("group_name", tab.groupName)
                    put("group_color", tab.groupColorIndex)
                }
                db.insert(TABLE_OPEN_TABS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getOpenTabs(): List<PersistedTab> {
        val result = mutableListOf<PersistedTab>()
        readableDatabase.query(
            TABLE_OPEN_TABS,
            null,
            null,
            null,
            null,
            null,
            "position ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    PersistedTab(
                        url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) != 0,
                        groupId = cursor.getString(cursor.getColumnIndexOrThrow("group_id")),
                        groupName = cursor.getString(cursor.getColumnIndexOrThrow("group_name")),
                        groupColorIndex = cursor.getInt(cursor.getColumnIndexOrThrow("group_color")),
                    ),
                )
            }
        }
        return result
    }

    /** Most-visited distinct pages, for the new-tab speed dial. */
    fun getTopSites(limit: Int = 8): List<HistoryEntry> {
        val result = mutableListOf<HistoryEntry>()
        readableDatabase.rawQuery(
            "SELECT MIN(id) AS id, url, MAX(title) AS title, COUNT(*) AS visits, MAX(visited_at) AS visited_at " +
                "FROM $TABLE_HISTORY GROUP BY url ORDER BY visits DESC, visited_at DESC LIMIT ?",
            arrayOf(limit.toString()),
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

    /** Bulk-inserts bookmarks parsed from an imported Netscape-format bookmarks HTML file. */
    fun importBookmarks(entries: List<Pair<String, String>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            entries.forEach { (url, title) ->
                val values = ContentValues().apply {
                    put("url", url)
                    put("title", title)
                    put("created_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_BOOKMARKS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun addToReadingList(url: String, title: String) {
        val values = ContentValues().apply {
            put("url", url)
            put("title", title)
            put("added_at", System.currentTimeMillis())
            put("is_read", 0)
        }
        writableDatabase.insertWithOnConflict(TABLE_READING_LIST, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeFromReadingList(url: String) {
        writableDatabase.delete(TABLE_READING_LIST, "url = ?", arrayOf(url))
    }

    fun isInReadingList(url: String): Boolean {
        readableDatabase.query(
            TABLE_READING_LIST,
            arrayOf("id"),
            "url = ?",
            arrayOf(url),
            null,
            null,
            null,
        ).use { cursor -> return cursor.moveToFirst() }
    }

    fun setReadingListEntryRead(id: Long, isRead: Boolean) {
        val values = ContentValues().apply { put("is_read", if (isRead) 1 else 0) }
        writableDatabase.update(TABLE_READING_LIST, values, "id = ?", arrayOf(id.toString()))
    }

    fun getReadingList(): List<ReadingListEntry> {
        val result = mutableListOf<ReadingListEntry>()
        readableDatabase.query(
            TABLE_READING_LIST,
            null,
            null,
            null,
            null,
            null,
            "added_at DESC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    ReadingListEntry(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        addedAt = cursor.getLong(cursor.getColumnIndexOrThrow("added_at")),
                        isRead = cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) != 0,
                    ),
                )
            }
        }
        return result
    }

    fun getSitePermission(origin: String, permission: String): Boolean? {
        readableDatabase.query(
            TABLE_SITE_PERMISSIONS,
            arrayOf("granted"),
            "origin = ? AND permission = ?",
            arrayOf(origin, permission),
            null,
            null,
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getInt(0) != 0
        }
    }

    fun setSitePermission(origin: String, permission: String, granted: Boolean) {
        val values = ContentValues().apply {
            put("origin", origin)
            put("permission", permission)
            put("granted", if (granted) 1 else 0)
        }
        writableDatabase.insertWithOnConflict(TABLE_SITE_PERMISSIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun clearSitePermission(origin: String, permission: String) {
        writableDatabase.delete(TABLE_SITE_PERMISSIONS, "origin = ? AND permission = ?", arrayOf(origin, permission))
    }

    fun getAllSitePermissions(): List<SitePermission> {
        val result = mutableListOf<SitePermission>()
        readableDatabase.query(
            TABLE_SITE_PERMISSIONS,
            null,
            null,
            null,
            null,
            null,
            "origin ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    SitePermission(
                        origin = cursor.getString(cursor.getColumnIndexOrThrow("origin")),
                        permission = cursor.getString(cursor.getColumnIndexOrThrow("permission")),
                        granted = cursor.getInt(cursor.getColumnIndexOrThrow("granted")) != 0,
                    ),
                )
            }
        }
        return result
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

    fun removeDownload(id: Long) {
        writableDatabase.delete(TABLE_DOWNLOADS, "id = ?", arrayOf(id.toString()))
    }

    fun addSavedPage(url: String, title: String, filePath: String) {
        val values = ContentValues().apply {
            put("url", url)
            put("title", title)
            put("file_path", filePath)
            put("saved_at", System.currentTimeMillis())
        }
        writableDatabase.insert(TABLE_SAVED_PAGES, null, values)
    }

    fun removeSavedPage(id: Long) {
        writableDatabase.delete(TABLE_SAVED_PAGES, "id = ?", arrayOf(id.toString()))
    }

    fun getSavedPages(): List<SavedPage> {
        val result = mutableListOf<SavedPage>()
        readableDatabase.query(
            TABLE_SAVED_PAGES,
            null,
            null,
            null,
            null,
            null,
            "saved_at DESC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    SavedPage(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        filePath = cursor.getString(cursor.getColumnIndexOrThrow("file_path")),
                        savedAt = cursor.getLong(cursor.getColumnIndexOrThrow("saved_at")),
                    ),
                )
            }
        }
        return result
    }
}
