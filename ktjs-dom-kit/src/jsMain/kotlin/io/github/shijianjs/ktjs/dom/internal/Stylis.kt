@file:JsModule("stylis")

package io.github.shijianjs.ktjs.dom.internal

import js.array.ReadonlyArray
import kotlin.js.JsAny

/**
 * The half of stylis [io.github.shijianjs.ktjs.dom.css.cssSheet] calls, declared as-is.
 *
 * stylis 4 is a parser plus a middleware chain, not the `compile(selector)` of stylis 3: `compile`
 * turns CSS text into an element tree, `serialize` walks it, and `middleware([stringify])` prints the
 * result — resolving nested selectors against their parent on the way (`:hover{…}` under
 * `.card{{…}}` becomes `.card:hover{…}`). `@emotion/cache` calls exactly this.
 *
 * `prefixer` is deliberately **not** in the chain: it adds `-webkit-` / `-ms-` fallbacks, and emotion
 * 11 removed that behaviour, so a sheet built here matches what emotion would emit.
 */
internal external fun compile(css: String): ReadonlyArray<JsAny?>

internal external val stringify: StylisMiddleware

internal external fun middleware(collection: ReadonlyArray<StylisMiddleware>): JsAny?

internal external fun serialize(
    children: JsAny?,
    callback: JsAny?,
): String

internal typealias StylisMiddleware = (
    element: JsAny?,
    index: Int,
    children: JsAny?,
    callback: JsAny?,
) -> String
