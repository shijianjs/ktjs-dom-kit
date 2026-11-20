package io.github.shijianjs.ktjs.dom.demo.page.attribute

import io.github.shijianjs.ktjs.dom.attribute.Attribute
import io.github.shijianjs.ktjs.dom.attribute.attribute
import io.github.shijianjs.ktjs.dom.attribute.literalAttribute
import web.html.HTMLElement
import js.reflect.unsafeCast

/**
 * A wrapper's vocabulary: upstream literals are `sealed external interface` values, not strings.
 *
 * The id is the same everywhere it is used, so Kotlin cannot give an external interface a companion
 * and the factory and the getter that reads the literal back sit next to it instead.
 */
internal sealed external interface Density

internal fun Density(token: String): Density = token.unsafeCast<Density>()

internal val Density.token: String get() = unsafeCast<String>()

/** `aria-label` is optional on every wrapper element: absent must stay distinguishable from `""`. */
internal val HTMLElement.ariaLabelAttr: Attribute<String> get() = attribute("aria-label")

/** The typed form of a literal attribute: `literalAttribute` rejects anything outside `Densities.all`. */
internal val HTMLElement.densityAttr: Attribute<Density>
    get() = literalAttribute("data-density", Densities.all) { it.token }
