package com.guardian.app

import android.content.Context
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * Anything that WEAKENS protection (removing a block, turning a toggle off, releasing device-owner) needs the PIN.
 * Typing the PIN does not unlock anything yet: it schedules a window that opens at 14:00 the NEXT day and lasts
 * only 5 minutes. During it you can change settings and use "Release & uninstall", which is the ONLY way to
 * uninstall. Nothing speeds this up. Strengthening is always free. With no PIN set everything is open.
 */
object Gate {
    private const val WINDOW_MS = 5 * 60_000L

    fun isUnlocked(c: Context): Boolean {
        if (!Prefs.hasPin(c)) return true
        Enforcer.clockWatch(c)
        val now = System.currentTimeMillis()
        val r = Prefs.readyAt(c)
        return r != 0L && now >= r && now < Prefs.unlockedUntil(c)
    }

    fun describe(c: Context): String {
        if (!Prefs.hasPin(c)) return "No PIN set: all changes are allowed. Set a PIN you won't be able to talk yourself out of."
        val now = System.currentTimeMillis()
        val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val r = Prefs.readyAt(c)
        val u = Prefs.unlockedUntil(c)
        return when {
            r != 0L && now >= r && now < u -> "UNLOCKED until ${df.format(Date(u))}. Changes allowed now."
            r > now -> "Unlock requested. Changes open at ${df.format(Date(r))} for 5 minutes."
            else -> "LOCKED. Enter the PIN to request an unlock; it opens at 14:00 tomorrow for 5 minutes."
        }
    }

    private fun nextDayEleven(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 14)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun requestUnlock(c: Context, pin: String): String {
        if (!Prefs.hasPin(c)) return "No PIN set."
        if (!Prefs.checkPin(c, pin)) { Alerts.event(c, "BAD_PIN", "wrong PIN on unlock request"); return "Wrong PIN." }
        val now = System.currentTimeMillis()
        if (Prefs.readyAt(c) != 0L && now < Prefs.unlockedUntil(c)) return describe(c)
        val ready = nextDayEleven()
        Prefs.setUnlock(c, ready, ready + WINDOW_MS)
        Alerts.event(c, "UNLOCK_REQUESTED", "window opens at 14:00 tomorrow (5 min)")
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
