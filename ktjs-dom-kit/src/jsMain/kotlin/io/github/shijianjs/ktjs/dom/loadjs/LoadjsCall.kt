package io.github.shijianjs.ktjs.dom.loadjs

import kotlin.js.Promise

/**
 * Calling the module object: `loadjs(files, …)`.
 *
 * Upstream's four call overloads spell their parameters as unions — `string | string[]` for the paths
 * and `string | LoadOptions | LoadedFn` for the second argument. Both are written here as **one
 * overload per branch**, so every branch is checked at the call site and a call reads like the
 * JavaScript one:
 *
 * ```kotlin
 * loadjs("/path/to/foo.js") { /* loaded */ }
 * loadjs(arrayOf("/path/to/a.css", "/path/to/b.png")) { /* loaded */ }
 * loadjs("/path/to/foo.js", "foobar", unsafeJso<LoadOptions> { success = { /* loaded */ } })
 * ```
 *
 * These are extensions rather than members of [Loadjs] because a JavaScript function object is one
 * value that is both callable and carries members, and Kotlin has no single declaration for that: the
 * members stay on the interface, the call becomes an `invoke` operator, and a caller importing the
 * value's package gets both.
 */

// ---- loadjs(files, …) ----

/** `loadjs(files, loaded)`. */
operator fun Loadjs.invoke(files: String, loaded: LoadedFn) {
    loadjsCall(files, loaded)
}

/** `loadjs(files, loaded)` with a list of paths. */
operator fun Loadjs.invoke(files: Array<String>, loaded: LoadedFn) {
    loadjsCall(files, loaded)
}

/** `loadjs(files, bundleId)`: register the bundle without a callback to run yet. */
operator fun Loadjs.invoke(files: String, bundleId: String) {
    loadjsCall(files, bundleId)
}

/** `loadjs(files, bundleId)` with a list of paths. */
operator fun Loadjs.invoke(files: Array<String>, bundleId: String) {
    loadjsCall(files, bundleId)
}

/** `loadjs(files, options)`. */
operator fun Loadjs.invoke(files: String, options: LoadOptions) {
    loadjsCall(files, options)
}

/** `loadjs(files, options)` with a list of paths. */
operator fun Loadjs.invoke(files: Array<String>, options: LoadOptions) {
    loadjsCall(files, options)
}

/** `loadjs(files, bundleId, loaded)`. */
operator fun Loadjs.invoke(files: String, bundleId: String, loaded: LoadedFn) {
    loadjsCall(files, bundleId, loaded)
}

/** `loadjs(files, bundleId, loaded)` with a list of paths. */
operator fun Loadjs.invoke(files: Array<String>, bundleId: String, loaded: LoadedFn) {
    loadjsCall(files, bundleId, loaded)
}

/** `loadjs(files, bundleId, options)`. */
operator fun Loadjs.invoke(files: String, bundleId: String, options: LoadOptions) {
    loadjsCall(files, bundleId, options)
}

/** `loadjs(files, bundleId, options)` with a list of paths. */
operator fun Loadjs.invoke(files: Array<String>, bundleId: String, options: LoadOptions) {
    loadjsCall(files, bundleId, options)
}

/**
 * Upstream `loadjs(files, options & {returnPromise: true})`: the promise rejects with the paths that
 * could not be loaded.
 *
 * The cast is what the promise branch of the call returns, and it is here rather than at the call
 * site: [promiseOptions] is the only way to build these options, and it is what makes upstream return
 * a promise instead of `undefined`.
 */
operator fun Loadjs.invoke(files: String, options: PromiseLoadOptions): Promise<Unit> =
    loadjsCall(files, options)

/** The promise overload with a list of paths. */
operator fun Loadjs.invoke(files: Array<String>, options: PromiseLoadOptions): Promise<Unit> =
    loadjsCall(files, options)

/** Upstream `loadjs(files, bundleId, options & {returnPromise: true})`. */
operator fun Loadjs.invoke(
    files: String,
    bundleId: String,
    options: PromiseLoadOptions,
): Promise<Unit> = loadjsCall(files, bundleId, options)

/** The promise overload with a list of paths. */
operator fun Loadjs.invoke(
    files: Array<String>,
    bundleId: String,
    options: PromiseLoadOptions,
): Promise<Unit> = loadjsCall(files, bundleId, options)
