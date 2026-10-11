package com.guardian.app

import android.content.Context
import android.os.Build
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/** Local log + optional partner webhook (ntfy.sh topic URL, Discord or Slack webhook, or any URL that takes a POST). */
object Alerts {
    private val io = Executors.newSingleThreadExecutor()

    fun event(c: Context, type: String, detail: String) {
        val ctx = c.applicationContext
        val stamp = SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date())
        Prefs.appendLog(ctx, "$stamp $type: $detail")
        val url = Prefs.webhook(ctx)
        if (url.isBlank()) return
        val text = "[Guardian ${Build.MODEL}] $type: $detail"
        io.execute {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "POST"
                conn.doOutput = true
                val body: String
                when {
                    url.contains("discord.com/api/webhooks") || url.contains("discordapp.com/api/webhooks") -> {
                        conn.setRequestProperty("Content-Type", "application/json")
                        body = "{\"content\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}"
                    }
                    url.contains("hooks.slack.com") -> {
                        conn.setRequestProperty("Content-Type", "application/json")
                        body = "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}"
                    }
                    else -> {
                        conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                        conn.setRequestProperty("Title", "Guardian: $type")
                        body = text
                    }
                }
                conn.outputStream.use { it.write(body.toByteArray()) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {
            }
        }
    }
}
