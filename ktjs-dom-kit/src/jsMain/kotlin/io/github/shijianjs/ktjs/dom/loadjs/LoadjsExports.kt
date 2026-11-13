@file:JsModule("loadjs")

package io.github.shijianjs.ktjs.dom.loadjs

import kotlin.js.JsName
import kotlin.js.Promise

/**
 * The `loadjs` module: upstream's loader, with [Loadjs.ready] / [Loadjs.done] / [Loadjs.reset] /
 * [Loadjs.isDefined] on it.
 *
 * `loadjs` is published as UMD (`module.exports = loadjs`), so it is reached through the module's
 * **default** export; a named import would come back `undefined`.
 *
 * That default export is both a function and the holder of the four members, so it is declared twice:
 * once as [loadjs], for the members, and once as [loadjsCall] / [loadjsPromiseCall], for the call. The
 * split is what keeps the call a plain JavaScript call — declaring a callable `operator fun invoke`
 * *on the interface* instead compiles into a lookup of an `invoke` property, which upstream has not
 * got. The calls themselves are the `operator fun [Loadjs].invoke` overloads in `LoadjsCall.kt`.
 *
 * ```kotlin
 * loadjs("/path/to/foo.js", "foobar")
 * loadjs.ready("foobar") { /* foo.js loaded */ }
 * ```
 */
@JsName("default")
external val loadjs: Loadjs

/**
 * The call itself: upstream's `loadjs(paths, arg1, arg2)`.
 *
 * `Any?` is enough for the arguments — the public overloads are what type them, and this declaration
 * only has to pass them through. The `definedExternally` defaults are what turn a shorter call into
 * `undefined` arguments, the way upstream reads them, rather than into `null`.
 *
 * The return type is the promise branch's; the callback overloads make the same call for its side
 * effect and ignore what comes back, which is `undefined` there. It stays **one** declaration for
 * every shape on purpose: a second one for the same `@JsName` makes the compiler derive an unmangled
 * `default` reference for the later declaration, which is a syntax error in a module.
 */
@JsName("default")
internal external fun loadjsCall(
    paths: Any?,
    arg1: Any? = definedExternally,
    arg2: Any? = definedExternally,
): Promise<Unit>
