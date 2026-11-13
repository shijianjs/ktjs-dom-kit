package io.github.shijianjs.ktjs.dom.css.dsl

import io.github.shijianjs.ktjs.dom.css.SupportsQuery
import csstype.RuleBuilder
import web.cssom.AtRules

/**
 * `@supports <condition> { … }` — the one at-rule the wrappers do not wrap.
 *
 * `csstype.RuleBuilder` already has `@media(query) { … }` (plus `@container`, `@font-face`,
 * `@view-transition`), so those are used directly; `web.cssom.AtRules` types every at-rule keyword
 * (`media`, `supports`, `layer`, `page`, …) as an `AtRules` constant. cssom has no supports-condition
 * type, so `SupportsQuery` (in `dom.css`) mirrors `MediaQuery`: typed declarations plus
 * `and` / `or` / `not`.
 *
 * ```kotlin
 * cssSheet {
 *     `@media`(MediaType.screen and maxWidth(576.px)) {
 *         actionsClass { flexDirection = FlexDirection.column }
 *     }
 *     supports(
 *         supportsDeclaration(PropertyName.backgroundClip, BackgroundClip.text) or
 *             supportsDeclaration(webkitBackgroundClipName, BackgroundClip.text),
 *     ) {
 *         titleClass { backgroundClip = BackgroundClip.text }
 *     }
 * }
 * ```
 */
fun <T : Any> RuleBuilder<T>.supports(
    query: SupportsQuery,
    block: T.() -> Unit,
) {
    "${AtRules.supports} $query"(block)
}
