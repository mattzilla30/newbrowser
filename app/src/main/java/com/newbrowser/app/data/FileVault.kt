package com.newbrowser.app.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

/** The six Safe Locker categories, matching their storage subfolder name. */
val VAULT_CATEGORIES = listOf("Images", "Videos", "Docs", "Epub", "Txt", "Others")

data class VaultFile(val name: String, val sizeBytes: Long)

/** Picks a vault category from a file's MIME type, for the top-level "add" shortcut. */
fun vaultCategoryForMimeType(mimeType: String?): String = when {
    mimeType == null -> "Others"
    mimeType.startsWith("image/") -> "Images"
    mimeType.startsWith("video/") -> "Videos"
    mimeType == "application/epub+zip" -> "Epub"
    mimeType == "text/plain" -> "Txt"
    mimeType == "application/pdf" || mimeType.contains("document") || mimeType.contains("msword") -> "Docs"
    else -> "Others"
}

/**
 * A small encrypted local file store ("Safe Locker"): each file a user adds is copied into
 * app-private storage through [EncryptedFile], so its contents are unreadable without this
 * app - unlike a plain copy into app-private storage, which is only as safe as the device's
 * own screen lock. Filenames themselves are stored in the clear (as the encrypted file's own
 * name) since only their contents are sensitive here.
 */
object FileVault {
    private fun masterKey(context: Context): MasterKey =
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private fun categoryDir(context: Context, category: String): File =
        File(context.filesDir, "vault/$category").apply { mkdirs() }

    fun addFile(context: Context, category: String, displayName: String, sourceBytes: ByteArray): Boolean {
        return try {
            val dest = File(categoryDir(context, category), sanitizeFileName(displayName))
            if (dest.exists()) dest.delete()
            val encryptedFile = EncryptedFile.Builder(
                context.applicationContext,
                dest,
                masterKey(context),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
            ).build()
            encryptedFile.openFileOutput().use { it.write(sourceBytes) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun listFiles(context: Context, category: String): List<VaultFile> =
        categoryDir(context, category).listFiles()
            ?.filter { it.isFile }
            ?.map { VaultFile(it.name, it.length()) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    fun itemCount(context: Context, category: String): Int = listFiles(context, category).size

    /** Decrypts a vault entry into the app's cache dir (for viewing/sharing) and returns it. */
    fun decryptToCache(context: Context, category: String, name: String): File? {
        return try {
            val source = File(categoryDir(context, category), name)
            if (!source.exists()) return null
            val encryptedFile = EncryptedFile.Builder(
                context.applicationContext,
                source,
                masterKey(context),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
            ).build()
            val cacheDir = File(context.cacheDir, "vault_open").apply { mkdirs() }
            val outFile = File(cacheDir, name)
            encryptedFile.openFileInput().use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }

    fun delete(context: Context, category: String, name: String) {
        File(categoryDir(context, category), name).delete()
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[/\\\\]"), "_").ifBlank { "file_${System.currentTimeMillis()}" }
}
