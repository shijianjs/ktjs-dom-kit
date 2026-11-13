@file:JsModule("@emotion/serialize")

package io.github.shijianjs.ktjs.dom.internal

import js.array.ReadonlyArray
import kotlin.js.JsAny

/**
 * The corner of `@emotion/serialize` [io.github.shijianjs.ktjs.dom.css.cssSheet] needs, declared as-is —
 * a rules object in, declaration text out. The package exports more; nothing else is called.
 *
 * `serializeStyles` takes the **argument list as an array**, the way emotion's `css` tagged template
 * passes its template strings: `serializeStyles([rules])`. Passing the object directly makes it read
 * `args[0]` of that object and serialize nothing.
 *
 * What it does not do: flat rules. A nested key is written out as `key{…}` verbatim; resolving those
 * against their parent is stylis' job (see `Stylis.kt`).
 */
internal external fun serializeStyles(args: ReadonlyArray<JsAny?>): SerializedStyles

internal external interface SerializedStyles {
    val name: String

    /** The serialized declarations plus nested `key{…}` blocks. */
    val styles: String

    val map: String?
}
