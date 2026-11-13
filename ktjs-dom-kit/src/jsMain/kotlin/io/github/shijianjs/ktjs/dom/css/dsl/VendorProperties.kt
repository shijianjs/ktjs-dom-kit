package io.github.shijianjs.ktjs.dom.css.dsl

import csstype.PropertiesBuilder
import js.reflect.unsafeCast
import web.cssom.BackgroundClip
import web.cssom.PropertyName

/**
 * `-webkit-background-clip` as a typed property name. cssom models `background-clip` but not the
 * prefixed property, and `background-clip: text` is still prefixed-only in Safari — the gradient
 * text needs both the declaration and the supports condition, so the name is shared with
 * `supportsDeclaration`.
 */
val webkitBackgroundClipName: PropertyName<BackgroundClip> = unsafeCast("-webkit-background-clip")

/**
 * `-webkit-background-clip` in a rules object; `@emotion/serialize` hyphenates camelCase keys and
 * leaves a key that already starts with `-` alone, so this serializes exactly as written:
 *
 * ```kotlin
 * cssSheet {
 *     titleClass {
 *         backgroundImage = linearGradient(120.deg, colorStop(accent), colorStop(accentBright))
 *         webkitBackgroundClip(BackgroundClip.text)
 *         backgroundClip = BackgroundClip.text
 *         color = Color.transparent
 *     }
 * }
 * ```
 *
 * Add one function per missing vendor property, in this file, with the value typed — a generic
 * `putRaw(name, value)` at call sites would put the strings back that `cssSheet` exists to remove.
 */
fun PropertiesBuilder.webkitBackgroundClip(value: BackgroundClip) {
    asDynamic()["$webkitBackgroundClipName"] = value
}
