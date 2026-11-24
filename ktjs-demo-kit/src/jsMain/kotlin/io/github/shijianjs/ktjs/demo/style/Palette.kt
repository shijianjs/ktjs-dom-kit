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
 * component carrying its own `[data-theme]` selector. [page] is the one exception, and says why.
 */
object Palette {
    /**
     * The canvas the shell's cards float on.
     *
     * It has to be a step *below* [background], and Lumo has no token for that: its `--lumo-contrast-*`
     * ladder goes the other way in the dark theme, where 5 % contrast is *lighter* than the base. So the
     * pair is spelled with `light-dark()`, which is how Lumo 25 flips its own palette: it follows the
     * `color-scheme` [Theme] writes on the document, and that inherits into every shadow tree, so no
     * component needs a `[theme]` selector. The values are deliberately neutral (a slate-tinted grey
     * rather than a hue), because they are the only colours of the site that are not derived from the
     * theme.
     */
    val page: Color = Color("light-dark(#e9edf2, #14181c)")

    /** A card's surface: the base colour, i.e. white in the light theme and the slate base in the dark one. */
    val background: Color = `var`(CustomPropertyName<Color>("--lumo-base-color"), rgb(255, 255, 255))

    /** A block inside a card, one step above its surface: a nested panel, a selected row, a badge's fill. */
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
