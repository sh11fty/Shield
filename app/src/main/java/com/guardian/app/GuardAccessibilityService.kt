package com.guardian.app

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GuardAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = HandlerThread("guard-scan").apply { start() }
    private val bg = Handler(worker.looper)
    @Volatile private var scanning = false
    private var lastScan = 0L
    private var lastHitAt = 0L
    private var cfgAt = 0L
    private var domains: List<String> = emptyList()
    private var strong: List<String> = emptyList()
    private var mild: List<String> = emptyList()
    private var threshold = 3
    private var netCb: ConnectivityManager.NetworkCallback? = null

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Enforcer.applyAsync(applicationContext, "package change")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        runCatching {
            registerReceiver(pkgReceiver, IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply { addDataScheme("package") })
        }
        watchVpn()
        Scheduler.scheduleAll(this)
        Enforcer.applyAsync(this, "a11y connected")
    }

    /** Runs on the main thread, so it only does cheap checks; the real scan happens on a background thread. */
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        e ?: return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName || pkg in IGNORED) return
        val type = e.eventType
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && isBlockedApp(pkg)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            val t = System.currentTimeMillis()
            if (t - lastHitAt >= 4000) {
                lastHitAt = t
                Prefs.recordHit(this)
                Alerts.event(this, "BLOCKED", "app opened: $pkg")
                showBlock("${labelOf(pkg)} is blocked")
            }
            return
        }
        // Light mode: only app-open blocking and the Settings tamper check; no page-text reading.
        val light = !Prefs.scanText(this)
        if (light && !(type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg in SETTINGS && Prefs.shield(this))) return
        val now = SystemClock.uptimeMillis()
        val gap = if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) 500L else 2500L
        if (scanning || now - lastScan < gap) return
        lastScan = now
        scanning = true
        bg.post {
            try { scan(pkg) } catch (_: Throwable) { } finally { scanning = false }
        }
    }

    private val blockedCache = HashMap<String, Boolean>()

    /** Package match first, then the app's visible label, so renamed mods are still caught. Cached per package. */
    private fun isBlockedApp(pkg: String): Boolean = blockedCache.getOrPut(pkg) {
        if (Blocklists.NEVER_HIDE_PREFIXES.any { pkg == it || pkg.startsWith(if (it.endsWith(".")) it else "$it.") }) return@getOrPut false
        if (Prefs.list(this, Prefs.K_HIDE).contains(pkg)) return@getOrPut true
        if (Blocklists.HIDE_PACKAGE_PREFIXES.any { pkg.startsWith(it) } || Blocklists.OPEN_BLOCK_REGEX.containsMatchIn(pkg)) return@getOrPut true
        Blocklists.HIDE_LABEL_REGEX.containsMatchIn(labelOf(pkg))
    }

    private fun labelOf(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun refreshConfig() {
        val now = System.currentTimeMillis()
        if (now - cfgAt < 30_000L && domains.isNotEmpty()) return
        domains = Prefs.domains(this); strong = Prefs.strong(this); mild = Prefs.mild(this); threshold = Prefs.threshold(this)
        cfgAt = now
    }

    private fun scan(pkg: String) {
        val root = rootInActiveWindow ?: return
        val sb = StringBuilder()
        collect(root, sb, intArrayOf(0), 0, SystemClock.uptimeMillis() + 150L)
        val text = sb.toString()
        if (pkg in SETTINGS && Prefs.shield(this) && text.lowercase().contains("guardian")) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            Alerts.event(this, "TAMPER", "opened Guardian's own settings page")
            return
        }
        if (!Prefs.scanText(this)) return
        refreshConfig()
        val hit = Matcher.evaluate(text, domains, strong, mild, threshold)
        if (hit != null) onHit(pkg, hit)
    }

    /** Bounded walk (each child lookup is a cross-process call): at most 300 nodes, depth 25, 150 ms. */
    private fun collect(n: AccessibilityNodeInfo?, sb: StringBuilder, count: IntArray, depth: Int, deadline: Long) {
        if (n == null || depth > 25 || count[0] > 300 || sb.length > 10_000 || SystemClock.uptimeMillis() > deadline) return
        count[0]++
        n.text?.let { sb.append(it).append(' ') }
        n.contentDescription?.let { sb.append(it).append(' ') }
        for (i in 0 until n.childCount) collect(n.getChild(i), sb, count, depth + 1, deadline)
    }

    private fun onHit(pkg: String, hit: Hit) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        val now = System.currentTimeMillis()
        if (now - lastHitAt < 4000) return
        lastHitAt = now
        Prefs.recordHit(this)
        Alerts.event(this, "BLOCKED", "${hit.kind} in $pkg")
        showBlock("Blocked ${hit.kind} in ${pkg.substringAfterLast('.')}")
    }

    private fun showBlock(reason: String) {
        handler.postDelayed({
            runCatching {
                startActivity(
                    Intent(this, BlockActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra("reason", reason)
                )
            }
        }, 200)
    }

    /** Tripwire: if any VPN transport comes up, hide every VPN-capable app. */
    private fun watchVpn() {
        runCatching {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val req = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Alerts.event(applicationContext, "VPN_ACTIVE", "a VPN connection came up; hiding VPN apps")
                    Enforcer.applyAsync(applicationContext, "vpn up")
                    showBlock("VPN detected")
                }
            }
            cm.registerNetworkCallback(req, cb)
            netCb = cb
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        runCatching { unregisterReceiver(pkgReceiver) }
        runCatching { worker.quitSafely() }
        runCatching { netCb?.let { (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(it) } }
        Alerts.event(applicationContext, "A11Y_STOPPED", "accessibility service stopped")
        Scheduler.scheduleAlarm(applicationContext)
        super.onDestroy()
    }

    companion object {
        private val IGNORED = setOf("com.android.systemui")
        private val SETTINGS = setOf(
            "com.android.settings", "com.google.android.permissioncontroller", "com.android.permissioncontroller",
            "com.samsung.android.settings", "com.android.packageinstaller", "com.google.android.packageinstaller"
        )
    }
}
