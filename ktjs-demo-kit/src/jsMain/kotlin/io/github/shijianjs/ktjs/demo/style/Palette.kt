package io.github.shijianjs.ktjs.demo.style

import web.cssom.Color
import web.cssom.CustomPropertyName
import web.cssom.`var`
import web.cssom.rgb

/**
 * The colours every sheet of the site shares.
 *
 * They are the one visual thing that genuinely changes together, so they live here instead of being
 * repeated in each component's sheet. Each value is a Lumo custom property with a light fallback:
 * Lumo defines them on `:root` and flips them under `html[theme~="dark"]`, and custom properties are
 * inherited into every shadow tree — that is how one switch repaints the whole site without a single
 * component carrying its own `[data-theme]` selector.
 */
object Palette {
    /** Page background. */
    val background: Color = `var`(CustomPropertyName<Color>("--lumo-base-color"), rgb(255, 255, 255))

    /** Card and sidebar surface: a 5 % tint of the contrast colour. */
    val surface: Color = `var`(CustomPropertyName<Color>("--lumo-contrast-5pct"), rgb(246, 247, 251))

    /** A raised block inside a card, e.g. a code fragment. */
    val surfaceRaised: Color = `var`(CustomPropertyName<Color>("--lumo-contrast-10pct"), rgb(236, 239, 244))

    /** Hairlines. */
    val border: Color = `var`(CustomPropertyName<Color>("--lumo-contrast-20pct"), rgb(223, 228, 234))

    /** Body text. */
    val text: Color = `var`(CustomPropertyName<Color>("--lumo-body-text-color"), rgb(35, 39, 45))

    /** Secondary text: hints, captions, breadcrumb. */
    val muted: Color = `var`(CustomPropertyName<Color>("--lumo-secondary-text-color"), rgb(87, 98, 110))

    /** Links and the active route. */
    val accent: Color = `var`(CustomPropertyName<Color>("--lumo-primary-text-color"), rgb(0, 92, 207))
}
