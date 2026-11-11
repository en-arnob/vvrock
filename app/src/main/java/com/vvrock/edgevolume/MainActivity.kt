package com.vvrock.edgevolume

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

/** Small settings screen. Changes apply to the bar live. */
class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var status: TextView
    private lateinit var root: LinearLayout
    private val dp get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.get(this)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (20 * dp).toInt()
            setPadding(p, p, p, p)
        }

        status = TextView(this).apply { textSize = 16f }
        root.addView(status)
        root.addView(Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        root.addView(CheckBox(this).apply {
            text = "Show edge bar"
            isChecked = prefs.getBoolean(Prefs.ENABLED, Prefs.DEF_ENABLED)
            setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean(Prefs.ENABLED, c).apply() }
        })

        header("Edge")
        val left = RadioButton(this).apply { text = "Left"; id = 1 }
        val right = RadioButton(this).apply { text = "Right"; id = 2 }
        root.addView(RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            addView(left)
            addView(right)
            check(if (prefs.getBoolean(Prefs.SIDE_RIGHT, Prefs.DEF_SIDE_RIGHT)) 2 else 1)
            setOnCheckedChangeListener { _, id -> prefs.edit().putBoolean(Prefs.SIDE_RIGHT, id == 2).apply() }
        })

        slider("Position (top to bottom)", Prefs.POSITION, Prefs.DEF_POSITION, 0, 100) { "$it%" }
        slider("Length", Prefs.LENGTH, Prefs.DEF_LENGTH, 10, 90) { "$it% of screen" }
        slider("Touch width", Prefs.WIDTH_DP, Prefs.DEF_WIDTH_DP, 6, 40) { "$it dp" }
        slider("Visibility", Prefs.OPACITY, Prefs.DEF_OPACITY, 0, 100) { "$it%" }
        slider("Slide distance per step (lower = faster)", Prefs.STEP_DP, Prefs.DEF_STEP_DP, 6, 60) { "$it dp" }

        header("Volume")
        root.addView(CheckBox(this).apply {
            text = "Always control media volume\n(off = like hardware keys: media when playing, ringer otherwise)"
            isChecked = prefs.getBoolean(Prefs.MEDIA_ONLY, Prefs.DEF_MEDIA_ONLY)
            setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean(Prefs.MEDIA_ONLY, c).apply() }
        })

        root.addView(TextView(this).apply {
            text = "\nSlide up = louder, slide down = quieter, tap = show volume panel."
            alpha = 0.7f
        })

        root.addView(TextView(this).apply {
            text = "Made with ❤️ by Khalid Utsob"
            gravity = Gravity.CENTER_HORIZONTAL
            alpha = 0.75f
            val pad = (12 * dp).toInt()
            setPadding(pad, (32 * dp).toInt(), pad, pad)
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://khalidutsob.com")))
                } catch (_: ActivityNotFoundException) {
                    // No browser installed; nothing to do.
                }
            }
        })

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        })
    }

    override fun onResume() {
        super.onResume()
        status.text = if (isServiceEnabled()) {
            "Edge bar service is running."
        } else {
            "Service is OFF. Enable \"${getString(R.string.service_label)}\" in Accessibility settings.\n" +
                "(Android 13+: if it is greyed out, open App info > menu > Allow restricted settings.)"
        }
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, EdgeVolumeService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun header(title: String) {
        root.addView(TextView(this).apply {
            text = title
            textSize = 18f
            setPadding(0, (20 * dp).toInt(), 0, (4 * dp).toInt())
        })
    }

    private fun slider(
        title: String, key: String, def: Int, min: Int, max: Int, fmt: (Int) -> String,
    ) {
        val label = TextView(this).apply { setPadding(0, (12 * dp).toInt(), 0, 0) }
        val value = prefs.getInt(key, def)
        fun show(v: Int) { label.text = "$title: ${fmt(v)}" }
        show(value)
        root.addView(label)
        root.addView(SeekBar(this).apply {
            this.min = min
            this.max = max
            progress = value
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    show(p)
                    prefs.edit().putInt(key, p).apply()
                }
                override fun onStartTrackingTouch(sb: SeekBar) = Unit
                override fun onStopTrackingTouch(sb: SeekBar) = Unit
            })
        })
    }
}
