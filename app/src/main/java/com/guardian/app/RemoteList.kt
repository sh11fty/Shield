package com.guardian.app

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads public adult-domain lists (hosts-file or plain format), keeps them in app storage and feeds
 * them to the on-screen scanner. Refreshed weekly. A download that is suspiciously small is rejected,
 * so a tampered URL cannot silently shrink the list; changing the URL is gated by the PIN unlock.
 */
object RemoteList {
    private const val WEEK = 7 * 24 * 3_600_000L
    private val HOST = Regex("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$")
    private val SKIP = setOf("localhost", "localhost.localdomain", "local", "broadcasthost", "ip6-localhost", "0.0.0.0")
    private const val MAX_BYTES = 40L * 1024 * 1024

    private fun file(c: Context) = File(c.applicationContext.filesDir, "remote_domains.txt")
    @Volatile private var loadedStamp = -1L
    @Volatile private var lastAttempt = 0L

    /** Load from disk if needed, then download if the copy is older than a week. Returns a status line. */
    fun refresh(c: Context, force: Boolean = false): String {
        val ctx = c.applicationContext
        loadFromDisk(ctx)
        val now = System.currentTimeMillis()
        val due = force || (now - Prefs.remoteUpdatedAt(ctx) > WEEK && now - lastAttempt > 3_600_000L)
        if (due && Prefs.remoteUrls(ctx).isNotEmpty()) {
            lastAttempt = now
            val msg = download(ctx)
            loadFromDisk(ctx)
            return msg
        }
        return status(ctx)
    }

    fun status(c: Context): String =
        "Remote list: ${Prefs.remoteCount(c)} domains" +
            if (Prefs.remoteUpdatedAt(c) > 0) " (updated ${java.text.DateFormat.getDateInstance().format(java.util.Date(Prefs.remoteUpdatedAt(c)))})" else " (not downloaded yet)"

    private fun loadFromDisk(ctx: Context) {
        val f = file(ctx)
        if (!f.exists() || f.lastModified() == loadedStamp) return
        val set = HashSet<String>(f.length().toInt() / 12)
        f.forEachLine { l -> val d = l.trim(); if (d.isNotEmpty()) set.add(Matcher.normalize(d)) }
        Matcher.remote = set
        loadedStamp = f.lastModified()
    }

    private fun parse(line: String): String? {
        var l = line.substringBefore('#').trim().lowercase()
        if (l.isEmpty()) return null
        val parts = l.split(Regex("\\s+"))
        l = if (parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1" || parts[0] == "::1")) parts[1] else parts[0]
        l = l.removePrefix("www.").removePrefix("*.").removePrefix("||").removeSuffix("^").trimEnd('.')
        if (l in SKIP || !HOST.matches(l)) return null
        if (Blocklists.NEVER_BLOCK.any { l == it || l.endsWith(".$it") }) return null
        return l
    }

    private fun download(ctx: Context): String {
        val all = LinkedHashSet<String>()
        var failed = 0
        for (u in Prefs.remoteUrls(ctx)) {
            try {
                val conn = URL(u).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000; conn.readTimeout = 60000
                if (conn.responseCode != 200) { failed++; continue }
                var read = 0L
                conn.inputStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        read += line.length + 1
                        if (read > MAX_BYTES) break
                        parse(line)?.let { all.add(it) }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) { failed++ }
        }
        // subdomains are redundant (matching is by label suffix), so drop entries whose parent is listed
        val slim = all.filter { d ->
            var p = d.substringAfter('.', "")
            var covered = false
            while (p.contains('.')) { if (p in all) { covered = true; break }; p = p.substringAfter('.') }
            !covered
        }
        val prev = Prefs.remoteCount(ctx)
        if (slim.size < 1000 || (prev > 0 && slim.size < prev / 2)) {
            Alerts.event(ctx, "LIST_UPDATE_REJECTED", "got ${slim.size} domains (had $prev), failed=$failed")
            return "Remote list: update rejected (${slim.size} domains, previous $prev)"
        }
        val tmp = File(file(ctx).path + ".tmp")
        tmp.bufferedWriter().use { w -> for (d in slim) { w.write(d); w.newLine() } }
        tmp.renameTo(file(ctx))
        Prefs.setRemoteState(ctx, System.currentTimeMillis(), slim.size)
        return "Remote list: updated, ${slim.size} domains"
    }
}
