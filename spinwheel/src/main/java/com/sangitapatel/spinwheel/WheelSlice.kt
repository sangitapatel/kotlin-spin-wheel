/**
 * WheelSlice.kt
 *
 * Author  : Sangita Patel
 * GitHub  : https://github.com/sangitapatel
 * License : MIT — Copyright (c) 2026 Sangita Patel
 *
 * Data model representing one slice of the SpinWheelView.
 */

package com.sangitapatel.spinwheel

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Represents a single slice on the spin wheel.
 *
 * @param label       Text displayed inside the slice (emoji supported ✅).
 * @param fillColor   Background colour of the slice.
 * @param textColor   Label colour; defaults to white.
 * @param weight      Relative size of this slice. Default = 1f (equal slices).
 *                    A slice with weight=2 is twice as wide as one with weight=1.
 * @param tag         Optional payload — any object you want back when this slice wins.
 */
data class WheelSlice(
    val label: String,
    @ColorInt val fillColor: Int,
    @ColorInt val textColor: Int = Color.WHITE,
    val weight: Float = 1f,
    val tag: Any? = null
) {
    init {
        require(label.isNotBlank()) { "WheelSlice label must not be blank." }
        require(weight > 0f) { "WheelSlice weight must be > 0." }
    }
}
