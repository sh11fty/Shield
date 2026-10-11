package com.guardian.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/** Small view-building toolkit so the whole UI is plain Kotlin with no libraries. */
object Ui {
    const val BG = 0xFF0E1116.toInt()
    const val CARD = 0xFF171B22.toInt()
    const val FIELD = 0xFF222836.toInt()
    const val TEXT = 0xFFE8EAED.toInt()
    const val MUTED = 0xFF9AA0A6.toInt()
    const val ACCENT = 0xFF4FD1C5.toInt()
    const val OK = 0xFF55D68A.toInt()
    const val BAD = 0xFFFF6B6B.toInt()
    const val WARN = 0xFFF6C85F.toInt()

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    fun rounded(c: Context, color: Int, radiusDp: Int, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(c, radiusDp).toFloat()
            if (strokeColor != null) setStroke(dp(c, 1), strokeColor)
        }

    fun lp(c: Context, top: Int = 0, bottom: Int = 0, width: Int = ViewGroup.LayoutParams.MATCH_PARENT) =
        LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(c, top); bottomMargin = dp(c, bottom)
        }

    fun column(c: Context) = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }

    fun card(c: Context): LinearLayout = column(c).apply {
        background = rounded(c, CARD, 18)
        setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16))
    }

    fun title(c: Context, t: String, size: Float = 18f) = TextView(c).apply {
        text = t; textSize = size; setTextColor(TEXT); typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    fun body(c: Context, t: String, muted: Boolean = true, size: Float = 14f) = TextView(c).apply {
        text = t; textSize = size; setTextColor(if (muted) MUTED else TEXT); setLineSpacing(0f, 1.18f)
    }

    fun label(c: Context, t: String) = TextView(c).apply {
        text = t.uppercase(); textSize = 11f; setTextColor(MUTED); letterSpacing = 0.1f
        setPadding(0, dp(c, 12), 0, dp(c, 4))
    }

    fun dotRow(c: Context, ok: Boolean?, t: String, detail: String = ""): LinearLayout =
        LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(c, 7), 0, dp(c, 7))
            val dot = View(c).apply {
                background = rounded(c, when (ok) { true -> OK; false -> BAD; null -> WARN }, 6)
            }
            addView(dot, LinearLayout.LayoutParams(dp(c, 10), dp(c, 10)).apply { rightMargin = dp(c, 12) })
            val col = column(c)
            col.addView(TextView(c).apply { text = t; textSize = 15f; setTextColor(TEXT) })
            if (detail.isNotEmpty()) col.addView(TextView(c).apply { text = detail; textSize = 12f; setTextColor(MUTED) })
            addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

    enum class Kind { PRIMARY, SECONDARY, DANGER }

    fun button(c: Context, label: String, kind: Kind = Kind.PRIMARY, onClick: () -> Unit): Button =
        Button(c).apply {
            text = label; isAllCaps = false; textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            stateListAnimator = null
            val (bg, fg, stroke) = when (kind) {
                Kind.PRIMARY -> Triple(ACCENT, 0xFF06201D.toInt(), null)
                Kind.SECONDARY -> Triple(FIELD, TEXT, null)
                Kind.DANGER -> Triple(0x00000000, BAD, BAD)
            }
            setTextColor(fg)
            background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), rounded(c, bg, 12, stroke), null)
            setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12))
            setOnClickListener { onClick() }
        }

    fun input(c: Context, hint: String, value: String, type: Int = InputType.TYPE_CLASS_TEXT, lines: Int = 1): EditText =
        EditText(c).apply {
            this.hint = hint; setText(value); inputType = type or (if (lines > 1) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0)
            setTextColor(TEXT); setHintTextColor(MUTED); textSize = 14f
            if (lines > 1) { minLines = lines; gravity = Gravity.TOP or Gravity.START }
            background = rounded(c, FIELD, 12)
            setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12))
        }

    fun toggle(c: Context, label: String, checked: Boolean): Switch = Switch(c).apply {
        text = label; isChecked = checked; setTextColor(TEXT); textSize = 14f
        setPadding(0, dp(c, 8), 0, dp(c, 8))
    }

    /** Header that shows/hides [content] when tapped. */
    fun collapsible(c: Context, heading: String, content: View, startOpen: Boolean = false): LinearLayout {
        val box = card(c)
        val head = TextView(c).apply { textSize = 17f; setTextColor(TEXT); typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
        fun render(open: Boolean) { head.text = (if (open) "▾  " else "▸  ") + heading; content.visibility = if (open) View.VISIBLE else View.GONE }
        var open = startOpen
        render(open)
        head.setOnClickListener { open = !open; render(open) }
        box.addView(head)
        box.addView(content, lp(c, 8))
        return box
    }
}
