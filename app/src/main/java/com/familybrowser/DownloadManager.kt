package com.familybrowser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * DownloadManager.kt
 * Handles file downloads with progress tracking, notifications, and resume support.
 *
 * Features:
 * - Download any file (PDF, image, video, etc.)
 * - Progress notification with percentage
 * - Pause / Resume / Cancel
 * - Download history
 * - Auto-open after download
 * - Organized save locations by file type
 */

enum class DownloadStatus { PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED }

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    var fileName: String,
    var mimeType: String = "application/octet-stream",
    var totalBytes: Long = -1L,
    var downloadedBytes: Long = 0L,
    var status: DownloadStatus = DownloadStatus.PENDING,
    var errorMessage: String = "",
    var localPath: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    var completedAt: Long = 0L
) {
    val progress: Int get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else -1
    val isActive: Boolean get() = status == DownloadStatus.DOWNLOADING
}

class BrowserDownloadManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val CHANNEL_NAME = "Downloads"
        const val MAX_CONCURRENT = 3
        private const val BUFFER_SIZE = 8192
    }

    val downloads = mutableStateListOf<DownloadItem>()
    private val activeJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        createNotificationChannel()
    }

    // ─── Start Download ───────────────────────────────────────────────────────

    fun startDownload(
        url: String,
        fileName: String? = null,
        mimeType: String = "application/octet-stream",
        userAgent: String = "FamilyBrowser/1.0",
        referer: String? = null,
        cookies: String? = null
    ): DownloadItem {
        val safeFileName = fileName ?: extractFileName(url)
        val item = DownloadItem(url = url, fileName = safeFileName, mimeType = mimeType)
        downloads.add(0, item)

        val job = scope.launch {
            executeDownload(item, userAgent, referer, cookies)
        }
        activeJobs[item.id] = job
        return item
    }

    private suspend fun executeDownload(
        item: DownloadItem,
        userAgent: String,
        referer: String?,
        cookies: String?
    ) = withContext(Dispatchers.IO) {
        try {
            updateItem(item.id) { it.copy(status = DownloadStatus.DOWNLOADING) }

            val connection = (URL(item.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                setRequestProperty("User-Agent", userAgent)
                referer?.let { setRequestProperty("Referer", it) }
                cookies?.let { setRequestProperty("Cookie", it) }
                // Resume support
                if (item.downloadedBytes > 0) {
                    setRequestProperty("Range", "bytes=${item.downloadedBytes}-")
                }
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                updateItem(item.id) { it.copy(status = DownloadStatus.FAILED, errorMessage = "HTTP $responseCode") }
                return@withContext
            }

            val total = connection.contentLengthLong
            if (total > 0) updateItem(item.id) { it.copy(totalBytes = total) }

            val file = getOutputFile(item)
            updateItem(item.id) { it.copy(localPath = file.absolutePath) }

            connection.inputStream.use { input ->
                FileOutputStream(file, item.downloadedBytes > 0).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = item.downloadedBytes
                    var bytes: Int

                    while (input.read(buffer).also { bytes = it } != -1) {
                        // Check for cancellation
                        val current = downloads.find { it.id == item.id }
                        if (current?.status == DownloadStatus.CANCELLED) {
                            file.delete()
                            return@withContext
                        }
                        if (current?.status == DownloadStatus.PAUSED) {
                            updateItem(item.id) { it.copy(downloadedBytes = downloaded) }
                            return@withContext
                        }

                        output.write(buffer, 0, bytes)
                        downloaded += bytes

                        // Update progress every 0.5s to avoid excessive recomposition
                        if (downloaded % (BUFFER_SIZE * 64) == 0L || bytes < BUFFER_SIZE) {
                            updateItem(item.id) { it.copy(downloadedBytes = downloaded) }
                            showProgressNotification(downloads.find { it.id == item.id }!!)
                        }
                    }

                    updateItem(item.id) {
                        it.copy(
                            status = DownloadStatus.COMPLETED,
                            downloadedBytes = downloaded,
                            completedAt = System.currentTimeMillis()
                        )
                    }
                    showCompletedNotification(downloads.find { it.id == item.id }!!, file)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) return@withContext
            updateItem(item.id) { it.copy(status = DownloadStatus.FAILED, errorMessage = e.message ?: "Unknown error") }
            showErrorNotification(item)
        } finally {
            activeJobs.remove(item.id)
        }
    }

    // ─── Controls ─────────────────────────────────────────────────────────────

    fun pauseDownload(downloadId: String) {
        updateItem(downloadId) { it.copy(status = DownloadStatus.PAUSED) }
    }

    fun resumeDownload(downloadId: String) {
        val item = downloads.find { it.id == downloadId } ?: return
        val updatedItem = item.copy(status = DownloadStatus.DOWNLOADING)
        val index = downloads.indexOfFirst { it.id == downloadId }
        if (index != -1) downloads[index] = updatedItem
        val job = scope.launch { executeDownload(updatedItem, "FamilyBrowser/1.0", null, null) }
        activeJobs[downloadId] = job
    }

    fun cancelDownload(downloadId: String) {
        updateItem(downloadId) { it.copy(status = DownloadStatus.CANCELLED) }
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        NotificationManagerCompat.from(context).cancel(downloadId.hashCode())
    }

    fun removeFromList(downloadId: String) {
        cancelDownload(downloadId)
        downloads.removeAll { it.id == downloadId }
    }

    fun retryDownload(downloadId: String) {
        val item = downloads.find { it.id == downloadId } ?: return
        val reset = item.copy(status = DownloadStatus.PENDING, downloadedBytes = 0, errorMessage = "")
        val index = downloads.indexOfFirst { it.id == downloadId }
        if (index != -1) downloads[index] = reset
        val job = scope.launch { executeDownload(reset, "FamilyBrowser/1.0", null, null) }
        activeJobs[downloadId] = job
    }

    fun openFile(downloadId: String) {
        val item = downloads.find { it.id == downloadId } ?: return
        val file = File(item.localPath)
        if (!file.exists()) return

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun getOutputFile(item: DownloadItem): File {
        val dir = when {
            item.mimeType.startsWith("image/") ->
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            item.mimeType.startsWith("video/") ->
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            item.mimeType.startsWith("audio/") ->
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            else ->
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }.also { it.mkdirs() }

        var file = File(dir, item.fileName)
        var counter = 1
        while (file.exists() && item.downloadedBytes == 0L) {
            val dotIndex = item.fileName.lastIndexOf('.')
            val name = if (dotIndex != -1) item.fileName.substring(0, dotIndex) else item.fileName
            val ext = if (dotIndex != -1) item.fileName.substring(dotIndex) else ""
            file = File(dir, "${name}($counter)$ext")
            counter++
        }
        return file
    }

    private fun extractFileName(url: String): String {
        return try {
            val path = URL(url).path
            val name = path.substringAfterLast('/')
            if (name.isNotEmpty() && name.contains('.')) name
            else "download_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }

    private fun updateItem(id: String, transform: (DownloadItem) -> DownloadItem) {
        val index = downloads.indexOfFirst { it.id == id }
        if (index != -1) downloads[index] = transform(downloads[index])
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "File download progress"
                setShowBadge(false)
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification(item: DownloadItem) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(item.fileName)
                .setContentText("${item.progress}% • ${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}")
                .setProgress(100, item.progress.coerceAtLeast(0), item.progress < 0)
                .setOngoing(true)
                .setSilent(true)
                .build()
            NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
        } catch (e: SecurityException) { /* Permission not granted */ }
    }

    private fun showCompletedNotification(item: DownloadItem, file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val pendingIntent = PendingIntent.getActivity(
                context, item.id.hashCode(), openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download complete")
                .setContentText(item.fileName)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
        } catch (e: Exception) { }
    }

    private fun showErrorNotification(item: DownloadItem) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Download failed")
                .setContentText(item.fileName)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
        } catch (e: Exception) { }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 0 -> "?"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / 1024f / 1024f)} MB"
        else -> "${"%.2f".format(bytes / 1024f / 1024f / 1024f)} GB"
    }

    fun cleanup() { scope.cancel() }
}
