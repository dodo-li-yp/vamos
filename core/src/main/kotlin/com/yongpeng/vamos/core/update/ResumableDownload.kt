// Modified by Vamos (2026): bounded, resumable and verified update downloads.
package com.yongpeng.vamos.core.update

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class ResumableDownload(private val client: OkHttpClient) {
    data class Progress(val bytes: Long, val total: Long, val bytesPerSecond: Long)
    enum class Result { Ready, Paused }
    private class Paused : RuntimeException()

    fun download(v: CosVersion, destination: File, paused: () -> Boolean = { false }, progress: (Progress) -> Unit = {}): Result {
        SignedVersion.requireHttps(v.apkUrl)
        val part = File(destination.path + ".part"); val etag = File(destination.path + ".etag")
        destination.parentFile?.mkdirs()
        if (destination.exists()) {
            if (verified(destination, v)) return Result.Ready
            destination.delete()
        }
        require(v.size <= Long.MAX_VALUE / 2 && destination.parentFile!!.usableSpace >= v.size * 2) { "存储空间不足，请清理后重试" }
        for (attempt in 0..3) {
            try {
                if (paused()) return Result.Paused
                if (part.length() == v.size && verified(part, v)) { check(part.renameTo(destination)); etag.delete(); return Result.Ready }
                val tag = etag.takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() && !it.startsWith("W/") }
                var offset = if (part.isFile && tag != null && part.length() < v.size) part.length() else 0L
                val request = Request.Builder().url(v.apkUrl).header("Accept-Encoding", "identity").apply {
                    if (offset > 0) { header("Range", "bytes=$offset-"); header("If-Range", tag!!) }
                }.build()
                client.newCall(request).execute().use { response ->
                    if (response.code !in listOf(200, 206)) throw IOException("HTTP ${response.code}")
                    val returnedTag = response.header("ETag")
                    if (response.code == 206) {
                        val range = Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(response.header("Content-Range").orEmpty())
                        val bounds = range?.groupValues?.drop(1)?.map { it.toLongOrNull() }
                        if (offset == 0L || returnedTag != tag || bounds != listOf(offset, v.size - 1, v.size)) {
                            part.delete(); etag.delete(); throw IOException("Invalid resume response")
                        }
                    } else offset = 0
                    returnedTag?.let { etag.writeText(it) } ?: etag.delete()
                    val started = System.nanoTime(); var bytes = offset
                    val body = response.body ?: throw IOException("Empty APK response")
                    body.byteStream().use { input ->
                        java.io.FileOutputStream(part, offset > 0).use { output ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                if (paused() || Thread.currentThread().isInterrupted) throw Paused()
                                val n = input.read(buffer); if (n < 0) break
                                bytes += n
                                if (bytes > v.size) throw SecurityException("安装包大小不符")
                                output.write(buffer, 0, n)
                                progress(Progress(bytes, v.size, (bytes - offset) * 1_000_000_000L / (System.nanoTime() - started).coerceAtLeast(1)))
                            }
                        }
                    }
                }
                if (part.length() < v.size) throw IOException("Incomplete APK response")
                if (!verified(part, v)) throw SecurityException("安装包校验失败")
                check(part.renameTo(destination)); etag.delete(); return Result.Ready
            } catch (_: Paused) { return Result.Paused
            } catch (e: SecurityException) { part.delete(); etag.delete(); destination.delete(); throw e
            } catch (e: IOException) {
                if (paused()) return Result.Paused
                if (attempt == 3) throw e
                Thread.sleep(500L shl attempt)
            }
        }
        error("unreachable")
    }
    companion object {
        fun verified(file: File, v: CosVersion): Boolean {
            if (!file.isFile || file.length() != v.size) return false
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input -> val b = ByteArray(65536); while(true) { val n = input.read(b); if(n < 0) break; digest.update(b,0,n) } }
            return digest.digest().joinToString("") { "%02x".format(it) }.equals(v.sha256, true)
        }
    }
}
