package com.guardian.app

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.TimeZone

/**
 * Time taken from the internet (UK NTP pool, with fallbacks), NOT from the phone's clock, so changing the
 * phone's date/time cannot shorten the unlock wait or dodge the curfew. Between syncs it advances with the
 * monotonic clock; after a reboot it is unknown until the next successful sync.
 */
object TrustedTime {
    val UK: TimeZone = TimeZone.getTimeZone("Europe/London")
    private val NTP_HOSTS = listOf("uk.pool.ntp.org", "0.uk.pool.ntp.org", "time.cloudflare.com", "time.google.com")
    private const val STALE_MS = 30 * 60_000L

    private fun sp(c: Context) = c.applicationContext.getSharedPreferences("guardian_time", Context.MODE_PRIVATE)
    private fun bootCount(c: Context) = Settings.Global.getInt(c.contentResolver, Settings.Global.BOOT_COUNT, -1)

    /** Verified current time in epoch millis, or null if not verified since the last boot. */
    fun now(c: Context): Long? {
        val s = sp(c)
        val t = s.getLong("t", 0L)
        if (t == 0L || s.getInt("b", -2) != bootCount(c)) return null
        val el = SystemClock.elapsedRealtime()
        val e = s.getLong("e", 0L)
        return if (el < e) null else t + (el - e)
    }

    fun syncIfStale(c: Context) {
        val s = sp(c)
        val age = SystemClock.elapsedRealtime() - s.getLong("e", 0L)
        if (now(c) == null || age > STALE_MS) sync(c)
    }

    /** Network call: run off the main thread. */
    fun sync(c: Context): Boolean {
        val t = NTP_HOSTS.firstNotNullOfOrNull { ntp(it) } ?: httpDate() ?: return false
        val e = SystemClock.elapsedRealtime()
        sp(c).edit().putLong("t", t).putLong("e", e).putInt("b", bootCount(c)).apply()
        val off = System.currentTimeMillis() - t
        if (kotlin.math.abs(off) > 120_000L) Alerts.event(c, "CLOCK_OFF", "phone clock differs from internet time by ${off / 60000} min (ignored)")
        return true
    }

    private fun ntp(host: String): Long? = try {
        val addr = InetAddress.getByName(host)
        DatagramSocket().use { s ->
            s.soTimeout = 3000
            val buf = ByteArray(48)
            buf[0] = 0x1B
            val t0 = SystemClock.elapsedRealtime()
            s.send(DatagramPacket(buf, buf.size, addr, 123))
            val resp = DatagramPacket(ByteArray(48), 48)
            s.receive(resp)
            val rtt = SystemClock.elapsedRealtime() - t0
            val b = resp.data
            var secs = 0L
            for (i in 40..43) secs = (secs shl 8) or (b[i].toLong() and 0xFF)
            var frac = 0L
            for (i in 44..47) frac = (frac shl 8) or (b[i].toLong() and 0xFF)
            if (secs == 0L) null else (secs - 2208988800L) * 1000 + ((frac * 1000) shr 32) + rtt / 2
        }
    } catch (_: Exception) { null }

    private fun httpDate(): Long? = try {
        val conn = URL("https://www.gov.uk").openConnection() as HttpURLConnection
        conn.requestMethod = "HEAD"; conn.connectTimeout = 6000; conn.readTimeout = 6000
        val d = conn.getHeaderFieldDate("Date", 0L)
        conn.disconnect()
        if (d == 0L) null else d
    } catch (_: Exception) { null }
}
