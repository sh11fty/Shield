package com.guardian.app

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.TimeUnit

class BlockActivity : Activity() {
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reason = intent.getStringExtra("reason") ?: "Blocked"
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF111111.toInt())
        }
        fun tv(text: String, size: Float, color: Int = 0xFFFFFFFF.toInt()) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER; setPadding(0, pad / 2, 0, pad / 2)
        }
        root.addView(tv("Blocked", 34f))
        root.addView(tv(reason, 16f, 0xFFBBBBBB.toInt()))
        val hits = Prefs.hits(this)
        root.addView(tv("Blocks so far: $hits", 14f, 0xFF888888.toInt()))
        root.addView(tv("Pause. Take three slow breaths. Stand up, drink some water, go for a short walk. The urge passes in a few minutes.", 16f))
        val btn = Button(this).apply { isEnabled = false; text = "Wait 15s" ; setOnClickListener { finishAndRemoveTask() } }
        root.addView(btn)
        setContentView(root)
        timer = object : CountDownTimer(15_000, 1000) {
            override fun onTick(ms: Long) { btn.text = "Wait ${TimeUnit.MILLISECONDS.toSeconds(ms) + 1}s" }
            override fun onFinish() { btn.isEnabled = true; btn.text = "OK" }
        }.start()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* no escape via back */ }

    override fun onDestroy() { timer?.cancel(); super.onDestroy() }
}
