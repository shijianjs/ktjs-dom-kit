package io.github.shijianjs.ktjs.dom.css.dsl

import csstype.RuleBuilder
import js.reflect.unsafeCast
import web.cssom.Selector
import web.cssom.many

/**
 * Typed pseudo variants, in the shape of `web.dom.TagName` (an external interface plus a
 * constructor-like factory) and `web.html.HtmlTagName` (an object listing every constant).
 *
 * The list is the complete set of selectors from `csstype.SimplePseudosRuleBuilder` (64 entries), but
 * each one is prefixed with `&`. That fixes two things at once:
 *
 * - the upstream `hover {}` / `before {}` helpers emit `":hover"` / `"::before"`, which stylis reads
 *   as a **descendant** selector (JetBrains/kotlin-wrappers#2933);
 * - and no upstream fix can express `"&:hover, &:focus-visible"` — one block for several states —
 *   because those helpers are one-pseudo methods. Here `many` joins any number of variants.
 *
 * ```kotlin
 * cssSheet {
 *     cardClass {
 *         Pseudos.hover { borderColor = accent }                         // same shape as the wrapper's hover {}
 *         pseudos(Pseudos.hover, Pseudos.focusVisible) { borderColor = accent }
 *     }
 * }
 * ```
 *
 * The short form is [Pseudo.invoke]: a context parameter picks up the enclosing `RuleBuilder` (the
 * `PropertiesBuilder` that `cssSheet { … }` provides), so a `Pseudo` value can be invoked just like
 * `csstype`'s `hover {}` — except that ours carries `&` and can be combined. `many` joins either
 * `Selector`s or `Pseudo`s: `many(Pseudos.hover, Pseudos.focusVisible)` is a `Pseudo`
 * (`"&:hover,&:focus-visible"`), and `many(Selector("&:hover"), Selector("&:focus-visible"))` is a
 * `Selector`. Invoking that result needs parentheses — `(many(…)) { … }` — because `many` itself has
 * a `vararg`; for readability prefer `pseudos(…)`. `Pseudo("&:not(...)")` covers selectors outside
 * the list below.
 */
external interface Pseudo

inline fun Pseudo(value: String): Pseudo = unsafeCast(value)

/** The variant as a `Selector`, attached to its parent rule by the leading `&`. */
fun Pseudo.asSelector(): Selector = Selector("$this")

/** One state of the enclosing rule. */
fun <T : Any> RuleBuilder<T>.pseudo(
    pseudo: Pseudo,
    block: T.() -> Unit,
) {
    pseudo.asSelector()(block)
}
/**
 * One state of the enclosing rule, spelled the way the wrappers' own pseudo helpers are:
 * `Pseudos.hover { … }`. The context parameter is the `RuleBuilder` in scope (`cssSheet`'s
 * `PropertiesBuilder`); the `&` is already part of the [Pseudo] value.
 *
 * Requires Kotlin 2.2+ context parameters (no compiler flag in 2.4). Import `…dom.css.invoke` at
 * call sites, as with any operator extension.
 */
context(builder: RuleBuilder<T>)
inline operator fun <T : Any> Pseudo.invoke(
    block: T.() -> Unit,
) {
    builder.apply {
        this@invoke.asSelector()(block)
    }
}

/** Several states sharing one block — `many` joins the selectors with commas. */
fun <T : Any> RuleBuilder<T>.pseudos(
    vararg pseudos: Pseudo,
    block: T.() -> Unit,
) {
    val selectors = Array(pseudos.size) { index -> pseudos[index].asSelector() }
    many(*selectors)(block)
}

/** Every simple pseudo from csstype's builder, each as a parent-attached variant. */
object Pseudos {
    inline val after: Pseudo get() = Pseudo("&::after")

    inline val backdrop: Pseudo get() = Pseudo("&::backdrop")

    inline val before: Pseudo get() = Pseudo("&::before")

    inline val cue: Pseudo get() = Pseudo("&::cue")

    inline val cueRegion: Pseudo get() = Pseudo("&::cue-region")

    inline val firstLetter: Pseudo get() = Pseudo("&::first-letter")

    inline val firstLine: Pseudo get() = Pseudo("&::first-line")

