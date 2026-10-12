package com.guardian.app

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

object Prefs {
    const val K_DOMAINS = "extra_domains"
    const val K_STRONG = "extra_strong"
    const val K_MILD = "extra_mild"
    const val K_HIDE = "extra_hide"
    const val K_BROWSER = "extra_browser_allow"
    const val K_CURFEW_APPS = "extra_curfew_apps"

    const val DEFAULT_DNS = "adult-filter-dns.cleanbrowsing.org"

    private fun sp(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences("guardian", Context.MODE_PRIVATE)

    // ---- lists (user extras on top of built-ins) ----
    fun list(c: Context, key: String): List<String> =
        (sp(c).getString(key, "") ?: "").split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    fun setList(c: Context, key: String, v: Collection<String>) {
        sp(c).edit().putString(key, v.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString("\n")).commit()
    }

    fun domains(c: Context) = Blocklists.DOMAINS + list(c, K_DOMAINS)
    fun strong(c: Context) = Blocklists.STRONG + list(c, K_STRONG)
    fun mild(c: Context) = Blocklists.MILD + list(c, K_MILD)
    fun browserAllow(c: Context) = Blocklists.BROWSER_ALLOW + list(c, K_BROWSER)
    fun curfewApps(c: Context) = Blocklists.CURFEW_APPS + list(c, K_CURFEW_APPS)

    // ---- scalars ----
    private fun s(c: Context, k: String, d: String) = sp(c).getString(k, d) ?: d
    private fun i(c: Context, k: String, d: Int) = sp(c).getInt(k, d)
    private fun b(c: Context, k: String, d: Boolean) = sp(c).getBoolean(k, d)
    private fun put(c: Context, f: SharedPreferences.Editor.() -> Unit) = sp(c).edit().apply(f).commit()

    fun dnsHost(c: Context) = s(c, "dns_host", DEFAULT_DNS)
    fun setDnsHost(c: Context, v: String) = put(c) { putString("dns_host", v.trim().ifEmpty { DEFAULT_DNS }) }
    fun threshold(c: Context) = i(c, "threshold", 3)
    fun setThreshold(c: Context, v: Int) = put(c) { putInt("threshold", v.coerceIn(1, 10)) }
    fun strict(c: Context) = b(c, "strict", false)
    fun setStrict(c: Context, v: Boolean) = put(c) { putBoolean("strict", v) }
    fun scanText(c: Context) = b(c, "scan_text", false)
    fun setScanText(c: Context, v: Boolean) = put(c) { putBoolean("scan_text", v) }
    fun dnsBad(c: Context) = i(c, "dns_bad", 0)
    fun setDnsBad(c: Context, v: Int) = put(c) { putInt("dns_bad", v) }
    fun dnsFallbackUntil(c: Context) = sp(c).getLong("dns_fallback_until", 0L)
    fun setDnsFallbackUntil(c: Context, v: Long) = put(c) { putLong("dns_fallback_until", v) }
    fun hideConfirmed(c: Context) = b(c, "hide_confirmed", false)
    fun setHideConfirmed(c: Context, v: Boolean) = put(c) { putBoolean("hide_confirmed", v) }
    fun shield(c: Context) = b(c, "shield", false)
    fun setShield(c: Context, v: Boolean) = put(c) { putBoolean("shield", v) }
    fun curfewOn(c: Context) = b(c, "curfew_on", true)
    fun setCurfewOn(c: Context, v: Boolean) = put(c) { putBoolean("curfew_on", v) }
    fun curfewStart(c: Context) = i(c, "curfew_start", 23)
    fun curfewEnd(c: Context) = i(c, "curfew_end", 6)
    fun setCurfew(c: Context, start: Int, end: Int) =
        put(c) { putInt("curfew_start", start.coerceIn(0, 23)); putInt("curfew_end", end.coerceIn(0, 23)) }
    const val DEFAULT_REMOTE = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn/hosts"
    fun remoteUrls(c: Context): List<String> =
        s(c, "remote_urls", DEFAULT_REMOTE).split('\n').map { it.trim() }.filter { it.startsWith("https://") }
    fun remoteUrlsRaw(c: Context) = s(c, "remote_urls", DEFAULT_REMOTE)
    fun setRemoteUrls(c: Context, v: String) = put(c) { putString("remote_urls", v.trim()) }
    fun remoteUpdatedAt(c: Context) = sp(c).getLong("remote_updated", 0L)
    fun remoteCount(c: Context) = i(c, "remote_count", 0)
    fun setRemoteState(c: Context, at: Long, count: Int) = put(c) { putLong("remote_updated", at); putInt("remote_count", count) }

    // ---- state ----
    fun hiddenByUs(c: Context): Set<String> = sp(c).getStringSet("hidden_by_us", emptySet())!!.toSet()
    fun setHiddenByUs(c: Context, v: Set<String>) = put(c) { putStringSet("hidden_by_us", v) }
    fun dnsEverSet(c: Context) = b(c, "dns_set", false)
    fun setDnsEverSet(c: Context) = put(c) { putBoolean("dns_set", true) }
    fun a11yEverOn(c: Context) = b(c, "a11y_on", false)
    fun setA11yEverOn(c: Context) = put(c) { putBoolean("a11y_on", true) }
    fun introDone(c: Context) = b(c, "intro_done", false)
    fun setIntroDone(c: Context) = put(c) { putBoolean("intro_done", true) }
    fun lastReport(c: Context) = s(c, "report", "(not applied yet)")
    fun setLastReport(c: Context, v: String) = put(c) { putString("report", v) }

    fun recordHit(c: Context) = put(c) {
        putLong("last_hit", System.currentTimeMillis()); putInt("hits", i(c, "hits", 0) + 1)
    }
    fun lastHit(c: Context) = sp(c).getLong("last_hit", 0L)
    fun hits(c: Context) = i(c, "hits", 0)

    fun log(c: Context): List<String> = s(c, "log", "").split('\n').filter { it.isNotBlank() }
    @Synchronized
    fun appendLog(c: Context, line: String) {
        val l = (log(c) + line).takeLast(60)
        put(c) { putString("log", l.joinToString("\n")) }
    }

    // ---- PIN / unlock gate ----
    fun hasPin(c: Context) = s(c, "pin_hash", "").isNotEmpty()
    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }

    fun setPin(c: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        put(c) { putString("pin_salt", salt); putString("pin_hash", hash(salt, pin)) }
    }
    fun checkPin(c: Context, pin: String): Boolean =
        hasPin(c) && hash(s(c, "pin_salt", ""), pin) == s(c, "pin_hash", "")

    fun readyAt(c: Context) = sp(c).getLong("ready_at", 0L)
    fun unlockedUntil(c: Context) = sp(c).getLong("unlocked_until", 0L)
    fun setUnlock(c: Context, readyAt: Long, unlockedUntil: Long) =
        put(c) { putLong("ready_at", readyAt); putLong("unlocked_until", unlockedUntil) }
}
