package com.evrenhouse.esteh.update

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
        if (!apiBase.startsWith("https://")) {
            return@withContext CheckResult.Failed("apiBase harus https")
        }
        var conn: HttpURLConnection? = null
        try {
            conn = (URL("$apiBase/repos/$repo/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (conn.responseCode != 200) return@withContext CheckResult.Failed("HTTP ${conn.responseCode}")
            val body = conn.inputStream.use { it.bufferedReader().readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name", "")
            if (tag.isBlank()) return@withContext CheckResult.Failed("Tag kosong")
            if (compareVersions(tag, currentVersion) <= 0) {
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
            if (apkUrl.isBlank()) {
                return@withContext CheckResult.Failed("APK tidak ditemukan di rilis $tag")
            }
            if (!apkUrl.startsWith("https://")) {
                return@withContext CheckResult.Failed("APK URL harus https")
            }
            CheckResult.Available(UpdateInfo(tag, apkUrl, json.optString("body", "")))
        } catch (e: Exception) {
            CheckResult.Failed(e.message ?: "unknown")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun checkBackend(
        baseUrl: String,
        currentVersion: String,
    ): CheckResult = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trimEnd('/') + "/api/ota/latest"
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode != 200) return@withContext CheckResult.Failed("HTTP ${conn.responseCode}")
            val body = conn.inputStream.use { it.bufferedReader().readText() }
            val json = JSONObject(body)
            val ota = json.optJSONObject("ota") ?: return@withContext CheckResult.Failed("Format respons ota tidak valid")
            val tag = ota.optString("tag", "")
            val apkUrl = ota.optString("apkUrl", "")
            val notes = ota.optString("notes", "")
            if (tag.isBlank() || apkUrl.isBlank()) return@withContext CheckResult.Failed("Data rilis tidak lengkap")
            if (compareVersions(tag, currentVersion) <= 0) {
                return@withContext CheckResult.UpToDate
            }
            CheckResult.Available(UpdateInfo(tag, apkUrl, notes))
        } catch (e: Exception) {
            CheckResult.Failed(e.message ?: "unknown")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun listenSse(
        baseUrl: String,
        currentVersion: String,
        onUpdate: suspend (UpdateInfo) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val sseUrl = baseUrl.trimEnd('/') + "/api/ota/stream"
        while (isActive) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(sseUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 60_000
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                if (conn.responseCode == 200) {
                    val reader = conn.inputStream.bufferedReader()
                    var currentEvent = ""
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        if (line.startsWith("event:")) {
                            currentEvent = line.substringAfter("event:").trim()
                        } else if (line.startsWith("data:")) {
                            val dataStr = line.substringAfter("data:").trim()
                            if (dataStr.isNotBlank() && (currentEvent == "ota" || currentEvent == "init")) {
                                try {
                                    val json = JSONObject(dataStr)
                                    val tag = json.optString("tag", "")
                                    val apkUrl = json.optString("apkUrl", "")
                                    val notes = json.optString("notes", "")
                                    if (tag.isNotBlank() && apkUrl.isNotBlank()) {
                                        if (compareVersions(tag, currentVersion) > 0) {
                                            onUpdate(UpdateInfo(tag, apkUrl, notes))
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            currentEvent = ""
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
            delay(15_000)
        }
    }

    /** -1/0/1. Pure, wajib di-cover unit test. */
    fun compareVersions(a: String, b: String): Int {
        val (pa, preA) = parseVersion(a)
        val (pb, preB) = parseVersion(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val d = (pa.getOrElse(i) { 0 }) - (pb.getOrElse(i) { 0 })
            if (d != 0) return if (d > 0) 1 else -1
        }
        // Basis sama: rilis > pre-release. Dua pre-release dibanding leksikal.
        if (preA == preB) return 0
        if (preA == null) return 1
        if (preB == null) return -1
        return preA.compareTo(preB).let { if (it > 0) 1 else -1 }
    }

    private fun parseVersion(v: String): Pair<List<Int>, String?> {
        // Toleran prefix v/V, abaikan build metadata +xxx.
        val s = v.trim().trimStart('v', 'V').split('+', limit = 2)[0]
        val coreAndPre = s.split('-', limit = 2)
        val nums = coreAndPre[0].split('.').map { it.toIntOrNull() ?: 0 }
        val pre = if (coreAndPre.size > 1) coreAndPre[1].takeIf { it.isNotEmpty() } else null
        return nums to pre
    }
}
