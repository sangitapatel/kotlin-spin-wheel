# 🎡 SpinWheel

<p align="center">
  <img src="assets/ss1.jpeg" width="300" alt="SpinWheel demo — colorful lucky wheel with prize labels"/>
</p>

<p align="center">
  <a href="https://jitpack.io/#sangitapatel/SpinWheel"><img src="https://jitpack.io/v/sangitapatel/SpinWheel.svg" alt="JitPack"/></a>
  <a href="https://android-arsenal.com/api?level=21"><img src="https://img.shields.io/badge/API-21%2B-brightgreen.svg" alt="API 21+"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License"/></a>
  <a href="https://github.com/sangitapatel"><img src="https://img.shields.io/badge/Author-Sangita%20Patel-orange.svg" alt="Author"/></a>
</p>

<p align="center">
  A fully custom Android <strong>Lucky Wheel</strong> view with weighted slices, guaranteed winners,<br/>
  bounce animation, and zero third-party dependencies.
</p>

---

## Features

| Feature | Details |
|---|---|
| Any number of slices | Minimum 2, no upper limit |
| Weighted probability | `weight = 2f` → twice the chance of winning |
| Guaranteed winner | `spin(targetIndex)` → server-controlled result |
| Tap-to-spin | Single tap on the wheel triggers spin |
| Bounce overshoot | Natural deceleration with landing bounce |
| Custom interpolator | `DeceleratingSpinInterpolator` — cubic ease-out |
| Off-screen bitmap cache | Smooth 60 fps rendering |
| Pointer / needle | Fully customisable color, size and position |
| Hub circle | Toggle on/off, custom color and size |
| Border + dividers | Configurable width and color |
| Spin callbacks | `onSpinStart` / `onSpinEnd(slice, index)` |
| Programmatic control | `spin()`, `stopNow()`, `resetAngle()` |
| API 21+ | Works on all modern Android devices |
| Zero dependencies | No Glide, no RxJava — nothing that conflicts with your stack |

---

## Installation

### Step 1 — Add JitPack to `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }   // ← add this
    }
}
```

<details>
<summary>Using the older Groovy DSL? (build.gradle)</summary>

```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```
</details>

### Step 2 — Add the dependency

```kotlin
// build.gradle.kts (app module)
dependencies {
    implementation("com.github.sangitapatel:SpinWheel:1.0.0")
}
```

---

## Quick Start

### 1. Add to your XML layout

```xml
<com.sangitapatel.spinwheel.SpinWheelView
    android:id="@+id/spinWheel"
    android:layout_width="300dp"
    android:layout_height="300dp"
    app:swv_pointerColor="#E53935"
    app:swv_borderColor="#37474F"
    app:swv_borderWidth="6dp"
    app:swv_dividerColor="#FFFFFF"
    app:swv_dividerWidth="2dp"
    app:swv_labelTextSize="13sp"
    app:swv_tapToSpin="true"
    app:swv_bounceEnabled="true"
    app:swv_showHub="true"
    app:swv_hubColor="#FFFFFF"
    app:swv_hubRadiusFraction="0.12"
    app:swv_minSpinDuration="3000"
    app:swv_maxSpinDuration="6000"/>
```

> **Note:** `SpinWheelView` is always square. Set `layout_width` = `layout_height`,
> or use `app:layout_constraintDimensionRatio="1:1"`.

### 2. Set slices in Kotlin

```kotlin
val wheel = findViewById<SpinWheelView>(R.id.spinWheel)

wheel.slices = listOf(
    WheelSlice("₹100",      Color.parseColor("#E53935"), weight = 1f, tag = 100),
    WheelSlice("Try Again", Color.parseColor("#1E88E5"), weight = 2f, tag = 0),   // 2× chance
    WheelSlice("₹500",      Color.parseColor("#43A047"), weight = 1f, tag = 500),
    WheelSlice("🎁 Gift",   Color.parseColor("#8E24AA"), weight = 1f, tag = -1),
    WheelSlice("₹1000",     Color.parseColor("#E91E63"), weight = 1f, tag = 1000),
)
```

### 3. Attach a listener and spin

```kotlin
wheel.spinListener = object : SpinWheelView.OnSpinListener {
    override fun onSpinStart(view: SpinWheelView) {
        btnSpin.isEnabled = false
    }
    override fun onSpinEnd(view: SpinWheelView, slice: WheelSlice, index: Int) {
        btnSpin.isEnabled = true
        Toast.makeText(this@MainActivity,
            "You won: ${slice.label}", Toast.LENGTH_SHORT).show()
        // slice.tag  → your payload (Int, String, or any object)
        // index      → position in the slices list (0-based)
    }
}

