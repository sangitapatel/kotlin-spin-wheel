/**
 * MainActivity.kt  —  SpinWheel Demo
 *
 * Author : Sangita Patel
 * GitHub : https://github.com/sangitapatel
 * License: MIT — Copyright (c) 2026 Sangita Patel
 */

package com.sangitapatel.spinwheel.demo

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sangitapatel.spinwheel.SpinWheelView
import com.sangitapatel.spinwheel.WheelSlice
import com.sangitapatel.spinwheel.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    // 8 prize slices — alternate colors for visual contrast
    private val prizeSlices = listOf(
        WheelSlice(
            label = "₹10",
            fillColor = Color.parseColor("#FF5252"), textColor = Color.WHITE, weight = 1f, tag = 10
        ),

        WheelSlice(
            label = "₹50",
            fillColor = Color.parseColor("#FF9800"), textColor = Color.WHITE, weight = 1f, tag = 50
        ),

        WheelSlice(
            label = "₹100",
            fillColor = Color.parseColor("#FFC107"), textColor = Color.BLACK, weight = 1f, tag = 100
        ),

        WheelSlice(
            label = "₹200",
            fillColor = Color.parseColor("#4CAF50"), textColor = Color.WHITE, weight = 1f, tag = 200
        ),

        WheelSlice(
            label = "₹300",
            fillColor = Color.parseColor("#009688"), textColor = Color.WHITE, weight = 1f, tag = 300
        ),

        WheelSlice(
            label = "₹400",
            fillColor = Color.parseColor("#03A9F4"), textColor = Color.WHITE, weight = 1f, tag = 400
        ),

        WheelSlice(
            label = "₹500",
            fillColor = Color.parseColor("#3F51B5"), textColor = Color.WHITE, weight = 1f, tag = 500
        ),

        WheelSlice(
            label = "🎁 Gift",
            fillColor = Color.parseColor("#9C27B0"), textColor = Color.WHITE, weight = 1f, tag = -1
        ),

        WheelSlice(
            label = "Try Again",
            fillColor = Color.parseColor("#607D8B"), textColor = Color.WHITE, weight = 2f, tag = 0
        ),

        WheelSlice(
            label = "₹1000",
            fillColor = Color.parseColor("#E91E63"),
            textColor = Color.WHITE,
            weight = 1f,
            tag = 1000
        ),

        WheelSlice(
            label = "₹2000",
            fillColor = Color.parseColor("#D32F2F"),
            textColor = Color.WHITE,
            weight = 1f,
            tag = 2000
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Set slices
        b.spinWheel.slices = prizeSlices

        // Listener
        b.spinWheel.spinListener = object : SpinWheelView.OnSpinListener {
            override fun onSpinStart(view: SpinWheelView) {
                b.btnSpin.isEnabled = false
                b.tvResultLabel.text = "Spinning…"
                b.tvResultValue.text = "🎡"
            }

            override fun onSpinEnd(view: SpinWheelView, slice: WheelSlice, index: Int) {
                b.btnSpin.isEnabled = true
                b.tvResultLabel.text = "🎉 You landed on:"
                b.tvResultValue.text = slice.label

                val prize = slice.tag as? Int ?: 0
                val msg = when {
                    prize > 0 -> "Congratulations! You won ${slice.label}! 🎊"
                    prize == -1 -> "Wow! You won a special Gift! 🎁"
                    else -> "Better luck next time! 😊"
                }
//                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Spin button
        b.btnSpin.setOnClickListener {
            b.spinWheel.spin()   // random winner based on weights
        }
    }
}
