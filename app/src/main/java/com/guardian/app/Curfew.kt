package com.guardian.app

import android.content.Context
import java.util.Calendar

object Curfew {
    /** Uses verified internet time in UK time zone; falls back to the phone clock (also in UK zone) if unverified. */
    fun active(c: Context): Boolean {
        if (!Prefs.curfewOn(c)) return false
        val cal = Calendar.getInstance(TrustedTime.UK)
        cal.timeInMillis = TrustedTime.now(c) ?: System.currentTimeMillis()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val s = Prefs.curfewStart(c)
        val e = Prefs.curfewEnd(c)
        return if (s <= e) h in s until e else (h >= s || h < e)
    }
}
