package io.github.shijianjs.ktjs.dom.css.dsl

import csstype.RuleBuilder
import js.reflect.unsafeCast
import web.cssom.Selector
import web.cssom.many

/**
 * A pseudo-class or a pseudo-element, holding exactly what CSS prints: `:hover`, `::before`,
 * `:nth-child`.
 *
 * A [Pseudo] never carries a leading `&`. That combinator is about **where** the variant is attached,
 * not about what the variant is — the scope that writes it into a rule adds it ([Pseudo.invoke],
 * `RuleBuilder.pseudo`, `RuleBuilder.pseudos`), and a variant used as a selector part
 * (`pseudo(Pseudos.not, …)`) has no use for it at all.
 *
 * The kind is decided when the value is created, with one factory per kind, because CSS fixes it for
 * every name — `:hover` can never be an element and `::before` can never be a class:
 *
 * ```kotlin
 * val hover = pseudoClass("hover")        // :hover
 * val before = pseudoElement("before")    // ::before
 * ```
 */
external interface Pseudo

/** `:name` — a pseudo-class. [name] is written without the colon. */
inline fun pseudoClass(name: String): Pseudo = unsafeCast(":$name")

/** `::name` — a pseudo-element. [name] is written without the colons. */
inline fun pseudoElement(name: String): Pseudo = unsafeCast("::$name")

/**
 * `&` + the variant: the selector that attaches it to the rule it is written in, and only there.
 *
 * Every comma-separated branch gets its own `&` (`&:hover,&:focus-visible`), because stylis rewrites
 * each branch separately — a branch left without `&` becomes a *descendant* of the parent rule
 * (`.card :focus-visible`) instead of the same element in another state.
 */
private fun Pseudo.attachedToParent(): Selector = Selector(
    "$this".branches().joinToString(",") { branch -> "&$branch" },
)

/** Splits on commas that are not inside parentheses — `:is(.a,.b)` stays one branch. */
private fun String.branches(): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    var start = 0
    for (index in indices) {
        when (this[index]) {
            '(' -> depth++
            ')' -> depth--
            ',' -> if (depth == 0) {
                result += substring(start, index).trim()
                start = index + 1
            }
        }
    }
    result += substring(start).trim()
    return result
}

/** One state of the enclosing rule: `pseudo(Pseudos.hover) { … }` is `&:hover`. */
fun <T : Any> RuleBuilder<T>.pseudo(
    pseudo: Pseudo,
    block: T.() -> Unit,
) {
    pseudo.attachedToParent()(block)
}

/**
 * One state of the enclosing rule, spelled like the wrappers' own helpers: `Pseudos.hover { … }`.
 *
 * The context parameter is the `RuleBuilder` in scope (`cssSheet`'s `PropertiesBuilder`), and it is
 * the scope that prefixes `&`. Requires Kotlin 2.2+ context parameters (no compiler flag in 2.4);
 * import `…css.dsl.invoke` at call sites, as with any operator extension.
 */
context(builder: RuleBuilder<T>)
operator fun <T : Any> Pseudo.invoke(
    block: T.() -> Unit,
) {
    builder.apply {
        this@invoke.attachedToParent()(block)
    }
}

/** Several states sharing one block — each one attached to the parent rule, joined by commas. */
fun <T : Any> RuleBuilder<T>.pseudos(
    vararg pseudos: Pseudo,
    block: T.() -> Unit,
) {
    val selectors = Array(pseudos.size) { index -> pseudos[index].attachedToParent() }
    many(*selectors)(block)
}

/**
 * Every simple pseudo from `csstype.SimplePseudosRuleBuilder` (64 entries), plus the Web Components
 * ones that list omits (`:host`).
 *
 * Each constant is the plain pseudo text (`Pseudos.hover` is `:hover`); the `&` appears only when the
 * variant is attached to a rule. That is what makes one value usable in both roles:
 *
 * ```kotlin
 * cssSheet {
 *     cardClass {
 *         Pseudos.hover { borderColor = accent }                       // &:hover
 *         pseudos(Pseudos.hover, Pseudos.focusVisible) { … }            // &:hover,&:focus-visible
 *     }
 *     pseudo(Pseudos.not, className(HIDDEN)) { display = Display.none } // :not(.is-hidden)
 * }
 * ```
 */
object Pseudos {
    // === pseudo-elements ===

