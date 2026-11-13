package io.github.shijianjs.ktjs.dom.loadjs

/**
 * Upstream `loadjs`, as far as its *members* go: the bundle registry that hangs off the loader.
 *
 * The loader is also a function, and Kotlin cannot make one declaration be both a value with members
 * and a callable — so the calls are the `operator fun Loadjs.invoke` extensions next to [loadjs], and
 * this interface only holds what is left:
 *
 * ```kotlin
 * loadjs.ready("foobar") { /* foobar's files have loaded */ }
 * loadjs.isDefined("foobar")
 * loadjs.done("foobar")
 * loadjs.reset()
 * ```
 *
 * A path is a URL; three prefixes override what upstream would infer from the extension:
 *
 * | Path | Element |
 * | --- | --- |
 * | `foo.js`, and anything else | `<script>` |
 * | `foo.css`, `css!foo.custom` | `<link rel="stylesheet">` |
 * | `foo.png` / `.jpg` / `.gif` / `.svg` / `.webp`, `img!foo.custom` | `<img>` |
 * | `module!foo.js` | `<script type="module">`; skipped in a browser without module support |
 * | `nomodule!foo.js` | plain `<script>`; skipped in a browser that *has* module support |
 *
 * Loading is DOM work (`document.createElement`, `<head>`), so the module is for the browser: pulling
 * it into Node — Docusaurus's prerender pass, for instance — fails at upstream's `document`.
 */
external interface Loadjs {

    /**
     * `loadjs.ready(bundleId, loaded)`: run the callback once that bundle has loaded.
     *
     * A bundle is satisfied when its files have loaded, or when [done] says so by hand. A bundle that
     * already finished reports immediately, so this can be called after `loadjs(…)` — and chaining
     * `.ready(…).ready(…)` works because the call returns the module again.
     */
    fun ready(bundleId: String, loaded: LoadedFn): Loadjs

    /** `loadjs.ready(bundleIds, loaded)` for several bundles. */
    fun ready(bundleIds: Array<String>, loaded: LoadedFn): Loadjs

    /** `loadjs.ready(bundleId, options)`: the callback form that names the bundles that failed. */
    fun ready(bundleId: String, options: ReadyOptions): Loadjs

    /** `loadjs.ready(bundleIds, options)` for several bundles. */
    fun ready(bundleIds: Array<String>, options: ReadyOptions): Loadjs

    /** `loadjs.isDefined(bundleId)`: whether that id has been handed to `loadjs` since [reset]. */
    fun isDefined(bundleId: String): Boolean

    /** `loadjs.done(bundleId)`: satisfy that bundle's dependencies by hand. */
    fun done(bundleId: String)

    /** `loadjs.reset()`: forget every bundle id, result and queued callback. */
    fun reset()
}
