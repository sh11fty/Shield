package com.guardian.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import java.util.concurrent.Executors

/** Applies every policy. Idempotent; called on boot, every ~5 min, on package installs and on tamper signals. */
object Enforcer {
    private val worker = Executors.newSingleThreadExecutor()
    private val lock = Any()

    private const val R_VPN = "no_config_vpn"
    private const val R_DNS = "no_config_private_dns"
    private const val R_UNKNOWN = "no_install_unknown_sources"
    private const val R_UNKNOWN_G = "no_install_unknown_sources_globally"
    private const val R_SAFE = "no_safe_boot"
    private const val R_USER = "no_add_user"
    private const val R_PROFILE = "no_add_managed_profile"
    private const val R_TIME = "no_config_date_time"
    private const val R_RESET = "no_factory_reset"
    private const val R_ADB = "no_debugging_features"
    private const val R_CTRL = "no_control_apps"
    private val MANAGED_RESTRICTIONS =
        listOf(R_VPN, R_DNS, R_UNKNOWN, R_UNKNOWN_G, R_SAFE, R_USER, R_PROFILE, R_TIME, R_RESET, R_ADB, R_CTRL)

    fun admin(c: Context) = ComponentName(c.applicationContext, GuardianAdminReceiver::class.java)
    private fun dpm(c: Context) = c.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    fun isOwner(c: Context) = dpm(c).isDeviceOwnerApp(c.applicationContext.packageName)

    fun applyAsync(c: Context, why: String) {
        val ctx = c.applicationContext
        worker.execute { try { apply(ctx, why) } catch (t: Throwable) { Prefs.appendLog(ctx, "apply failed: ${t.message}") } }
    }

    fun apply(c: Context, why: String = ""): String = synchronized(lock) {
        val ctx = c.applicationContext
        val out = StringBuilder()
        out.append(ensureAccessibility(ctx)).append('\n')
        if (!isOwner(ctx)) {
            out.append("NOT device owner: hiding apps, VPN/DNS locks and browser policies are inactive.")
            Prefs.setLastReport(ctx, out.toString())
            RemoteList.refresh(ctx)
            return out.toString()
        }
        val d = dpm(ctx)
        val a = admin(ctx)
        runCatching { d.setUninstallBlocked(a, ctx.packageName, true) }
        out.append(restrictions(ctx, d, a)).append('\n')
        out.append(privateDns(ctx, d, a)).append('\n')
        out.append(hideApps(ctx, d, a)).append('\n')
        out.append(browserPolicies(ctx, d, a))
        Prefs.setLastReport(ctx, out.toString())
        RemoteList.refresh(ctx)
        out.toString()
    }

    // ---------- accessibility self-heal ----------
    private fun a11yEnabled(ctx: Context): Boolean {
        val cr = ctx.contentResolver
        val cur = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val ours = ComponentName(ctx, GuardAccessibilityService::class.java)
        val listed = cur.split(':').any { ComponentName.unflattenFromString(it) == ours }
        return listed && Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
    }

    fun a11yOk(ctx: Context) = runCatching { a11yEnabled(ctx) }.getOrDefault(false)

