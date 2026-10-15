package com.evrenhouse.esteh.update

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * OTA update checker: bandingkan versionName aplikasi dengan tag rilis
 * terbaru di GitHub Releases publik. Pola "Update Checker" kasir-mobile.
 */
object UpdateChecker {

    data class UpdateInfo(val latestTag: String, val apkUrl: String, val notes: String)

    sealed interface CheckResult {
        data object UpToDate : CheckResult
        data class Available(val info: UpdateInfo) : CheckResult
        data class Failed(val message: String) : CheckResult
    }

    suspend fun check(
        repo: String,
        currentVersion: String,
        apiBase: String = "https://api.github.com",
    ): CheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("$apiBase/repos/$repo/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (conn.responseCode != 200) return@withContext CheckResult.Failed("HTTP ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = json.optString("tag_name", "")
            if (tag.isBlank()) return@withContext CheckResult.Failed("Tag kosong")
            if (compareVersions(tag.trimStart('v'), currentVersion) <= 0) {
                return@withContext CheckResult.UpToDate
            }
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val name = a.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url", "")
                        break
                    }
                }
            }
            CheckResult.Available(UpdateInfo(tag, apkUrl, json.optString("body", "")))
        } catch (e: Exception) {
            CheckResult.Failed(e.message ?: "unknown")
        }
    }

    /** -1/0/1. Pure, wajib di-cover unit test. */
    fun compareVersions(a: String, b: String): Int {
        val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val d = (pa.getOrElse(i) { 0 }) - (pb.getOrElse(i) { 0 })
            if (d != 0) return if (d > 0) 1 else -1
        }
        return 0
    }
}
