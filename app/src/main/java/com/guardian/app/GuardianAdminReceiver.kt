package com.guardian.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class GuardianAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Alerts.event(context, "ADMIN_ENABLED", "Guardian is now device admin/owner")
        Scheduler.scheduleAll(context)
        Enforcer.applyAsync(context, "admin enabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Alerts.event(context, "ADMIN_DISABLE_REQUEST", "someone tried to deactivate the device admin")
        return "Guardian protects you from content you asked to block."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Alerts.event(context, "ADMIN_DISABLED", "device admin removed")
    }
}
