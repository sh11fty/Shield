package com.guardian.app

import android.content.Context
import java.text.DateFormat
import java.util.Date

/**
 * Anything that WEAKENS protection (removing a block, turning a toggle off, releasing device-owner)
 * needs: PIN -> cooling-off wait (default 24h, partner is alerted) -> PIN again inside a 30 min window
 * -> 15 minutes of edit access. Strengthening is always free. With no PIN set everything is open.
 */
object Gate {
    private const val CONFIRM_WINDOW_MS = 30 * 60 * 1000L
    private const val EDIT_WINDOW_MS = 15 * 60 * 1000L

    fun isUnlocked(c: Context): Boolean = !Prefs.hasPin(c) || System.currentTimeMillis() < Prefs.unlockedUntil(c)

    fun describe(c: Context): String {
        if (!Prefs.hasPin(c)) return "No PIN set: all changes are allowed. Set a PIN (ideally typed by your accountability partner)."
        val now = System.currentTimeMillis()
        val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val until = Prefs.unlockedUntil(c)
        val ready = Prefs.readyAt(c)
        return when {
            now < until -> "UNLOCKED until ${df.format(Date(until))}. Changes allowed now."
            ready > now -> "Unlock requested. Confirm with PIN after ${df.format(Date(ready))} (window: 30 min)."
            ready != 0L && now <= ready + CONFIRM_WINDOW_MS -> "Cooling-off over. Enter PIN and press Confirm unlock now."
            else -> "LOCKED. Weakening changes need PIN + ${Prefs.cooldownHours(c)}h cooling-off."
        }
    }

    fun requestUnlock(c: Context, pin: String): String {
        if (!Prefs.hasPin(c)) return "No PIN set."
        if (!Prefs.checkPin(c, pin)) { Alerts.event(c, "BAD_PIN", "wrong PIN on unlock request"); return "Wrong PIN." }
        val now = System.currentTimeMillis()
        if (Prefs.readyAt(c) > now) return describe(c)
        val ready = now + Prefs.cooldownHours(c) * 3_600_000L
        Prefs.setUnlock(c, ready, 0L)
        Alerts.event(c, "UNLOCK_REQUESTED", "cooling-off ${Prefs.cooldownHours(c)}h started")
        return describe(c)
    }

    fun confirmUnlock(c: Context, pin: String): String {
        if (!Prefs.hasPin(c)) return "No PIN set."
        if (!Prefs.checkPin(c, pin)) { Alerts.event(c, "BAD_PIN", "wrong PIN on unlock confirm"); return "Wrong PIN." }
        val now = System.currentTimeMillis()
        val ready = Prefs.readyAt(c)
        if (ready == 0L) return "Request an unlock first."
        if (now < ready) return describe(c)
        if (now > ready + CONFIRM_WINDOW_MS) { Prefs.setUnlock(c, 0L, 0L); return "Window expired. Request again." }
        Prefs.setUnlock(c, 0L, now + EDIT_WINDOW_MS)
        Alerts.event(c, "UNLOCKED", "15 minute edit window opened")
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
