# 🎡 android-spin-wheel

[![](https://jitpack.io/v/sangitapatel/android-spin-wheel.svg)](https://jitpack.io/#sangitapatel/android-spin-wheel)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Author](https://img.shields.io/badge/Author-Sangita%20Patel-orange.svg)](https://github.com/sangitapatel)

**Android Spin Wheel** — a fully custom Lucky Wheel / Prize Wheel View library for Android, built entirely by [Sangita Patel](https://github.com/sangitapatel).
Zero dependencies. No copy. 100% original Kotlin.

> Keywords: android spin wheel, lucky wheel android, prize wheel view, android fortune wheel, spinning wheel android library, android custom wheel view, kotlin spin wheel

---

## 📸 Preview

<p align="center">
  <img src="assets/spinwheel_demo.gif" width="280" alt="Android Spin Wheel demo — lucky wheel prize wheel"/>
</p>

---

## ✨ Features

| Feature | Details |
|---|---|
| Custom slices | Any number of slices (min 2) |
| Weighted slices | `weight=2f` = twice the chance of winning |
| Guaranteed winner | `spin(targetIndex)` — server-controlled result |
| Pointer position | Top or bottom pointer support |
| Tap-to-spin | Single tap on wheel triggers spin |
| Bounce overshoot | Natural coin-spin feel at landing |
| Custom interpolator | `DeceleratingSpinInterpolator` — cubic ease-out |
| Off-screen bitmap cache | Smooth 60 fps animation |
| Pointer / needle | Fully customisable color and size |
| Hub circle | Toggle on/off, custom color and size |
| Border + divider | Configurable width and color |
| Spin callbacks | `onSpinStart` / `onSpinEnd(slice, index)` |
| Programmatic control | `spin()`, `stopNow()`, `resetAngle()` |
| API 21+ | Works on all modern Android devices |
| Zero dependencies | No Glide, no RxJava — nothing that conflicts with your existing dependency tree |

---

## 🚀 Installation

### Step 1 — `settings.gradle.kts` (Gradle 7+)

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // ← add this
    }
}
```

<details>
<summary>Using older Groovy DSL? Click here</summary>

In your **project-level** `build.gradle`:
```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // ← add this
    }
}
```
</details>

### Step 2 — `build.gradle.kts` (app module)

```kotlin
dependencies {
    implementation("com.github.sangitapatel:android-spin-wheel:1.0.0")
}
```

---

## ⚡ Quick Start

### 1. Add to XML layout

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

> **Note:** SpinWheelView is always square. Set `layout_width` = `layout_height`
> or use `constraintDimensionRatio="1:1"`.

### 2. Set slices (Kotlin)

```kotlin
val wheel = findViewById<SpinWheelView>(R.id.spinWheel)

wheel.slices = listOf(
    WheelSlice("₹100",      Color.parseColor("#E53935"), weight = 1f, tag = 100),
    WheelSlice("Try Again", Color.parseColor("#1E88E5"), weight = 2f, tag = 0),
    WheelSlice("₹500",      Color.parseColor("#43A047"), weight = 1f, tag = 500),
    WheelSlice("🎁 Gift",   Color.parseColor("#8E24AA"), weight = 1f, tag = -1),
    WheelSlice("₹1000",     Color.parseColor("#E91E63"), weight = 1f, tag = 1000),
)
```

### 3. Add listener + trigger

```kotlin
wheel.spinListener = object : SpinWheelView.OnSpinListener {
    override fun onSpinStart(view: SpinWheelView) {
        btnSpin.isEnabled = false
    }
    override fun onSpinEnd(view: SpinWheelView, slice: WheelSlice, index: Int) {
        btnSpin.isEnabled = true
        Toast.makeText(this@MainActivity,
            "You won: ${slice.label}", Toast.LENGTH_SHORT).show()
        // slice.tag  → your custom payload (Int, String, or any object)
        // index      → position in the slices list (0-based)
    }
}

btnSpin.setOnClickListener { wheel.spin() }
```

---

## 🧩 WheelSlice — All Parameters

```kotlin
WheelSlice(
    label     = "₹500",                         // text shown in slice (emoji OK ✅)
    fillColor = Color.parseColor("#43A047"),     // slice background color
    textColor = Color.WHITE,                     // label text color (default = WHITE)
    weight    = 1f,                              // relative probability weight (Any positive Float)
    tag       = 500                              // Any? payload returned in onSpinEnd
)
```

### Weighted Probability Example

```kotlin
// "Try Again" has 3× more chance than "₹1000"
WheelSlice("₹1000",     ..., weight = 1f)
WheelSlice("Try Again", ..., weight = 3f)
```

---

## 🎯 Guaranteed Winner (Server-controlled)

```kotlin
// Force the wheel to always land on index 2
wheel.spin(targetIndex = 2)

