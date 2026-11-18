package io.github.shijianjs.ktjs.dom.demo.wrapper

import io.github.shijianjs.ktjs.dom.attribute.TokenListAttribute
import io.github.shijianjs.ktjs.dom.attribute.tokenListAttribute
import io.github.shijianjs.ktjs.vaadin.themablemixin.ThemePropertyMixin
import kotlin.js.unsafeCast
import web.html.HTMLElement

/**
 * Wrapper elements and the attributes we read on them.
 *
 * A wrapper element is an `external` class, so the attribute name is the one string that cannot be
 * checked by the compiler. Keeping it here — once, in an extension property — means the pages call
 * `button.themeAttr.toggle("primary")` instead of repeating `tokenListAttribute("theme")` and a typo
 * stays in one line rather than in every call site.
 *
 * One extension per attribute, named `<attribute>Attr` and typed with the operation object that
 * matches its shape: a token list (`theme`), a `String` (`aria-label`), an `Int` … See
 * `ktjs-dom-kit`'s `Attribute` / `TokenListAttribute`.
 */

/**
 * The themable `theme` attribute — a space-separated variant list.
 *
 * Declared on [ThemePropertyMixin] rather than `HTMLElement` because only components mixing the
 * interface in have the attribute at all: a plain element has no `theme`.
 * `ThemePropertyMixin` is a marker without members, so the element behind it is an
 * `unsafeCast<HTMLElement>()` — the same cast upstream's own `ThemeVariants` uses.
 */
val ThemePropertyMixin.themeAttr: TokenListAttribute
    get() = unsafeCast<HTMLElement>().tokenListAttribute("theme")
