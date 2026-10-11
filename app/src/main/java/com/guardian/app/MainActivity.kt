package com.guardian.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var gateText: TextView
    private lateinit var logText: TextView
    private lateinit var pinField: EditText

    private lateinit var cbStrict: CheckBox
    private lateinit var cbShield: CheckBox
    private lateinit var cbCurfew: CheckBox
    private lateinit var etCurfewStart: EditText
    private lateinit var etCurfewEnd: EditText
    private lateinit var etCooldown: EditText
    private lateinit var etThreshold: EditText
    private lateinit var etDns: EditText
    private lateinit var etWebhook: EditText
    private lateinit var etRemote: EditText

    private val setupCmds: String
        get() = "adb shell dpm set-device-owner $packageName/.GuardianAdminReceiver\n" +
            "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Prefs.introDone(this)) { showIntro(); return }
        buildMain()
    }

    private fun showIntro() {
        val pad = dp(18)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        col.addView(TextView(this).apply { text = "Read this first"; textSize = 26f })
        col.addView(TextView(this).apply { text = INTRO_TEXT; textSize = 15f; setPadding(0, dp(12), 0, dp(12)) })
        col.addView(Button(this).apply {
            text = "I understand, continue"
            setOnClickListener { Prefs.setIntroDone(this@MainActivity); recreate() }
        })
        col.addView(Button(this).apply { text = "No thanks, close"; setOnClickListener { finish() } })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun buildMain() {
        val pad = dp(16)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        setContentView(ScrollView(this).apply { addView(root) })

        head("Guardian")
        button("What this app does (read again)") {
            androidx_free_dialog()
        }
        status = text(""); root.addView(status)
        button("Apply protections now") { Enforcer.applyAsync(this, "manual"); toast("Applying..."); status.postDelayed({ refresh() }, 1500) }
        button("Open accessibility settings") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }

        head("Setup (one time, from a computer)")
        text("1. Remove every Google/other account from the phone (Settings > Accounts), you can add them back afterwards.\n" +
            "2. Enable USB debugging, connect, then run:").also { root.addView(it) }
        text(setupCmds).apply { setTextIsSelectable(true); typeface = android.graphics.Typeface.MONOSPACE; root.addView(this) }
        button("Copy commands") {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("cmds", setupCmds))
            toast("Copied")
        }

        head("PIN and unlock (anti-bypass)")
        gateText = text(""); root.addView(gateText)
        pinField = EditText(this).apply { hint = "PIN"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        root.addView(pinField)
        button("Set / change PIN") { toast(Gate.setPin(this, pinField.text.toString())); refresh() }
        button("1. Request unlock (starts cooling-off)") { toast(Gate.requestUnlock(this, pinField.text.toString())); refresh() }
        button("2. Confirm unlock (after cooling-off)") { toast(Gate.confirmUnlock(this, pinField.text.toString())); refresh() }
        button("Lock again now") { Gate.lock(this); refresh() }

        head("Settings")
        cbStrict = check("Strict mode: block factory reset, USB debugging, force-stop/clear data of apps", Prefs.strict(this))
        cbShield = check("Self-shield: kick out of Settings pages that show Guardian", Prefs.shield(this))
        cbCurfew = check("Night curfew: hide all browsers + curfew apps", Prefs.curfewOn(this))
        etCurfewStart = num("Curfew start hour (0-23)", Prefs.curfewStart(this))
        etCurfewEnd = num("Curfew end hour (0-23)", Prefs.curfewEnd(this))
        etCooldown = num("Cooling-off hours", Prefs.cooldownHours(this))
        etThreshold = num("Keyword threshold (3 = one strong word or three mild)", Prefs.threshold(this))
        etDns = field("Private DNS host (family filter)", Prefs.dnsHost(this))
        etWebhook = field("Partner alert URL (ntfy.sh/topic, Discord or Slack webhook)", Prefs.webhook(this))
        etRemote = EditText(this).apply {
            setText(Prefs.remoteUrlsRaw(this@MainActivity)); minLines = 2; gravity = android.view.Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        root.addView(text("Online blocklist URLs (https, hosts or plain format, one per line; refreshed weekly)")); root.addView(etRemote)
        button("Update online blocklist now") {
            Thread { val m = RemoteList.refresh(this, true); runOnUiThread { toast(m); refresh() } }.start()
        }
        button("Save settings") { saveSettings() }

        head("Your additions (built-in lists can't be edited)")
        listEditor("Extra blocked domains", Prefs.K_DOMAINS, weakenOnAdd = false)
        listEditor("Extra strong keywords (1 hit blocks)", Prefs.K_STRONG, false)
        listEditor("Extra mild keywords (3 hits block)", Prefs.K_MILD, false)
        listEditor("Extra packages to hide", Prefs.K_HIDE, false)
        listEditor("Extra curfew-only apps (package names)", Prefs.K_CURFEW_APPS, false)
        listEditor("Extra ALLOWED browsers (weakens: needs unlock)", Prefs.K_BROWSER, true)

        head("Release")
        text("Removes device-owner protection so Guardian can be uninstalled. Requires an open unlock window.").also { root.addView(it) }
        button("Release device owner") {
            if (!Gate.isUnlocked(this)) toast("Locked. Unlock first.") else toast(Enforcer.releaseAll(this))
            refresh()
        }

        head("Recent events")
        logText = text(""); root.addView(logText)
        refresh()
    }

    private fun androidx_free_dialog() {
        android.app.AlertDialog.Builder(this).setTitle("What this app does").setMessage(INTRO_TEXT).setPositiveButton("OK", null).show()
    }

    override fun onResume() { super.onResume(); if (::status.isInitialized) refresh() }

    private fun refresh() {
        status.text = Enforcer.status(this) + "\n" + RemoteList.status(this)
        gateText.text = Gate.describe(this)
        logText.text = Prefs.log(this).takeLast(25).reversed().joinToString("\n").ifEmpty { "(none)" }
    }

    // ---- guarded saving ----
    private fun saveSettings() {
        val newStrict = cbStrict.isChecked
        val newShield = cbShield.isChecked; val newCurfew = cbCurfew.isChecked
        val cs = etCurfewStart.text.toString().toIntOrNull() ?: Prefs.curfewStart(this)
        val ce = etCurfewEnd.text.toString().toIntOrNull() ?: Prefs.curfewEnd(this)
        val cd = etCooldown.text.toString().toIntOrNull() ?: Prefs.cooldownHours(this)
        val th = etThreshold.text.toString().toIntOrNull() ?: Prefs.threshold(this)
        val dns = etDns.text.toString().trim().ifEmpty { Prefs.DEFAULT_DNS }
        val hook = etWebhook.text.toString().trim()
        val remote = etRemote.text.toString().trim()

        val weakens = (Prefs.strict(this) && !newStrict) ||
            (Prefs.shield(this) && !newShield) ||
            (Prefs.curfewOn(this) && (!newCurfew || cs != Prefs.curfewStart(this) || ce != Prefs.curfewEnd(this))) ||
            cd < Prefs.cooldownHours(this) ||
            th > Prefs.threshold(this) ||
            dns != Prefs.dnsHost(this) ||
            (remote != Prefs.remoteUrlsRaw(this).trim() && Prefs.remoteUrlsRaw(this).isNotBlank()) ||
            (Prefs.webhook(this).isNotEmpty() && hook != Prefs.webhook(this))
        if (weakens && !Gate.isUnlocked(this)) { toast("Locked: that change weakens protection. Unlock first."); return }

        Prefs.setStrict(this, newStrict); Prefs.setShield(this, newShield)
        Prefs.setCurfewOn(this, newCurfew); Prefs.setCurfew(this, cs, ce); Prefs.setCooldownHours(this, cd)
        Prefs.setThreshold(this, th); Prefs.setDnsHost(this, dns); Prefs.setWebhook(this, hook); Prefs.setRemoteUrls(this, remote)
        if (weakens) Alerts.event(this, "SETTINGS_WEAKENED", "settings changed during unlock window")
        Enforcer.applyAsync(this, "settings saved")
        toast("Saved"); refresh()
    }

    private fun listEditor(label: String, key: String, weakenOnAdd: Boolean) {
        text(label).also { root.addView(it) }
        val et = EditText(this).apply {
            setText(Prefs.list(this@MainActivity, key).joinToString("\n"))
            minLines = 2; gravity = android.view.Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hint = "one per line"
        }
        root.addView(et)
        button("Save: $label") {
            val old = Prefs.list(this, key).toSet()
            val new = et.text.toString().split('\n').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            val weakens = if (weakenOnAdd) (new - old).isNotEmpty() else (old - new).isNotEmpty()
            if (weakens && !Gate.isUnlocked(this)) {
                toast(if (weakenOnAdd) "Locked: adding allowed browsers needs unlock." else "Locked: removing entries needs unlock.")
            } else {
                Prefs.setList(this, key, new)
                if (weakens) Alerts.event(this, "LIST_WEAKENED", "$key edited during unlock window")
                Enforcer.applyAsync(this, "list saved"); toast("Saved")
            }
        }
    }

    // ---- tiny view helpers ----
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    private fun text(s: String) = TextView(this).apply { text = s; setPadding(0, dp(4), 0, dp(4)) }
    private fun head(s: String) { root.addView(TextView(this).apply { text = s; textSize = 20f; setPadding(0, dp(20), 0, dp(6)) }) }
    private fun button(label: String, onClick: () -> Unit) { root.addView(Button(this).apply { text = label; setOnClickListener { onClick() } }) }
    private fun check(label: String, on: Boolean) = CheckBox(this).apply { text = label; isChecked = on }.also { root.addView(it) }
    private fun field(label: String, value: String) = EditText(this).apply { hint = label; setText(value) }.also {
        root.addView(text(label)); root.addView(it)
    }
    private fun num(label: String, value: Int) = field(label, value.toString()).apply { inputType = InputType.TYPE_CLASS_NUMBER }

    companion object {
        const val INTRO_TEXT = """Guardian is a self-control app. You install it to stop YOURSELF reaching porn, nude-leak sites, Telegram, X (Twitter) and VPNs, and it is built so that you cannot easily turn it off.

WHAT IT DOES
- Becomes the phone's Device Owner (set once from a computer with adb). After that Android itself stops you uninstalling it.
- Hides the Telegram and X apps (and Telegram clients) so they cannot open at all, and blocks their websites.
- Blocks VPNs: VPN configuration is locked, every app that can create a VPN (and apps with vpn/proxy/tor/DNS-changer style names) is hidden, and a VPN connection triggers an alarm.
- Locks Private DNS to CleanBrowsing's adult filter, so every app's internet lookups go through it. CleanBrowsing sees your DNS lookups, and its adult filter also applies SafeSearch to Google/Bing/YouTube at DNS level (change the DNS host in the app, with the unlock, if that causes wrong blocks).
- Allows only Chrome and Brave. All other browsers are hidden. In Chrome/Brave: about 300 built-in adult domains are blocked, and incognito is off. SafeSearch is not forced and browsing history/cookies can still be cleared.
- Reads the on-screen text and web addresses of EVERY app through an accessibility service, and sends you to the home screen with a 15-second pause screen when it sees blocked sites or adult keywords. This is processed on the phone only.
- Downloads a public adult-domain list from GitHub every week and checks pages against it.
- Night curfew (default 23:00-06:00): all browsers plus Reddit, Instagram, Snapchat, TikTok and Discord are hidden.
- If you set a partner alert URL (ntfy.sh, Discord or Slack), it sends them short alerts: blocks (app name only, never the page), VPN attempts, accessibility turned off, DNS changes, wrong PIN attempts and unlock requests.
- Hides clone/virtual-space and root-tool apps, scans every newly installed app, blocks safe mode, adding users and work profiles, and watches the system clock (it is NOT locked; changing it cancels a pending unlock and alerts your partner). Installing from unknown sources stays allowed.

WHAT IT MEANS FOR YOU
- Any change that weakens protection (removing a block, turning a protection off, releasing the app) needs your PIN, then a cooling-off wait (default 24 hours), then the PIN again within 30 minutes. Your partner is alerted at every step. Adding protection is always instant.
- Set the PIN together with a trusted person and let THEM keep it. If you forget it, the only way out is a factory reset of the phone.
- Work or school VPNs will stop working. Telegram and X apps stay hidden, even if you need them for something legitimate.
- It may block harmless pages that contain several adult keywords, or domains on the downloaded list that are wrong.
- Android Settings stays fully usable in this version. Strict mode (off by default) additionally blocks factory reset, USB debugging and force-stop.

WHAT IT CANNOT STOP
- A factory reset from recovery, flashing the phone, another phone or computer, or switching the accessibility service off in Settings (it is re-enabled within about 5 minutes and your partner is told).

It collects nothing for any company and has no account. Its only network traffic is the weekly list download, DNS via CleanBrowsing, and the alert URL you choose.

Do not continue unless you want these restrictions on this phone."""
    }
}
