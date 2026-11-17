package io.github.shijianjs.ktjs.demo

import web.cssom.Color

/**
 * The colours every sheet of the site shares.
 *
 * They are the one visual thing that genuinely changes together, so they live here instead of being
 * repeated in each component's sheet. Values stay plain `Color`s: the dark theme is Lumo's
 * `html[theme~="dark"]`, and these are the neutral tones the demo's own chrome uses in both themes.
 */
object Palette {
    val background: Color = Color("#0f1115")

    val surface: Color = Color("#171a21")

    val surfaceRaised: Color = Color("#1e222b")

    val border: Color = Color("#2a2f3a")

    val text: Color = Color("#e6e9ef")

    val muted: Color = Color("#9aa3af")

    val accent: Color = Color("#7aa2ff")

    val accentText: Color = Color("#0b1020")
}
