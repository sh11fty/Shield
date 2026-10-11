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
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GuardAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastScan = 0L
    private var lastHitAt = 0L
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

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        e ?: return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName || pkg in IGNORED) return
        val now = SystemClock.uptimeMillis()
        if (now - lastScan < 350) return
        lastScan = now
        val root = rootInActiveWindow ?: return
        val sb = StringBuilder()
        collect(root, sb, intArrayOf(0), 0)
        val text = sb.toString()
        if (pkg in SETTINGS && Prefs.shield(this) && text.lowercase().contains("guardian")) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            Alerts.event(this, "TAMPER", "opened Guardian's own settings page")
            return
        }
        val hit = Matcher.evaluate(text, Prefs.domains(this), Prefs.strong(this), Prefs.mild(this), Prefs.threshold(this))
        if (hit != null) onHit(pkg, hit)
    }

    private fun collect(n: AccessibilityNodeInfo?, sb: StringBuilder, count: IntArray, depth: Int) {
        if (n == null || depth > 40 || count[0] > 900) return
        count[0]++
        n.text?.let { sb.append(it).append(' ') }
        n.contentDescription?.let { sb.append(it).append(' ') }
        for (i in 0 until n.childCount) collect(n.getChild(i), sb, count, depth + 1)
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

    /** Tripwire: if any VPN transport comes up, hide every VPN-capable app and tell the partner. */
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
