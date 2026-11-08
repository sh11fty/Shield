package com.guardian.app

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Searchable, bulk-editable UI for the existing extra_hide list. */
class AppBlocklistActivity : Activity() {
    private data class App(val pkg: String, val name: String, val info: ApplicationInfo?)
    private val apps = ArrayList<App>()
    private val pending = LinkedHashSet<String>()
    private lateinit var rows: LinearLayout
    private lateinit var summary: TextView
    private lateinit var search: EditText
    private var blockedOnly = false
    private var showSystem = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        build()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
        render()
    }

    private fun installed(info: ApplicationInfo) =
        (info.flags and ApplicationInfo.FLAG_INSTALLED) != 0

    private fun loadApps() {
        apps.clear()
        val pm = packageManager
        val names = LinkedHashMap<String, ApplicationInfo?>()
        @Suppress("DEPRECATION")
        val installedApps = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        for (info in installedApps) {
            if (installed(info) || info.packageName in Prefs.hiddenByUs(this)) names[info.packageName] = info
        }
        for (pkg in Prefs.list(this, Prefs.K_HIDE) + Prefs.hiddenByUs(this)) {
            if (pkg !in names) names[pkg] = runCatching {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            }.getOrNull()
        }
        for ((pkg, info) in names) {
            val name = info?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() } ?: pkg
            apps.add(App(pkg, name, info))
        }
        apps.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        pending.clear()
        pending.addAll(Prefs.list(this, Prefs.K_HIDE))
    }

    private fun build() {
        val root = Ui.column(this).apply {
            setPadding(Ui.dp(this@AppBlocklistActivity, 16), Ui.dp(this@AppBlocklistActivity, 22),
                Ui.dp(this@AppBlocklistActivity, 16), Ui.dp(this@AppBlocklistActivity, 24))
        }
        setContentView(ScrollView(this).apply { setBackgroundColor(Ui.BG); addView(root) })
        root.addView(Ui.title(this, "App Blocklist", 27f))
        root.addView(Ui.body(this, "Select apps by name. Added blocks take effect immediately; removing a block requires the delayed PIN unlock."), Ui.lp(this, 6, 14))
        summary = Ui.body(this, "")
        root.addView(summary, Ui.lp(this, 0, 10))
        search = Ui.input(this, "Search app name or package", "")
        root.addView(search)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render()
            override fun afterTextChanged(s: Editable?) {}
        })
        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val blocked = CheckBox(this).apply {
            text = "Blocked only"; setTextColor(Ui.TEXT)
            setOnCheckedChangeListener { _, value -> blockedOnly = value; render() }
        }
        val system = CheckBox(this).apply {
            text = "Show system apps"; setTextColor(Ui.TEXT)
            setOnCheckedChangeListener { _, value -> showSystem = value; render() }
        }
        filters.addView(blocked, LinearLayout.LayoutParams(0, -2, 1f))
        filters.addView(system, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(filters, Ui.lp(this, 10))
        root.addView(Ui.button(this, "Block selected apps") { saveSelection() }, Ui.lp(this, 8))
        root.addView(Ui.button(this, "Add package manually", Ui.Kind.SECONDARY) { manualAdd() }, Ui.lp(this, 8))
        root.addView(Ui.body(this, "Built-in blocks cannot be removed here. System-critical apps are excluded. Device Owner is required to hide apps."), Ui.lp(this, 12))
        rows = Ui.column(this)
        root.addView(rows, Ui.lp(this, 10))
    }

    private fun builtin(app: App): Boolean =
        Blocklists.HIDE_PACKAGE_PREFIXES.any { app.pkg.startsWith(it) } ||
        Blocklists.HIDE_PACKAGE_REGEX.containsMatchIn(app.pkg) ||
        (app.info != null && Blocklists.HIDE_LABEL_REGEX.containsMatchIn(app.name))

    private fun hidden(pkg: String): Boolean {
        if (!Enforcer.isOwner(this)) return false
        val d = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return runCatching { d.isApplicationHidden(Enforcer.admin(this), pkg) }.getOrDefault(false)
    }

    private fun render() {
        if (!::rows.isInitialized) return
        rows.removeAllViews()
        val query = search.text.toString().trim()
        val own = Enforcer.isOwner(this)
        summary.text = "${pending.size} custom blocks · ${Prefs.hiddenByUs(this).size} hidden by Shield · " +
            if (own) "Device Owner active" else "Device Owner not active"
        val filtered = apps.filter { app ->
            (showSystem || app.info == null || (app.info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                app.pkg in pending || app.pkg in Prefs.hiddenByUs(this)) &&
                (!blockedOnly || app.pkg in pending || builtin(app) || app.pkg in Prefs.hiddenByUs(this)) &&
                (query.isEmpty() || app.name.contains(query, true) || app.pkg.contains(query, true))
        }
        if (filtered.isEmpty()) rows.addView(Ui.body(this, "No matching apps. Use manual entry for apps not installed."))
        for (app in filtered) {
            val builtIn = builtin(app)
            val protected = Enforcer.isProtectedPackage(this, app.pkg)
            val statusText = Enforcer.appBlockStatus(this, app.pkg)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(0, Ui.dp(this@AppBlocklistActivity, 8), 0, Ui.dp(this@AppBlocklistActivity, 8))
            }
            val icon = ImageView(this).apply {
                setImageDrawable(app.info?.let { runCatching { packageManager.getApplicationIcon(it) }.getOrNull() })
            }
            row.addView(icon, LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38)).apply { rightMargin = Ui.dp(this@AppBlocklistActivity, 12) })
            val text = Ui.column(this)
            text.addView(Ui.title(this, app.name, 15f))
            text.addView(Ui.body(this, app.pkg, true, 11f))
            val status = when {
                protected -> "Protected system app"
                statusText == "Hidden + suspended" || statusText == "Hidden" || statusText.startsWith("Suspended") -> statusText
                builtIn -> "Built-in block · " + statusText
                app.pkg in pending -> "Selected · " + statusText
                else -> "Not blocked"
            }
            text.addView(Ui.body(this, status, true, 11f))
            row.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
            val checkbox = CheckBox(this).apply {
                isChecked = builtIn || app.pkg in pending
                isEnabled = !builtIn && !protected
                setOnCheckedChangeListener { _, checked ->
                    if (checked) pending.add(app.pkg) else pending.remove(app.pkg)
                    summary.text = "${pending.size} custom blocks selected"
                }
            }
            row.addView(checkbox)
            row.setOnClickListener { if (checkbox.isEnabled) checkbox.isChecked = !checkbox.isChecked }
            rows.addView(row)
            rows.addView(View(this).apply { setBackgroundColor(Ui.FIELD) }, LinearLayout.LayoutParams(-1, Ui.dp(this, 1)))
        }
    }

    private fun saveSelection() {
        val old = Prefs.list(this, Prefs.K_HIDE).toSet()
        val additions = pending - old
        val removals = old - pending
        if (removals.isNotEmpty() && !Gate.isUnlocked(this)) {
            Toast.makeText(this, "Removing blocks requires the delayed PIN unlock.", Toast.LENGTH_LONG).show()
            return
        }
        if (additions.isEmpty() && removals.isEmpty()) {
            Toast.makeText(this, "No changes", Toast.LENGTH_SHORT).show()
            return
        }
        val safe = pending.filterNot { Enforcer.isProtectedPackage(this, it) }.toSet()
        Prefs.setList(this, Prefs.K_HIDE, safe)
        if (removals.isNotEmpty()) Alerts.event(this, "LIST_WEAKENED", "custom app blocks removed during unlock window")
        Enforcer.applyAsync(this, "app blocklist updated")
        Toast.makeText(this, "Saved. Applying Device Owner restrictions.", Toast.LENGTH_LONG).show()
        loadApps()
        render()
    }

    private fun manualAdd() {
        val field = Ui.input(this, "com.example.app", "")
        val box = Ui.column(this).apply { setPadding(Ui.dp(this@AppBlocklistActivity, 20), Ui.dp(this@AppBlocklistActivity, 10), Ui.dp(this@AppBlocklistActivity, 20), 0); addView(field) }
        AlertDialog.Builder(this).setTitle("Block package name")
            .setView(box).setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val pkg = field.text.toString().trim()
                if (!Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+").matches(pkg) ||
                    Enforcer.isProtectedPackage(this, pkg)) {
                    Toast.makeText(this, "Invalid or protected package name", Toast.LENGTH_LONG).show()
                } else {
                    val updated = Prefs.list(this, Prefs.K_HIDE).toMutableSet()
                    updated.add(pkg)
                    Prefs.setList(this, Prefs.K_HIDE, updated)
                    Enforcer.applyAsync(this, "manual app block added")
                    loadApps()
                    render()
                }
            }.show()
    }
}