    val after: Pseudo = pseudoElement("after")

    val backdrop: Pseudo = pseudoElement("backdrop")

    val before: Pseudo = pseudoElement("before")

    val cue: Pseudo = pseudoElement("cue")

    val cueRegion: Pseudo = pseudoElement("cue-region")

    val firstLetter: Pseudo = pseudoElement("first-letter")

    val firstLine: Pseudo = pseudoElement("first-line")

    val grammarError: Pseudo = pseudoElement("grammar-error")

    val marker: Pseudo = pseudoElement("marker")

    val placeholder: Pseudo = pseudoElement("placeholder")

    val selection: Pseudo = pseudoElement("selection")

    val spellingError: Pseudo = pseudoElement("spelling-error")

    val targetText: Pseudo = pseudoElement("target-text")

    val viewTransition: Pseudo = pseudoElement("view-transition")

    // === pseudo-classes ===

    val active: Pseudo = pseudoClass("active")

    val anyLink: Pseudo = pseudoClass("any-link")

    val blank: Pseudo = pseudoClass("blank")

    val checked: Pseudo = pseudoClass("checked")

    val current: Pseudo = pseudoClass("current")

    val default: Pseudo = pseudoClass("default")

    val defined: Pseudo = pseudoClass("defined")

    val disabled: Pseudo = pseudoClass("disabled")

    val empty: Pseudo = pseudoClass("empty")

    val enabled: Pseudo = pseudoClass("enabled")

    val first: Pseudo = pseudoClass("first")

    val firstChild: Pseudo = pseudoClass("first-child")

    val firstOfType: Pseudo = pseudoClass("first-of-type")

    val focus: Pseudo = pseudoClass("focus")

    val focusVisible: Pseudo = pseudoClass("focus-visible")

    val focusWithin: Pseudo = pseudoClass("focus-within")

    val fullscreen: Pseudo = pseudoClass("fullscreen")

    val future: Pseudo = pseudoClass("future")

    /**
     * `:host` — the shadow host of the element whose sheet this is. Not part of csstype's list (it is
     * a Web Components pseudo), but without it no shadow-root rule can be written typed.
     *
     * A bare `:host` rule key is `pseudo(Pseudos.host)`; `Pseudos.host { … }` would attach `&` and
     * produce `&:host`, which never matches.
     */
    val host: Pseudo = pseudoClass("host")

    val hover: Pseudo = pseudoClass("hover")

    val inRange: Pseudo = pseudoClass("in-range")

    val indeterminate: Pseudo = pseudoClass("indeterminate")

    val invalid: Pseudo = pseudoClass("invalid")

    val lastChild: Pseudo = pseudoClass("last-child")

    val lastOfType: Pseudo = pseudoClass("last-of-type")

    val left: Pseudo = pseudoClass("left")

    val link: Pseudo = pseudoClass("link")

    val localLink: Pseudo = pseudoClass("local-link")

    val nthCol: Pseudo = pseudoClass("nth-col")

    val nthLastCol: Pseudo = pseudoClass("nth-last-col")

    val onlyChild: Pseudo = pseudoClass("only-child")

    val onlyOfType: Pseudo = pseudoClass("only-of-type")

    val optional: Pseudo = pseudoClass("optional")

    val outOfRange: Pseudo = pseudoClass("out-of-range")

    val past: Pseudo = pseudoClass("past")

    val paused: Pseudo = pseudoClass("paused")

    val pictureInPicture: Pseudo = pseudoClass("picture-in-picture")

    val placeholderShown: Pseudo = pseudoClass("placeholder-shown")

    val playing: Pseudo = pseudoClass("playing")

    val readOnly: Pseudo = pseudoClass("read-only")

    val readWrite: Pseudo = pseudoClass("read-write")

    val required: Pseudo = pseudoClass("required")

    val right: Pseudo = pseudoClass("right")

    val root: Pseudo = pseudoClass("root")

    val scope: Pseudo = pseudoClass("scope")

    val target: Pseudo = pseudoClass("target")

    val targetWithin: Pseudo = pseudoClass("target-within")

    val userInvalid: Pseudo = pseudoClass("user-invalid")

    val userValid: Pseudo = pseudoClass("user-valid")

    val valid: Pseudo = pseudoClass("valid")

    val visited: Pseudo = pseudoClass("visited")
}
