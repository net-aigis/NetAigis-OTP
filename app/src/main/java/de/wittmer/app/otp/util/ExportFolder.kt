package de.wittmer.app.otp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile

/**
 * SAF export-folder helpers, aligned with fahrtenbuch-android storage (DocumentFile +
 * persistable tree URI).
 */
data class ExportStorageResult(
    val success: Boolean,
    val uri: Uri? = null,
    val fileName: String? = null
)

class UriDisplayNameReader(private val context: Context) {
    /**
     * Label for a SAF tree URI. Prefer the decoded storage path (e.g. `Download/Export`)
     * over [DocumentFile.getName], which is only the last segment (`Export`).
     */
    fun treeLabel(uri: Uri): String? {
        friendlyTreePath(uri)?.let { return it }
        return DocumentFile.fromTreeUri(context, uri)?.name
            ?: runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) {
                        cursor.getString(index)
                    } else {
                        null
                    }
                }
            }.getOrNull()
    }

    /** Decoded path after the volume id, e.g. `primary:Download/Export` → `Download/Export`. */
    fun friendlyTreePath(uri: Uri): String? {
        val segment = uri.lastPathSegment ?: return null
        val decoded = Uri.decode(segment)
        return decoded.substringAfter(':', missingDelimiterValue = decoded).ifBlank { null }
    }
}

class StoragePermissionService(
    private val context: Context,
    private val settings: Settings,
    private val uriDisplayNameReader: UriDisplayNameReader = UriDisplayNameReader(context)
) {
    /**
     * @param takeFlags flags from the SAF result Intent (preferred), or 0 to use read+write.
     */
    fun persistTreePermission(uri: Uri, takeFlags: Int = 0) {
        val flags = if (takeFlags != 0) {
            takeFlags and (
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
        } else {
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        context.contentResolver.takePersistableUriPermission(uri, flags)

        val previous = settings.exportFolderUri
        if (!previous.isNullOrBlank() && previous != uri.toString()) {
            try {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(previous),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Already revoked.
            }
        }
        settings.exportFolderUri = uri.toString()
        settings.exportFolderDisplayName = uriDisplayNameReader.treeLabel(uri)
    }

    fun clearTreePermission() {
        val existing = settings.exportFolderUri
        if (!existing.isNullOrBlank()) {
            try {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(existing),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Already revoked.
            }
        }
        settings.exportFolderUri = null
        settings.exportFolderDisplayName = null
    }

    fun hasPersistedPermission(uri: Uri): Boolean {
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }
    }
}

class DocumentTreeWriter(private val context: Context) {
    fun ensureDirectory(treeUri: Uri): ExportStorageResult = try {
        val dir = rootDirectory(treeUri)
        if (!dir.isDirectory) {
            ExportStorageResult(false)
        } else {
            ExportStorageResult(true, dir.uri)
        }
    } catch (_: Exception) {
        ExportStorageResult(false)
    }

    fun createTextFile(
        treeUri: Uri,
        fileName: String,
        mimeType: String
    ): ExportStorageResult = try {
        val dir = rootDirectory(treeUri)
        dir.findFile(fileName)?.delete()
        val file = dir.createFile(mimeType, fileName)
            ?: return ExportStorageResult(false, fileName = fileName)
        ExportStorageResult(true, file.uri, fileName)
    } catch (_: Exception) {
        ExportStorageResult(false, fileName = fileName)
    }

    private fun rootDirectory(treeUri: Uri): DocumentFile {
        val current = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Export folder unavailable")
        if (!current.isDirectory) {
            throw IllegalStateException("Selected export path is not a folder")
        }
        return current
    }
}