btnSpin.setOnClickListener { wheel.spin() }
```

---

## WheelSlice

```kotlin
WheelSlice(
    label     = "₹500",                         // text shown in slice (emoji supported ✅)
    fillColor = Color.parseColor("#43A047"),     // slice background color
    textColor = Color.WHITE,                     // label color (default = WHITE)
    weight    = 1f,                              // relative probability — see below
    tag       = 500                              // Any? payload returned at onSpinEnd
)
```

### Weighted probability

The winner is selected randomly in proportion to weights. A slice with `weight = 3f` is three times more likely to be chosen than a slice with `weight = 1f`.

```kotlin
WheelSlice("₹1000",     ..., weight = 1f)   // ~25 % chance
WheelSlice("Try Again", ..., weight = 3f)   // ~75 % chance
```

---

## Guaranteed Winner (server-controlled)

Use `spin(targetIndex)` when your backend decides the outcome before the animation plays.

```kotlin
wheel.spin(targetIndex = 2)      // always lands on index 2
wheel.spin()                     // random — uses weights
wheel.spin(targetIndex = -1)     // same as above (random)
```

---

## XML Attributes

| Attribute | Type | Default | Description |
|---|---|---|---|
| `swv_pointerColor` | color | `#E53935` | Needle / pointer fill color |
| `swv_dividerWidth` | dimension | `2dp` | Line between slices |
| `swv_dividerColor` | color | `#FFFFFF` | Divider line color |
| `swv_borderWidth` | dimension | `6dp` | Outer ring width |
| `swv_borderColor` | color | `#37474F` | Outer ring color |
| `swv_labelTextSize` | dimension | `14sp` | Slice label text size |
| `swv_labelTextColor` | color | `#FFFFFF` | Default label color |
| `swv_minSpinDuration` | integer (ms) | `3000` | Minimum spin duration |
| `swv_maxSpinDuration` | integer (ms) | `6000` | Maximum spin duration |
| `swv_tapToSpin` | boolean | `true` | Tap the wheel to spin |
| `swv_bounceEnabled` | boolean | `true` | Bounce overshoot at landing |
| `swv_showHub` | boolean | `true` | Show the center hub circle |
| `swv_hubColor` | color | `#FFFFFF` | Hub fill color |
| `swv_hubRadiusFraction` | float | `0.12` | Hub radius as fraction of wheel radius (range: `0.05`–`0.40`) |

---

## Full Public API

```kotlin
// ── Slices ─────────────────────────────────────────────────────
wheel.slices = listOf(...)          // set slices (min 2 required)

// ── Appearance ─────────────────────────────────────────────────
wheel.pointerColor      = Color.RED
wheel.borderColor       = Color.DKGRAY
wheel.borderWidth       = 6f                  // pixels
wheel.dividerColor      = Color.WHITE
wheel.dividerWidth      = 2f                  // pixels
wheel.labelTextSize     = 14f                 // sp (in pixels)
wheel.showHub           = true
wheel.hubColor          = Color.WHITE
wheel.hubRadiusFraction = 0.12f               // 0.05 – 0.40

// ── Behaviour ──────────────────────────────────────────────────
wheel.tapToSpin         = true
wheel.bounceEnabled     = true
wheel.minSpinDuration   = 3_000L              // ms
wheel.maxSpinDuration   = 6_000L              // ms
wheel.spinListener      = myListener

// ── Control ────────────────────────────────────────────────────
wheel.spin()                        // random (weighted)
wheel.spin(targetIndex = 2)         // guaranteed winner
wheel.stopNow()                     // hard stop — no callback fired, isCurrentlySpinning → false
wheel.resetAngle()                  // rotate wheel back to 0°

// ── State ──────────────────────────────────────────────────────
val spinning: Boolean = wheel.isCurrentlySpinning
```

> **`stopNow()` note:** Stops the animation immediately without firing `onSpinEnd`.
> `isCurrentlySpinning` returns `false` afterward and a new `spin()` call is safe.

> **Minimum slices:** Passing fewer than 2 slices throws `IllegalArgumentException`.

---

## License

This project is licensed under the [MIT License](LICENSE).

```
MIT License — Copyright (c) 2026 Sangita Patel
https://github.com/sangitapatel
```
