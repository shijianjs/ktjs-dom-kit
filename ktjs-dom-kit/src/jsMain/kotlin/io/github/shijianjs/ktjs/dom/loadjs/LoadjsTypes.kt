package io.github.shijianjs.ktjs.dom.loadjs

import js.objects.unsafeJso
import web.html.HTMLElement

/** Upstream `type LoadedFn = () => void`, the plain callback form of `loadjs(…)` and [Loadjs.ready]. */
typealias LoadedFn = () -> Unit

/**
 * Upstream `LoadOptions`: what the loading call does while it works and when it finishes.
 *
 * It is a JavaScript object literal, so it is built with [unsafeJso] — a field that is left out stays
 * `undefined`, which is what upstream reads:
 *
 * ```kotlin
 * loadjs(
 *     StringOrStringArray(arrayOf("/a.js", "/b.js")),
 *     "assets",
 *     unsafeJso<LoadOptions> {
 *         success = { /* a.js and b.js loaded */ }
 *         error = { paths -> /* at least one of them did not */ }
 *         numRetries = 3.0
 *     },
 * )
 * ```
 */
external interface LoadOptions {

    /**
     * Called for every file right before its element is inserted, with the path as written and the
     * `<script>` / `<link>` / `<img>` that was created for it — the hook for setting attributes such
     * as `crossOrigin`.
     *
     * Returning `false` skips upstream's insertion into `<head>` (the README's "bypass the default
     * DOM insertion mechanism"); anything else, `true` included, inserts it. Upstream's `.d.ts`
     * declares this as a `void` callback and only the implementation and the README carry the `false`
     * contract, which is why the Kotlin type returns `Boolean?`.
     */
    var before: ((path: String, scriptEl: HTMLElement) -> Boolean?)?

    /** `async`: load the scripts asynchronously; upstream defaults it to `true`. Ignored for CSS and images. */
    var async: Boolean?

    /**
     * `numRetries`: how many extra attempts a file gets after a failed load before it counts as not
     * found; upstream defaults it to `0`. Each attempt calls [before] again.
     */
    var numRetries: Double?

    /** Called once every file in the bundle has loaded (and not one of them failed). */
    var success: (() -> Unit)?

    /** Called instead of [success] with the paths that could not be loaded. */
    var error: ((pathsNotFound: Array<String>) -> Unit)?

    /**
     * Upstream spells the promise branch as a separate type (`LoadOptions & {returnPromise: true}`,
     * see [PromiseLoadOptions]); here it is a field, so one object can be handed to either branch.
     * Only the promise-returning overloads read it.
     */
    var returnPromise: Boolean?
}

/**
 * Upstream `LoadOptions & {returnPromise: true}`: the options of the overloads that return a
 * [kotlin.js.Promise].
 *
 * Kotlin cannot require a literal `true` in a field's type, so the field is not redeclared here —
 * build one with [promiseOptions], which sets it. Passing a plain [LoadOptions] picks the callback
 * overload, passing this type picks the promise one.
 */
external interface PromiseLoadOptions : LoadOptions

/**
 * Builds [PromiseLoadOptions] with `returnPromise` already set, because the promise overloads are only
 * correct when it is on: upstream returns `undefined` instead of a promise otherwise.
 *
 * ```kotlin
 * loadjs(StringOrStringArray("/a.js"), promiseOptions { numRetries = 1.0 })
 *     ?.then { /* loaded */ }
 * ```
 */
inline fun promiseOptions(configure: LoadOptions.() -> Unit = {}): PromiseLoadOptions =
    unsafeJso<PromiseLoadOptions> {
        returnPromise = true
        configure()
    }

/**
 * Upstream `ReadyOptions`: the object [Loadjs.ready] takes instead of a plain callback, so the bundles
 * that never arrived are named.
 */
external interface ReadyOptions {

    /** Called once every bundle in the dependency list has loaded. */
    var success: (() -> Unit)?

    /** Called instead of [success] with the ids of the bundles that did not load. */
    var error: ((depsNotFound: Array<String>) -> Unit)?
}
