package com.vvrock.edgevolume

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Hosts the edge bar as an accessibility overlay: no "draw over other apps" permission,
 * the system keeps it alive in the background, and it also works on the lock screen.
 */
class EdgeVolumeService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var wm: WindowManager
    private lateinit var audio: AudioManager
    private lateinit var prefs: SharedPreferences
    private var bar: EdgeBarView? = null

    private val params = WindowManager.LayoutParams(
        0, 0,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WindowManager::class.java)
        audio = getSystemService(AudioManager::class.java)
        prefs = Prefs.get(this)
        prefs.registerOnSharedPreferenceChangeListener(this)

        val view = EdgeBarView(this, ::adjustVolume)
        bar = view
        updateParams(view)
        wm.addView(view, params)
    }

    private fun adjustVolume(dir: Int) {
        val direction = when {
            dir > 0 -> AudioManager.ADJUST_RAISE
            dir < 0 -> AudioManager.ADJUST_LOWER
            else -> AudioManager.ADJUST_SAME
        }
        val flags = AudioManager.FLAG_SHOW_UI
        try {
            if (prefs.getBoolean(Prefs.MEDIA_ONLY, Prefs.DEF_MEDIA_ONLY)) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, flags)
            } else {
                // Same as the hardware keys: media while playing, call volume
                // during a call, ringer otherwise.
                audio.adjustSuggestedStreamVolume(direction, AudioManager.USE_DEFAULT_STREAM_TYPE, flags)
            }
        } catch (_: SecurityException) {
            // E.g. leaving Do Not Disturb via the ringer needs extra access; fall back to media.
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, flags)
        }
    }

    private fun updateParams(view: EdgeBarView) {
        val density = resources.displayMetrics.density
        val screenH = realScreenHeight()
        val enabled = prefs.getBoolean(Prefs.ENABLED, Prefs.DEF_ENABLED)
        val right = prefs.getBoolean(Prefs.SIDE_RIGHT, Prefs.DEF_SIDE_RIGHT)
        val length = (screenH * prefs.getInt(Prefs.LENGTH, Prefs.DEF_LENGTH) / 100f).toInt()
        val position = prefs.getInt(Prefs.POSITION, Prefs.DEF_POSITION) / 100f

        params.width = (prefs.getInt(Prefs.WIDTH_DP, Prefs.DEF_WIDTH_DP) * density).toInt()
        params.height = length
        params.gravity = (if (right) Gravity.RIGHT else Gravity.LEFT) or Gravity.TOP
        params.x = 0
        params.y = ((screenH - length) * position).toInt()
        params.flags = if (enabled) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        view.rightSide = right
        view.stepPx = prefs.getInt(Prefs.STEP_DP, Prefs.DEF_STEP_DP) * density
        view.idleAlpha = prefs.getInt(Prefs.OPACITY, Prefs.DEF_OPACITY) / 100f
        view.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
        view.invalidate()
    }

    private fun relayout() {
        val view = bar ?: return
        updateParams(view)
        if (view.isAttachedToWindow) wm.updateViewLayout(view, params)
    }

    @Suppress("DEPRECATION")
    private fun realScreenHeight(): Int {
        val display = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val dm = DisplayMetrics()
        display.getRealMetrics(dm)
        return dm.heightPixels
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) = relayout()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        relayout() // rotation changes the screen height
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(this)
        bar?.let { if (it.isAttachedToWindow) wm.removeView(it) }
        bar = null
        super.onDestroy()
    }
}