// Random winner based on weights (default)
wheel.spin()
// or
wheel.spin(targetIndex = -1)
```

---

## 🛡️ Edge Cases

| Situation | Behavior |
|---|---|
| `slices` count < 2 | Throws `IllegalArgumentException` — min 2 slices required |
| `stopNow()` called mid-spin | Hard stop, no `onSpinEnd` callback fired. `isCurrentlySpinning` becomes `false` immediately. Calling `spin()` again after `stopNow()` is safe. |
| `hubRadiusFraction` valid range | `0.05f` to `0.40f`. Values outside this range are clamped automatically. |

---

## 📐 XML Attributes

| Attribute | Type | Default | Description |
|---|---|---|---|
| `swv_pointerColor` | color | `#E53935` | Needle / pointer color |
| `swv_dividerWidth` | dimension | `2dp` | Line between slices |
| `swv_dividerColor` | color | `#FFFFFF` | Divider line color |
| `swv_borderWidth` | dimension | `6dp` | Outer ring width |
| `swv_borderColor` | color | `#37474F` | Outer ring color |
| `swv_labelTextSize` | dimension | `14sp` | Slice label text size |
| `swv_labelTextColor` | color | `#FFFFFF` | Default label color |
| `swv_minSpinDuration` | integer (ms) | `3000` | Min spin time |
| `swv_maxSpinDuration` | integer (ms) | `6000` | Max spin time |
| `swv_tapToSpin` | boolean | `true` | Tap wheel to spin |
| `swv_bounceEnabled` | boolean | `true` | Bounce overshoot at landing |
| `swv_showHub` | boolean | `true` | Show center hub circle |
| `swv_hubColor` | color | `#FFFFFF` | Hub fill color |
| `swv_hubRadiusFraction` | float (0.05–0.40) | `0.12` | Hub size (fraction of wheel radius) |

---

## 🔧 Full Public API

```kotlin
// ── Data ──────────────────────────────────────────────────────
wheel.slices = listOf(...)         // set slices (min 2, throws if fewer)

// ── Configuration ──────────────────────────────────────────────
wheel.pointerColor      = Color.RED
wheel.borderColor       = Color.DKGRAY
wheel.borderWidth       = 6f                  // px
wheel.dividerColor      = Color.WHITE
wheel.dividerWidth      = 2f                  // px
wheel.labelTextSize     = 14f                 // sp in px
wheel.tapToSpin         = true
wheel.bounceEnabled     = true
wheel.showHub           = true
wheel.hubColor          = Color.WHITE
wheel.hubRadiusFraction = 0.12f               // valid range: 0.05–0.40
wheel.minSpinDuration   = 3_000L              // ms
wheel.maxSpinDuration   = 6_000L              // ms
wheel.spinListener      = myListener

// ── Control ────────────────────────────────────────────────────
wheel.spin()                       // random (weighted)
wheel.spin(targetIndex = 2)        // guaranteed winner
wheel.stopNow()                    // hard stop, no callback, safe to call spin() after
wheel.resetAngle()                 // rotate back to 0°

// ── Read-only ──────────────────────────────────────────────────
val spinning: Boolean = wheel.isCurrentlySpinning
```

---

## 📦 Publish to JitPack

<details>
<summary>Steps for maintainers — click to expand</summary>

### Step 1 — Push to GitHub

```bash
git init
git add .
git commit -m "feat: SpinWheel v1.0.0 by Sangita Patel"
git branch -M main
git remote add origin https://github.com/sangitapatel/android-spin-wheel.git
git push -u origin main
```

### Step 2 — Create version tag

```bash
git tag 1.0.0
git push origin 1.0.0
```

Or on GitHub: **Releases → Draft a new release → Tag: `1.0.0` → Publish**

### Step 3 — Trigger JitPack

Open in browser:
```
https://jitpack.io/#sangitapatel/android-spin-wheel/1.0.0
```
Click **"Get it"** → wait for green ✅ → library is live!

</details>

---

## 🏷️ Suggested GitHub Topics

Add these in your repo settings for better discoverability:

`android` `spin-wheel` `lucky-wheel` `prize-wheel` `android-library` `kotlin` `jitpack` `custom-view` `fortune-wheel` `android-spin-wheel`

---

## 📄 License

MIT License — Copyright (c) 2026 Sangita Patel
[https://github.com/sangitapatel](https://github.com/sangitapatel)

See [LICENSE](LICENSE) for full text.
