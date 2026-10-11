package com.guardian.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var heroTitle: TextView
    private lateinit var heroSub: TextView
    private lateinit var statusBox: LinearLayout
    private lateinit var gateText: TextView
    private lateinit var logText: TextView
    private lateinit var pinField: EditText

    private lateinit var swStrict: Switch
    private lateinit var swShield: Switch
    private lateinit var swCurfew: Switch
    private lateinit var etCurfewStart: EditText
    private lateinit var etCurfewEnd: EditText
    private lateinit var etThreshold: EditText
    private lateinit var etDns: EditText
    private lateinit var etRemote: EditText

    private val setupCmds: String
        get() = "adb shell dpm set-device-owner $packageName/com.guardian.app.GuardianAdminReceiver\n" +
            "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        if (!Prefs.introDone(this)) showIntro() else buildMain()
    }

    override fun onResume() {
        super.onResume()
        if (::heroTitle.isInitialized) {
            refresh()
            Thread { TrustedTime.syncIfStale(this); runOnUiThread { refresh() } }.start()
        }
    }

    // ---------------------------------------------------------------- intro
    private fun showIntro() {
        val col = Ui.column(this).apply { setPadding(Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 28), Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 28)) }
        col.addView(TextView(this).apply { text = "⚠"; textSize = 34f; setTextColor(Ui.WARN) })
        col.addView(Ui.title(this, "Read this first", 30f))
        col.addView(Ui.body(this, "This is exactly what you are signing up for.", true, 15f), Ui.lp(this, 4, 16))

        for (block in INTRO_TEXT.split("\n\n")) {
            val lines = block.lines()
            val head = lines[0]
            if (lines.size > 1 && head.all { it.isUpperCase() || it == ' ' }) {
                col.addView(Ui.label(this, head), Ui.lp(this, 10))
                val card = Ui.card(this)
                lines.drop(1).forEachIndexed { i, l ->
                    val row = Ui.body(this, "•  " + l.removePrefix("- "), false, 14f)
                    card.addView(row, Ui.lp(this, if (i == 0) 0 else 10))
                }
                col.addView(card, Ui.lp(this, 4))
            } else {
                col.addView(Ui.body(this, block, false, 15f), Ui.lp(this, 8, 8))
            }
        }

        val agree = CheckBox(this).apply { text = "I have read all of this and want these restrictions on this phone"; setTextColor(Ui.TEXT); textSize = 14f }
        val go = Ui.button(this, "Continue") { Prefs.setIntroDone(this); recreate() }
        go.isEnabled = false; go.alpha = 0.4f
        agree.setOnCheckedChangeListener { _, on -> go.isEnabled = on; go.alpha = if (on) 1f else 0.4f }
        col.addView(agree, Ui.lp(this, 18, 8))
        col.addView(go, Ui.lp(this, 4))
        col.addView(Ui.button(this, "No thanks, close", Ui.Kind.SECONDARY) { finish() }, Ui.lp(this, 10))
        setContentView(ScrollView(this).apply { setBackgroundColor(Ui.BG); addView(col) })
    }

    // ---------------------------------------------------------------- main
    private fun buildMain() {
        val pad = Ui.dp(this, 16)
        val root = Ui.column(this).apply { setPadding(pad, Ui.dp(this@MainActivity, 24), pad, Ui.dp(this@MainActivity, 32)) }
        setContentView(ScrollView(this).apply { setBackgroundColor(Ui.BG); addView(root) })

        // hero
        root.addView(TextView(this).apply { text = getString(R.string.app_name); textSize = 14f; setTextColor(Ui.ACCENT); letterSpacing = 0.12f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
        heroTitle = Ui.title(this, "", 30f)
        heroSub = Ui.body(this, "")
        root.addView(heroTitle, Ui.lp(this, 4))
        root.addView(heroSub, Ui.lp(this, 4, 14))

        val status = Ui.card(this)
        statusBox = Ui.column(this)
        status.addView(statusBox)
        status.addView(Ui.button(this, "Apply protections now") {
            Enforcer.applyAsync(this, "manual"); toast("Applying…"); status.postDelayed({ refresh() }, 1500)
        }, Ui.lp(this, 10))
        status.addView(Ui.button(this, "Check UK time now", Ui.Kind.SECONDARY) {
            Thread { val ok = TrustedTime.sync(this); runOnUiThread { toast(if (ok) "UK time verified" else "Couldn't reach a time server"); refresh() } }.start()
        }, Ui.lp(this, 8))
        status.addView(Ui.button(this, "Open accessibility settings", Ui.Kind.SECONDARY) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, Ui.lp(this, 8))
        root.addView(status, Ui.lp(this, 0, 12))

        // unlock
        val unlock = Ui.card(this)
        unlock.addView(Ui.title(this, "PIN and unlock"))
        gateText = Ui.body(this, "")
        unlock.addView(gateText, Ui.lp(this, 6, 10))
        pinField = Ui.input(this, "PIN (4+ digits)", "", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        unlock.addView(pinField)
        unlock.addView(Ui.button(this, "Set PIN") { toast(Gate.setPin(this, pinField.text.toString())); refresh() }, Ui.lp(this, 10))
        unlock.addView(Ui.button(this, "Request unlock (14:00 UK tomorrow, 5 min)", Ui.Kind.SECONDARY) {
            toast(Gate.requestUnlock(this, pinField.text.toString())); refresh()
        }, Ui.lp(this, 8))
        unlock.addView(Ui.button(this, "Lock again now", Ui.Kind.SECONDARY) { Gate.lock(this); refresh() }, Ui.lp(this, 8))
        root.addView(unlock, Ui.lp(this, 0, 12))

        // settings
        val set = Ui.column(this)
        swStrict = Ui.toggle(this, "Strict mode: block factory reset, USB debugging and force-stop", Prefs.strict(this))
        swShield = Ui.toggle(this, "Self-shield: kick out of Settings pages showing this app", Prefs.shield(this))
        swCurfew = Ui.toggle(this, "Night curfew: hide Instagram and TikTok apps (UK time)", Prefs.curfewOn(this))
        set.addView(swStrict); set.addView(swShield); set.addView(swCurfew)
        set.addView(Ui.label(this, "Curfew hours (0-23)"))
        val hours = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        etCurfewStart = Ui.input(this, "from", Prefs.curfewStart(this).toString(), InputType.TYPE_CLASS_NUMBER)
        etCurfewEnd = Ui.input(this, "to", Prefs.curfewEnd(this).toString(), InputType.TYPE_CLASS_NUMBER)
        hours.addView(etCurfewStart, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = Ui.dp(this@MainActivity, 8) })
        hours.addView(etCurfewEnd, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        set.addView(hours)
        set.addView(Ui.label(this, "Keyword threshold (3 = one strong word or three mild)"))
        etThreshold = Ui.input(this, "3", Prefs.threshold(this).toString(), InputType.TYPE_CLASS_NUMBER)
        set.addView(etThreshold)
        set.addView(Ui.label(this, "Private DNS host"))
        etDns = Ui.input(this, "DNS host", Prefs.dnsHost(this))
        set.addView(etDns)
        set.addView(Ui.label(this, "Online blocklist URLs (https, one per line, refreshed weekly)"))
        etRemote = Ui.input(this, "https://…", Prefs.remoteUrlsRaw(this), InputType.TYPE_CLASS_TEXT, 2)
        set.addView(etRemote)
        set.addView(Ui.button(this, "Update online blocklist now", Ui.Kind.SECONDARY) {
            Thread { val m = RemoteList.refresh(this, true); runOnUiThread { toast(m); refresh() } }.start()
        }, Ui.lp(this, 10))
        set.addView(Ui.button(this, "Save settings") { saveSettings() }, Ui.lp(this, 8))
        root.addView(Ui.collapsible(this, "Settings", set, false), Ui.lp(this, 0, 12))

        // lists
        val lists = Ui.column(this)
        lists.addView(Ui.body(this, "Your additions to the built-in lists. Built-ins can't be removed. Removing your own entries needs an unlock."))
        listEditor(lists, "Extra blocked domains", Prefs.K_DOMAINS, false)
        listEditor(lists, "Extra strong keywords (1 hit blocks)", Prefs.K_STRONG, false)
        listEditor(lists, "Extra mild keywords (3 hits block)", Prefs.K_MILD, false)
        listEditor(lists, "Extra packages to hide", Prefs.K_HIDE, false)
        listEditor(lists, "Extra curfew-only apps", Prefs.K_CURFEW_APPS, false)
        listEditor(lists, "Extra ALLOWED browsers (weakens: needs unlock)", Prefs.K_BROWSER, true)
        root.addView(Ui.collapsible(this, "Block lists", lists, false), Ui.lp(this, 0, 12))

        // setup
        val setup = Ui.column(this)
        setup.addView(Ui.body(this, "1. Remove every account from the phone (Settings > Accounts); re-add them afterwards.\n2. Enable USB debugging, connect to a computer and run:"))
        setup.addView(TextView(this).apply {
            text = setupCmds; textSize = 12f; setTextColor(Ui.ACCENT); typeface = Typeface.MONOSPACE; setTextIsSelectable(true)
            background = Ui.rounded(this@MainActivity, Ui.FIELD, 12)
            setPadding(Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 12))
        }, Ui.lp(this, 8))
        setup.addView(Ui.button(this, "Copy commands", Ui.Kind.SECONDARY) {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("cmds", setupCmds)); toast("Copied")
        }, Ui.lp(this, 8))
        root.addView(Ui.collapsible(this, "Setup commands", setup, !Enforcer.isOwner(this)), Ui.lp(this, 0, 12))

        // log
        val log = Ui.column(this)
        logText = Ui.body(this, "", true, 12f)
        log.addView(logText)
        root.addView(Ui.collapsible(this, "Recent events", log, false), Ui.lp(this, 0, 12))

        // release / uninstall
        val danger = Ui.card(this).apply { background = Ui.rounded(this@MainActivity, Ui.CARD, 18, Ui.BAD) }
        danger.addView(Ui.title(this, "Release & uninstall"))
        danger.addView(Ui.body(this, "The only way to uninstall. It works only inside the 5-minute window at 14:00 UK time, after you requested an unlock with your PIN the day before."), Ui.lp(this, 6, 10))
        danger.addView(Ui.button(this, "Release & uninstall", Ui.Kind.DANGER) { releaseAndUninstall() })
        root.addView(danger, Ui.lp(this, 0, 12))

        root.addView(Ui.button(this, "What this app does", Ui.Kind.SECONDARY) {
            AlertDialog.Builder(this).setTitle("What this app does").setMessage(INTRO_TEXT).setPositiveButton("OK", null).show()
        })
        refresh()
    }

    private fun refresh() {
        val owner = Enforcer.isOwner(this)
        val a11y = Enforcer.a11yOk(this)
        val dnsMode = Settings.Global.getString(contentResolver, "private_dns_mode")
        val dnsOk = dnsMode == "hostname" && Settings.Global.getString(contentResolver, "private_dns_specifier") == Prefs.dnsHost(this)
        val full = owner && a11y && dnsOk
        heroTitle.text = if (full) "Protection active" else "Setup needed"
        heroTitle.setTextColor(if (full) Ui.OK else Ui.WARN)
        heroSub.text = "${Prefs.hits(this)} blocks so far" + if (Curfew.active(this)) "  ·  night curfew is ON" else ""

        statusBox.removeAllViews()
        statusBox.addView(Ui.dotRow(this, owner, "Device owner", if (owner) "Uninstall blocked, VPN and DNS locked" else "Run the setup commands below"))
        statusBox.addView(Ui.dotRow(this, a11y, "Screen scanner", if (a11y) "Accessibility service running" else "Tap Apply, or enable it in accessibility settings"))
        statusBox.addView(Ui.dotRow(this, dnsOk, "Private DNS", if (dnsOk) Prefs.dnsHost(this) else "Not locked yet"))
        val ukNow = TrustedTime.now(this)
        val ukText = if (ukNow == null) "Not verified. Connect to the internet." else
            java.text.SimpleDateFormat("EEE d MMM HH:mm", java.util.Locale.UK).apply { timeZone = TrustedTime.UK }.format(java.util.Date(ukNow))
        statusBox.addView(Ui.dotRow(this, ukNow != null, "UK time (internet)", ukText))
        statusBox.addView(Ui.dotRow(this, null, RemoteList.status(this).removePrefix("Remote list: ").replaceFirstChar { it.uppercase() }, "Online blocklist"))
        val report = Prefs.lastReport(this).lines().filter { it.isNotBlank() }
        if (report.isNotEmpty()) statusBox.addView(Ui.body(this, report.joinToString("\n"), true, 12f), Ui.lp(this, 8))

        gateText.text = Gate.describe(this)
        logText.text = Prefs.log(this).takeLast(25).reversed().joinToString("\n").ifEmpty { "(none yet)" }
    }

    // ---------------------------------------------------------------- actions
    private fun releaseAndUninstall() {
        if (!Gate.isUnlocked(this)) { toast("Locked. Request an unlock with your PIN; the window opens at 14:00 UK time the next day for 5 minutes."); return }
        AlertDialog.Builder(this)
            .setTitle("Release and uninstall?")
            .setMessage("This removes all protection and opens the uninstall prompt.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Release") { _, _ ->
                val msg = Enforcer.releaseAll(this)
                toast(msg)
                if (msg.startsWith("Released")) startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
                refresh()
            }.show()
    }

    private fun saveSettings() {
        val newStrict = swStrict.isChecked
        val newShield = swShield.isChecked; val newCurfew = swCurfew.isChecked
        val cs = etCurfewStart.text.toString().toIntOrNull() ?: Prefs.curfewStart(this)
        val ce = etCurfewEnd.text.toString().toIntOrNull() ?: Prefs.curfewEnd(this)
        val th = etThreshold.text.toString().toIntOrNull() ?: Prefs.threshold(this)
        val dns = etDns.text.toString().trim().ifEmpty { Prefs.DEFAULT_DNS }
        val remote = etRemote.text.toString().trim()

        val weakens = (Prefs.strict(this) && !newStrict) ||
            (Prefs.shield(this) && !newShield) ||
            (Prefs.curfewOn(this) && (!newCurfew || cs != Prefs.curfewStart(this) || ce != Prefs.curfewEnd(this))) ||
            th > Prefs.threshold(this) ||
            dns != Prefs.dnsHost(this) ||
            (remote != Prefs.remoteUrlsRaw(this).trim() && Prefs.remoteUrlsRaw(this).isNotBlank())
        if (weakens && !Gate.isUnlocked(this)) { toast("Locked: that change weakens protection. Unlock first."); return }

        Prefs.setStrict(this, newStrict); Prefs.setShield(this, newShield)
        Prefs.setCurfewOn(this, newCurfew); Prefs.setCurfew(this, cs, ce)
        Prefs.setThreshold(this, th); Prefs.setDnsHost(this, dns); Prefs.setRemoteUrls(this, remote)
        if (weakens) Alerts.event(this, "SETTINGS_WEAKENED", "settings changed during unlock window")
        Enforcer.applyAsync(this, "settings saved")
        toast("Saved"); refresh()
    }

    private fun listEditor(parent: LinearLayout, label: String, key: String, weakenOnAdd: Boolean) {
        parent.addView(Ui.label(this, label))
        val et = Ui.input(this, "one per line", Prefs.list(this, key).joinToString("\n"), InputType.TYPE_CLASS_TEXT, 2)
        parent.addView(et)
        parent.addView(Ui.button(this, "Save", Ui.Kind.SECONDARY) {
            val old = Prefs.list(this, key).toSet()
            val new = et.text.toString().split('\n').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            val weakens = if (weakenOnAdd) (new - old).isNotEmpty() else (old - new).isNotEmpty()
            if (weakens && !Gate.isUnlocked(this)) {
                toast(if (weakenOnAdd) "Locked: adding allowed browsers needs an unlock." else "Locked: removing entries needs an unlock.")
            } else {
                Prefs.setList(this, key, new)
                if (weakens) Alerts.event(this, "LIST_WEAKENED", "$key edited during unlock window")
                Enforcer.applyAsync(this, "list saved"); toast("Saved")
            }
        }, Ui.lp(this, 6))
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    companion object {
        const val INTRO_TEXT = """Guardian is a self-control app. You install it to stop YOURSELF reaching porn, nude-leak sites, Telegram, X (Twitter) and VPNs, and it is built so that you cannot easily turn it off.

WHAT IT DOES
- Becomes the phone's Device Owner (set once from a computer with adb). After that Android itself stops you uninstalling it. The only way to uninstall is the 5-minute window described below.
- Hides the Telegram, X (Twitter), Reddit and Discord apps (and alternative clients) so they cannot open at all, and blocks the Telegram and X websites. Reddit and Discord in a browser are NOT blocked.
- Blocks VPNs: VPN configuration is locked, every app that can create a VPN (and apps with vpn/proxy/tor/DNS-changer style names) is hidden, and a VPN connection triggers an alarm.
- Locks Private DNS to CleanBrowsing's adult filter, so every app's internet lookups go through it. CleanBrowsing sees your DNS lookups, and its adult filter also applies SafeSearch to Google/Bing/YouTube at DNS level (change the DNS host in the app, with the unlock, if that causes wrong blocks).
- Allows only Chrome and Brave. All other browsers are hidden. In Chrome/Brave: about 300 built-in adult domains are blocked, and incognito is off. SafeSearch is not forced and browsing history/cookies can still be cleared.
- Reads the on-screen text and web addresses of EVERY app through an accessibility service, and sends you to the home screen with a 15-second pause screen when it sees blocked sites or adult keywords. This is processed on the phone only.
- Downloads a public adult-domain list from GitHub every week and checks pages against it.
- Night curfew (default 23:00-06:00 UK time): the Instagram and TikTok apps are hidden. Browsers are not affected.
- Every build installs as a SEPARATE app with its own package name and display name. It never updates an older copy and never updates itself. Only one app can be Device Owner, so to move to a newer build you must first release the old one (PIN, then the next-day unlock) or factory reset.
- Hides clone/virtual-space and root-tool apps, scans every newly installed app, blocks safe mode, adding users and work profiles, and takes the time from the internet (UK time) for the unlock window and curfew, so changing the phone's clock does nothing; if the UK time cannot be verified, everything stays locked. Installing from unknown sources stays allowed.

WHAT IT MEANS FOR YOU
- Any change that weakens protection (removing a block, turning a protection off, changing the PIN) needs your PIN. Typing the PIN does not unlock anything straight away: it schedules a window that opens at 14:00 UK time the NEXT day and lasts only 5 minutes. Open the app during those 5 minutes to make changes. Adding protection is always instant.
- That 5-minute window is also the ONLY way to uninstall Guardian: tap Release & uninstall inside it. If you miss it you must request again and wait another day.
- Nothing can speed this up. If you forget the PIN, the only way out is a factory reset of the phone.
- Work or school VPNs will stop working. Telegram and X apps stay hidden, even if you need them for something legitimate.
- It may block harmless pages that contain several adult keywords, or domains on the downloaded list that are wrong.
- Android Settings stays fully usable in this version. Strict mode (off by default) additionally blocks factory reset, USB debugging and force-stop.

WHAT IT CANNOT STOP
- A factory reset from recovery, flashing the phone, another phone or computer, or switching the accessibility service off in Settings (it is re-enabled within about 5 minutes).

It collects nothing for any company and has no account. Its only network traffic is the weekly list download and DNS via CleanBrowsing.

Do not continue unless you want these restrictions on this phone."""
    }
}
