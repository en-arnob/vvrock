package com.vvrock.edgevolume

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "edge_bar"

    const val ENABLED = "enabled"          // bar shown & touchable
    const val SIDE_RIGHT = "side_right"    // true = right edge, false = left edge
    const val POSITION = "position"        // 0 = top, 100 = bottom
    const val LENGTH = "length"            // % of screen height
    const val WIDTH_DP = "width_dp"        // touch strip width
    const val OPACITY = "opacity"          // idle visibility of the line, 0..100
    const val STEP_DP = "step_dp"          // finger travel per volume step
    const val MEDIA_ONLY = "media_only"    // always control media instead of the "smart" stream

    const val DEF_ENABLED = true
    const val DEF_SIDE_RIGHT = true
    const val DEF_POSITION = 45
    const val DEF_LENGTH = 35
    const val DEF_WIDTH_DP = 14
    const val DEF_OPACITY = 20
    const val DEF_STEP_DP = 22
    const val DEF_MEDIA_ONLY = false

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