    inline val grammarError: Pseudo get() = Pseudo("&::grammar-error")

    inline val marker: Pseudo get() = Pseudo("&::marker")

    inline val placeholder: Pseudo get() = Pseudo("&::placeholder")

    inline val selection: Pseudo get() = Pseudo("&::selection")

    inline val spellingError: Pseudo get() = Pseudo("&::spelling-error")

    inline val targetText: Pseudo get() = Pseudo("&::target-text")

    inline val viewTransition: Pseudo get() = Pseudo("&::view-transition")

    inline val active: Pseudo get() = Pseudo("&:active")

    inline val anyLink: Pseudo get() = Pseudo("&:any-link")

    inline val blank: Pseudo get() = Pseudo("&:blank")

    inline val checked: Pseudo get() = Pseudo("&:checked")

    inline val current: Pseudo get() = Pseudo("&:current")

    inline val default: Pseudo get() = Pseudo("&:default")

    inline val defined: Pseudo get() = Pseudo("&:defined")

    inline val disabled: Pseudo get() = Pseudo("&:disabled")

    inline val empty: Pseudo get() = Pseudo("&:empty")

    inline val enabled: Pseudo get() = Pseudo("&:enabled")

    inline val first: Pseudo get() = Pseudo("&:first")

    inline val firstChild: Pseudo get() = Pseudo("&:first-child")

    inline val firstOfType: Pseudo get() = Pseudo("&:first-of-type")

    inline val focus: Pseudo get() = Pseudo("&:focus")

    inline val focusVisible: Pseudo get() = Pseudo("&:focus-visible")

    inline val focusWithin: Pseudo get() = Pseudo("&:focus-within")

    inline val fullscreen: Pseudo get() = Pseudo("&:fullscreen")

    inline val future: Pseudo get() = Pseudo("&:future")

    inline val hover: Pseudo get() = Pseudo("&:hover")

    inline val inRange: Pseudo get() = Pseudo("&:in-range")

    inline val indeterminate: Pseudo get() = Pseudo("&:indeterminate")

    inline val invalid: Pseudo get() = Pseudo("&:invalid")

    inline val lastChild: Pseudo get() = Pseudo("&:last-child")

    inline val lastOfType: Pseudo get() = Pseudo("&:last-of-type")

    inline val left: Pseudo get() = Pseudo("&:left")

    inline val link: Pseudo get() = Pseudo("&:link")

    inline val localLink: Pseudo get() = Pseudo("&:local-link")

    inline val nthCol: Pseudo get() = Pseudo("&:nth-col")

    inline val nthLastCol: Pseudo get() = Pseudo("&:nth-last-col")

    inline val onlyChild: Pseudo get() = Pseudo("&:only-child")

    inline val onlyOfType: Pseudo get() = Pseudo("&:only-of-type")

    inline val optional: Pseudo get() = Pseudo("&:optional")

    inline val outOfRange: Pseudo get() = Pseudo("&:out-of-range")

    inline val past: Pseudo get() = Pseudo("&:past")

    inline val paused: Pseudo get() = Pseudo("&:paused")

    inline val pictureInPicture: Pseudo get() = Pseudo("&:picture-in-picture")

    inline val placeholderShown: Pseudo get() = Pseudo("&:placeholder-shown")

    inline val playing: Pseudo get() = Pseudo("&:playing")

    inline val readOnly: Pseudo get() = Pseudo("&:read-only")

    inline val readWrite: Pseudo get() = Pseudo("&:read-write")

    inline val required: Pseudo get() = Pseudo("&:required")

    inline val right: Pseudo get() = Pseudo("&:right")

    inline val root: Pseudo get() = Pseudo("&:root")

    inline val scope: Pseudo get() = Pseudo("&:scope")

    inline val target: Pseudo get() = Pseudo("&:target")

    inline val targetWithin: Pseudo get() = Pseudo("&:target-within")

    inline val userInvalid: Pseudo get() = Pseudo("&:user-invalid")

    inline val userValid: Pseudo get() = Pseudo("&:user-valid")

    inline val valid: Pseudo get() = Pseudo("&:valid")

    inline val visited: Pseudo get() = Pseudo("&:visited")
}
