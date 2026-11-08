package com.guardian.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Package broadcasts are delivered independently of the accessibility service.
 * Never remove a saved block when its package is uninstalled.
 */
class PermanentBlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.data?.schemeSpecificPart ?: return
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                // Includes both first installs and reinstalls of permanently blocked packages.
                // The enforcer also evaluates built-in rules and alternative client signatures.
                Enforcer.applyAsync(context.applicationContext, "package installed or replaced: $pkg")
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                // Deliberately retain Prefs.K_HIDE and all built-in block rules.
                // Do not clear the package's permanent block on uninstall.
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) &&
                    pkg in Prefs.list(context, Prefs.K_HIDE)) {
                    Prefs.appendLog(context, "Permanent block retained after uninstall: $pkg")
                }
            }
        }
    }
}
