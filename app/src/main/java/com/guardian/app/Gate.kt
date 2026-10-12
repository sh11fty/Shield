package com.guardian.app

import android.content.Context
import java.text.DateFormat
import java.util.Date

/**
 * Anything that WEAKENS protection (removing a block, turning a toggle off, changing the PIN) needs the PIN.
 * Typing the PIN does not unlock anything yet: it schedules a window that opens 10 minutes later
 * and lasts only 5 minutes. During it you can change settings and use "Release & uninstall", which is the ONLY
 * way to uninstall. Time comes from the internet (see TrustedTime), so changing the phone's clock does nothing;
 * if the time cannot be verified the app stays locked. Strengthening is always free. No PIN = everything open.
 */
object Gate {
    private const val WINDOW_MS = 5 * 60_000L
    /** Test build: the window opens 10 minutes after the PIN is entered. */
    private const val DELAY_MS = 10 * 60_000L

    fun isUnlocked(c: Context): Boolean {
        if (!Prefs.hasPin(c)) return true
        val now = TrustedTime.now(c) ?: return false
        val r = Prefs.readyAt(c)
        return r != 0L && now >= r && now < Prefs.unlockedUntil(c)
    }

    private fun fmt(ms: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).apply { timeZone = TrustedTime.UK }.format(Date(ms)) + " UK"

    fun describe(c: Context): String {
        if (!Prefs.hasPin(c)) return "No PIN set: all changes are allowed. Set a PIN you won't be able to talk yourself out of."
        val now = TrustedTime.now(c)
            ?: return "UK time not verified yet. Connect to the internet; everything stays locked until it is."
        val r = Prefs.readyAt(c)
        val u = Prefs.unlockedUntil(c)
        return when {
            r != 0L && now >= r && now < u -> "UNLOCKED until ${fmt(u)}. Changes and Release & uninstall are allowed now."
            r > now -> "Unlock requested. The 5-minute window opens at ${fmt(r)}."
            else -> "LOCKED. Enter the PIN to request an unlock; it opens 10 minutes later and lasts 5 minutes."
        }
    }

    fun requestUnlock(c: Context, pin: String): String {
        if (!Prefs.hasPin(c)) return "No PIN set."
        if (!Prefs.checkPin(c, pin)) { Alerts.event(c, "BAD_PIN", "wrong PIN on unlock request"); return "Wrong PIN." }
        val now = TrustedTime.now(c) ?: return "Can't verify the UK time yet. Connect to the internet and try again."
        if (Prefs.readyAt(c) != 0L && now < Prefs.unlockedUntil(c)) return describe(c)
        val ready = now + DELAY_MS
        Prefs.setUnlock(c, ready, ready + WINDOW_MS)
        Alerts.event(c, "UNLOCK_REQUESTED", "window opens in 10 min (5 min long)")
        return describe(c)
    }

    fun lock(c: Context) = Prefs.setUnlock(c, 0L, 0L)

    /** Setting the first PIN is free; changing it needs an open unlock window. */
    fun setPin(c: Context, pin: String): String {
        if (pin.length < 4) return "PIN must be at least 4 digits."
        if (Prefs.hasPin(c) && !isUnlocked(c)) return "Locked. Unlock first to change the PIN."
        Prefs.setPin(c, pin)
        Alerts.event(c, "PIN_SET", "PIN changed")
        return "PIN saved."
    }
}
