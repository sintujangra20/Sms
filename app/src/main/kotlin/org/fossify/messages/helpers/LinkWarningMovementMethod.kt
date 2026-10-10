package org.fossify.messages.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.fossify.messages.R

class LinkWarningMovementMethod(private val context: Context) : LinkMovementMethod() {
    override fun onTouchEvent(widget: TextView, buffer: android.text.Spannable, event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) {
            return super.onTouchEvent(widget, buffer, event)
        }

        val layout = widget.layout ?: return super.onTouchEvent(widget, buffer, event)
        val x = (event.x - widget.totalPaddingLeft + widget.scrollX).toInt()
        val y = (event.y - widget.totalPaddingTop + widget.scrollY).toInt()
        if (x < 0 || y < 0 || x > layout.width || y > layout.height) {
            return super.onTouchEvent(widget, buffer, event)
        }

        val line = layout.getLineForVertical(y)
        val off = layout.getOffsetForHorizontal(line, x.toFloat())
        val spans = buffer.getSpans(off, off, URLSpan::class.java)
        if (spans.isEmpty()) return super.onTouchEvent(widget, buffer, event)

        val url = spans[0].url
        if (Config.newInstance(context).linkOpenWarningEnabled) {
            showWarning(url)
        } else {
            openUrl(url)
        }
        return true
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    private fun themedColor(attr: Int, fallback: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) {
                runCatching { context.getColor(value.resourceId) }.getOrDefault(fallback)
            } else {
                value.data
            }
        } else fallback
    }

    private fun makeButton(text: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(themedColor(android.R.attr.colorAccent, 0xff8ab4f8.toInt()))
            gravity = Gravity.CENTER
            isAllCaps = false
            minHeight = dp(48)
            minWidth = dp(92)
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { onClick() }
            background = null
            isClickable = true
            isFocusable = true
        }

    private fun showWarning(url: String) {
        val padding = dp(24)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, dp(20), padding, dp(4))
        }

        val info = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_dialog_info)
            contentDescription = null
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(10)
            }
        }
        root.addView(info)

        val title = TextView(context).apply {
            text = context.getString(R.string.link_warning_title)
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(themedColor(android.R.attr.textColorPrimary, Color.WHITE))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) }
        }
        root.addView(title)

        val message = TextView(context).apply {
            text = context.getString(R.string.link_open_warning_message)
            textSize = 18f
            setTextColor(themedColor(android.R.attr.textColorPrimary, Color.WHITE))
            setLineSpacing(0f, 1.12f)
        }
        root.addView(message)

        val urlView = TextView(context).apply {
            text = url
            textSize = 18f
            setTextColor(themedColor(android.R.attr.textColorPrimary, Color.WHITE))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
            background = GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(themedColor(android.R.attr.colorBackground, Color.DKGRAY).let {
                    // Keep the URL chip subtly separated from the dialog in both themes.
                    if (Color.luminance(it) < 0.5f) Color.WHITE.withAlpha(0.08f) else Color.BLACK.withAlpha(0.06f)
                })
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(10)
            }
        }
        root.addView(urlView)

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        }

        val dialog = AlertDialog.Builder(context)
            .setView(root)
            .create()

        buttons.addView(makeButton(context.getString(R.string.link_copy)) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("link", url))
            Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }, LinearLayout.LayoutParams(0, dp(56), 1f))

        buttons.addView(makeButton(context.getString(R.string.link_cancel)) {
            dialog.dismiss()
        }, LinearLayout.LayoutParams(0, dp(56), 1f))

        buttons.addView(makeButton(context.getString(R.string.link_open)) {
            dialog.dismiss()
            openUrl(url)
        }, LinearLayout.LayoutParams(0, dp(56), 1f))

        root.addView(buttons)
        dialog.show()
    }

    private fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun Int.withAlpha(alpha: Float): Int =
        Color.argb((alpha * 255).toInt(), Color.red(this), Color.green(this), Color.blue(this))
}
