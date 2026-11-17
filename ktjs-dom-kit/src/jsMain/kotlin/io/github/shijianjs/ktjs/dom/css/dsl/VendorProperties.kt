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
 * `-webkit-background-clip` in a rules object.
 *
 * The key has to be the **camelCase** spelling: `@emotion/serialize` hyphenates camelCase keys itself
 * (`WebkitBackgroundClip` → `-webkit-background-clip`), and in development it rejects a key that is
 * already kebab-case — logging "Using kebab-case for css properties in objects is not supported" and
 * dropping the declaration. `webkitBackgroundClipName` above is the opposite case: that string is
 * handed to CSS `@supports`, which wants the real property name.
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
    asDynamic()["WebkitBackgroundClip"] = value
}
