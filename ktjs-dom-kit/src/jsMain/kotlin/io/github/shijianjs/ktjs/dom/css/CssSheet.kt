package io.github.shijianjs.ktjs.dom.css

import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import csstype.PropertiesBuilder
import io.github.shijianjs.ktjs.dom.internal.compile
import io.github.shijianjs.ktjs.dom.internal.middleware
import io.github.shijianjs.ktjs.dom.internal.serialize
import io.github.shijianjs.ktjs.dom.internal.serializeStyles
import io.github.shijianjs.ktjs.dom.internal.stringify
import js.array.jsArrayOf
import js.objects.Object
import js.objects.unsafeJso
import js.reflect.unsafeCast
import kotlin.js.JsAny
import web.cssom.CSSStyleSheet

/**
 * Builds a stylesheet from a csstype rules object.
 *
 * Properties written on the receiver become the declarations of their enclosing rule; a nested block
 * keyed by a selector becomes a rule of its own:
 *
 * ```kotlin
 * private val cardClass = ClassName("card")
 *
 * internal val CARD_SHEET: CSSStyleSheet = cssSheet {
 *     cardClass {
 *         border = Border(1.px, LineStyle.solid, var(CustomPropertyName<Color>("--card-border"), NamedColor.black))
 *         transition = many(Transition(PropertyName.borderColor, 0.2.s, TransitionTimingFunction.ease))
 *         Pseudos.hover { borderColor = NamedColor.white }
 *     }
 * }
 * ```
 *
 * Serialization is the two packages emotion's cache uses (`@emotion/serialize` + stylis), so the
 * sheet holds what emotion would emit: nested selectors resolved against their parent, values
 * normalized (`0.2s ease` stays, `0` may become `0px`), `fallback(…)` arrays expanded into repeated
 * declarations. The whole pipeline, including what does *not* work and why, is the docs site's
 * "Typed stylesheets" page (`docs/src/jsMain/resources/docs/dom-kit/css-sheet.md`).
 *
 * The top level is a map of **selectors** (`ClassName`, `":host"`); state variants are written with
 * `pseudo` / `pseudos` (see `css/dsl/Pseudos.kt`), and at-rules come from
 * the wrappers' own helpers — `` `@media`(MediaType.screen and maxWidth(576.px)) `` and
 * [supports][io.github.shijianjs.ktjs.dom.css.dsl.supports] — rather than a hand-written `"@media …"` key.
 * A rules object that carries only declarations is not a sheet. One sheet per component class,
 * adopted by every instance through [attachStyledShadow] — never one per instance.
 */
fun cssSheet(block: PropertiesBuilder.() -> Unit): CSSStyleSheet =
    CSSStyleSheet().apply { replaceSync(cssText(rulesObject(block))) }

/** The CSS text [cssSheet] would parse — for tests, and for eyeballing what the DSL emits. */
fun cssText(block: PropertiesBuilder.() -> Unit): String = cssText(rulesObject(block))

/** Serializes an already-built rules object; see [cssSheet] for the contract. */
fun cssText(rules: PropertiesBuilder): String = buildString {
    val source = rules.asDynamic()
    val selectors = Object.keys(rules.unsafeCast<JsAny>())
    val middlewares = stylisMiddlewares
    for (index in 0 until selectors.length) {
        val selector = selectors[index].toString()
        val declarations = serializeStyles(jsArrayOf<JsAny?>(source[selector] as JsAny?)).styles
        append(serialize(compile("$selector{$declarations}"), middlewares))
    }
}

private fun rulesObject(block: PropertiesBuilder.() -> Unit): PropertiesBuilder =
    unsafeJso<PropertiesBuilder>().apply(block)

/**
 * `stringify` only: `prefixer` would add `-webkit-` / `-ms-` fallbacks and emotion 11 does not use it
 * either, so a sheet from here matches what emotion would emit. Built once — it is a middleware
 * array, not a per-call value.
 */
private val stylisMiddlewares: JsAny? by lazy {
    middleware(jsArrayOf(stringify))
}
