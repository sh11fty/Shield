package com.guardian.app

import android.content.Context
import java.util.Calendar

object Curfew {
    fun active(c: Context): Boolean {
        if (!Prefs.curfewOn(c)) return false
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val s = Prefs.curfewStart(c)
        val e = Prefs.curfewEnd(c)
        return if (s <= e) h in s until e else (h >= s || h < e)
    }
}
