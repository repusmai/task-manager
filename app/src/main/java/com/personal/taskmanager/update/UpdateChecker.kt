package com.personal.taskmanager.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ReleaseInfo(val tagName: String, val apkUrl: String)

// ⚠️ Replace these with your actual GitHub username and repo name
private const val GITHUB_REPO = "repusmai/task-manager"

@Singleton
class UpdateChecker @Inject constructor(private val client: OkHttpClient) {

    suspend fun getLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val json = JSONObject(response.body?.string() ?: return@withContext null)
            val tag = json.getString("tag_name")
            val assets = json.getJSONArray("assets")
            if (assets.length() == 0) return@withContext null
            val apkUrl = assets.getJSONObject(0).getString("browser_download_url")
            ReleaseInfo(tag, apkUrl)
        } catch (e: Exception) {
            null
        }
    }

    fun isUpdateAvailable(context: Context, releaseInfo: ReleaseInfo): Boolean {
        return try {
            val current = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName
            releaseInfo.tagName.removePrefix("v") != current
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

@Singleton
class UpdateInstaller @Inject constructor() {

    suspend fun downloadApk(
        context: Context,
        client: OkHttpClient,
        url: String,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body ?: throw Exception("Empty response")
        val totalBytes = body.contentLength()

        val file = File(context.getExternalFilesDir(null), "update.apk")
        var downloaded = 0L

        file.outputStream().use { out ->
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    out.write(buffer, 0, bytes)
                    downloaded += bytes
                    if (totalBytes > 0) {
                        onProgress(((downloaded * 100) / totalBytes).toInt())
                    }
                }
            }
        }
        file
    }

    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
