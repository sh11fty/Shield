package com.guardian.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local event log only (shown on the main screen). Nothing is sent anywhere. */
object Alerts {
    fun event(c: Context, type: String, detail: String) {
        val stamp = SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date())
        Prefs.appendLog(c.applicationContext, "$stamp $type: $detail")
    }
}
