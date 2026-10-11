package com.guardian.app

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.TimeUnit

class BlockActivity : Activity() {
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        val reason = intent.getStringExtra("reason") ?: "Blocked"
        val pad = Ui.dp(this, 28)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Ui.BG)
        }
        root.addView(TextView(this).apply { text = "🛡"; textSize = 56f; gravity = Gravity.CENTER })
        root.addView(Ui.title(this, "Blocked", 34f).apply { gravity = Gravity.CENTER }, Ui.lp(this, 8))
        root.addView(Ui.body(this, reason, true, 15f).apply { gravity = Gravity.CENTER }, Ui.lp(this, 6))
        root.addView(Ui.body(this, "${Prefs.hits(this)} blocks so far", true, 13f).apply { gravity = Gravity.CENTER }, Ui.lp(this, 2, 20))

        val card = Ui.card(this)
        card.addView(TextView(this).apply {
            text = "Pause. Take three slow breaths. Stand up, drink some water, go for a short walk.\n\nThe urge passes in a few minutes."
            textSize = 16f; setTextColor(Ui.TEXT); gravity = Gravity.CENTER; setLineSpacing(0f, 1.2f)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        })
        root.addView(card, Ui.lp(this, 0, 24))

        val btn = Ui.button(this, "Wait 15s") { finishAndRemoveTask() }
        btn.isEnabled = false; btn.alpha = 0.4f
        root.addView(btn)
        setContentView(root)

        timer = object : CountDownTimer(15_000, 1000) {
            override fun onTick(ms: Long) { btn.text = "Wait ${TimeUnit.MILLISECONDS.toSeconds(ms) + 1}s" }
            override fun onFinish() { btn.isEnabled = true; btn.alpha = 1f; btn.text = "OK" }
        }.start()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* no escape via back */ }

    override fun onDestroy() { timer?.cancel(); super.onDestroy() }
}