    private fun ensureAccessibility(ctx: Context): String {
        if (a11yOk(ctx)) { Prefs.setA11yEverOn(ctx); return "Accessibility: ON" }
        if (Prefs.a11yEverOn(ctx)) Alerts.event(ctx, "A11Y_OFF", "accessibility service was disabled")
        val ours = ComponentName(ctx, GuardAccessibilityService::class.java).flattenToString()
        runCatching {
            if (isOwner(ctx)) dpm(ctx).setPermissionGrantState(
                admin(ctx), ctx.packageName, "android.permission.WRITE_SECURE_SETTINGS",
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            )
        }
        return try {
            val cr = ctx.contentResolver
            val cur = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            val next = if (cur.isEmpty()) ours else "$cur:$ours"
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, next)
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            Prefs.setA11yEverOn(ctx)
            "Accessibility: re-enabled"
        } catch (e: SecurityException) {
            "Accessibility: OFF (grant once: adb shell pm grant ${ctx.packageName} android.permission.WRITE_SECURE_SETTINGS)"
        }
    }

    // ---------- user restrictions ----------
    private fun restrictions(ctx: Context, d: DevicePolicyManager, a: ComponentName): String {
        val want = mutableSetOf(R_VPN, R_DNS, R_UNKNOWN, R_UNKNOWN_G, R_SAFE, R_USER, R_PROFILE)
        if (Prefs.lockTime(ctx)) {
            want += R_TIME
            runCatching {
                if (Build.VERSION.SDK_INT >= 30) {
                    d.setAutoTimeEnabled(a, true)
                } else {
                    @Suppress("DEPRECATION")
                    d.setAutoTimeRequired(a, true)
                }
            }
        }
        if (Prefs.strict(ctx)) want += listOf(R_RESET, R_ADB, R_CTRL)
        for (r in MANAGED_RESTRICTIONS) runCatching { if (r in want) d.addUserRestriction(a, r) else d.clearUserRestriction(a, r) }
        return "Restrictions: ${want.size} active (VPN config, DNS config, sideloading, safe mode, extra users${if (Prefs.lockTime(ctx)) ", date/time" else ""})"
    }

    // ---------- private DNS ----------
    private fun privateDns(ctx: Context, d: DevicePolicyManager, a: ComponentName): String {
        val host = Prefs.dnsHost(ctx)
        val cr = ctx.contentResolver
        val mode = Settings.Global.getString(cr, "private_dns_mode")
        val spec = Settings.Global.getString(cr, "private_dns_specifier")
        if (mode == "hostname" && spec == host) { Prefs.setDnsEverSet(ctx); return "DNS: locked to $host" }
        if (Prefs.dnsEverSet(ctx)) Alerts.event(ctx, "DNS_TAMPER", "private DNS changed to ${mode}/${spec}; restoring")
        return try {
            val r = d.setGlobalPrivateDnsModeSpecifiedHost(a, host)
            if (r == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) { Prefs.setDnsEverSet(ctx); "DNS: set to $host" }
            else "DNS: failed to set $host (code $r, will retry)"
        } catch (t: Throwable) { "DNS: error ${t.message}" }
    }

    // ---------- hide apps ----------
    private fun isSystemApp(i: ApplicationInfo) =
        (i.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && (i.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

    private fun neverHide(ctx: Context, pkg: String): Boolean {
        if (pkg == ctx.packageName || pkg == "android") return true
        return Blocklists.NEVER_HIDE_PREFIXES.any { it != "android" && (pkg == it || pkg.startsWith(if (it.endsWith(".")) it else "$it.")) }
    }

    private fun desiredHidden(ctx: Context): Pair<Set<String>, Map<String, String>> {
        val pm = ctx.packageManager
        val reasons = LinkedHashMap<String, String>()
        fun add(p: String, why: String) { if (!neverHide(ctx, p)) reasons.putIfAbsent(p, why) }

        @Suppress("QueryPermissionsNeeded")
        val apps = pm.getInstalledApplications(0)
        val labels = HashMap<String, String>()
        for (ai in apps) labels[ai.packageName] = runCatching { pm.getApplicationLabel(ai).toString() }.getOrDefault(ai.packageName)

        // explicit social apps (installed or not: hidden state sticks once installed)
        for (p in Prefs.list(ctx, Prefs.K_HIDE)) add(p, "listed")
        for (ai in apps) {
            val p = ai.packageName
            val label = labels[p] ?: p
            if (Blocklists.HIDE_PACKAGE_PREFIXES.any { p.startsWith(it) }) add(p, "telegram/x")
            else if (!isSystemApp(ai) && Blocklists.HIDE_LABEL_REGEX.containsMatchIn(label)) add(p, "telegram/x")
        }

        // VPN: anything that declares a VpnService, plus name/package heuristics
        runCatching {
            for (ri in pm.queryIntentServices(Intent("android.net.VpnService"), 0)) {
                val p = ri.serviceInfo.packageName
                val ai = apps.firstOrNull { it.packageName == p }
                if (ai == null || !isSystemApp(ai)) add(p, "vpn")
            }
        }
        for (ai in apps) {
            if (isSystemApp(ai)) continue
            val p = ai.packageName
            val label = labels[p] ?: p
            if (Blocklists.VPN_REGEX.containsMatchIn(label) || Blocklists.VPN_REGEX.containsMatchIn(p)) add(p, "vpn-name")
            if (Blocklists.CLONE_REGEX.containsMatchIn(label) || Blocklists.CLONE_REGEX.containsMatchIn(p)) add(p, "cloner")
        }

        // other browsers (no managed policy possible) and curfew
        val allow = Prefs.browserAllow(ctx).toSet()
        val curfew = Curfew.active(ctx)
        val browsers = runCatching {
            val i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
            pm.queryIntentActivities(i, PackageManager.MATCH_ALL).map { it.activityInfo.packageName }.toSet()
        }.getOrDefault(emptySet())
        val viewers = runCatching {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)
            pm.queryIntentActivities(i, PackageManager.MATCH_ALL).map { it.activityInfo.packageName }.toSet()
        }.getOrDefault(emptySet())
        for (p in browsers) {
            if (p !in allow) add(p, "browser")
            else if (curfew) add(p, "curfew")
        }
        // Chromium shells that only appear as VIEW handlers with a launcher icon and "browser" label
        for (p in viewers) {
            val l = (labels[p] ?: "").lowercase()
            if (p !in allow && !neverHide(ctx, p) && (l.contains("browser") || l.contains("firefox") || l.contains("opera") || l.contains("duckduckgo"))) add(p, "browser")
        }
        if (curfew) for (p in Prefs.curfewApps(ctx)) add(p, "curfew")
        return reasons.keys to reasons
    }

    private fun hideApps(ctx: Context, d: DevicePolicyManager, a: ComponentName): String {
        val (want, reasons) = desiredHidden(ctx)
        val mine = Prefs.hiddenByUs(ctx).toMutableSet()
        var newlyHidden = 0
        for (p in want) {
            val ok = runCatching { d.setApplicationHidden(a, p, true) }.getOrDefault(false)
            if (ok && mine.add(p)) {
                newlyHidden++
                val why = reasons[p]
                if (why == "vpn" || why == "vpn-name" || why == "cloner") Alerts.event(ctx, "APP_HIDDEN", "$p ($why)")
            }
        }
        for (p in mine - want) {
            if (runCatching { d.setApplicationHidden(a, p, false) }.getOrDefault(false)) mine.remove(p)
        }
        Prefs.setHiddenByUs(ctx, mine)
        return "Apps hidden: ${mine.size} (+$newlyHidden new)${if (Curfew.active(ctx)) " [CURFEW ACTIVE]" else ""}"
    }

    // ---------- browser managed policies ----------
    private fun browserPolicies(ctx: Context, d: DevicePolicyManager, a: ComponentName): String {
        val urls = Prefs.domains(ctx).map { it.trim().removePrefix("www.") }.distinct().take(990).toTypedArray()
        for (p in Blocklists.MANAGED_BROWSERS) {
            val b = Bundle().apply {
                putStringArray("URLBlocklist", urls)
                putInt("IncognitoModeAvailability", 1)
                putBoolean("ForceGoogleSafeSearch", true)
                putInt("ForceYouTubeRestrict", 2)
                putInt("SafeSitesFilterBehavior", 1)
                putString("DnsOverHttpsMode", "off")
                putBoolean("AllowDeletingBrowserHistory", false)
                putInt("DeveloperToolsAvailability", 2)
            }
            runCatching { d.setApplicationRestrictions(a, p, b) }
        }
        return "Browser policy: ${urls.size} blocked domains, incognito off, SafeSearch+SafeSites on, DoH off, history delete off"
    }

    // ---------- status ----------
    fun status(ctx: Context): String {
        val own = isOwner(ctx)
        return buildString {
            append("Device owner: ").append(if (own) "YES" else "NO").append('\n')
            append("Accessibility: ").append(if (a11yOk(ctx)) "ON" else "OFF").append('\n')
            append("Curfew: ").append(if (Prefs.curfewOn(ctx)) "${Prefs.curfewStart(ctx)}:00-${Prefs.curfewEnd(ctx)}:00" + if (Curfew.active(ctx)) " (ACTIVE)" else "" else "off").append('\n')
            append("Blocks so far: ").append(Prefs.hits(ctx)).append('\n')
            append("Last report:\n").append(Prefs.lastReport(ctx))
        }
    }

    // ---------- release (only reachable through the Gate) ----------
    fun releaseAll(ctx: Context): String {
        if (!isOwner(ctx)) return "Not device owner."
        val d = dpm(ctx)
        val a = admin(ctx)
        for (r in MANAGED_RESTRICTIONS) runCatching { d.clearUserRestriction(a, r) }
        for (p in Prefs.hiddenByUs(ctx)) runCatching { d.setApplicationHidden(a, p, false) }
        Prefs.setHiddenByUs(ctx, emptySet())
        for (p in Blocklists.MANAGED_BROWSERS) runCatching { d.setApplicationRestrictions(a, p, Bundle()) }
        runCatching { d.setUninstallBlocked(a, ctx.packageName, false) }
        runCatching { if (Build.VERSION.SDK_INT >= 29) d.setGlobalPrivateDnsModeOpportunistic(a) }
        Alerts.event(ctx, "RELEASED", "device owner removed")
        return runCatching { d.clearDeviceOwnerApp(ctx.packageName); "Released. You can now uninstall Guardian." }
            .getOrElse { "Failed: ${it.message}" }
    }
}
