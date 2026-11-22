package io.github.shijianjs.ktjs.dom.loadjs

/**
 * The load failed: the browser could not fetch [path], after the retries that were asked for.
 *
 * It comes out of `Loadjs(…)` and, for a bundle that failed, out of `LoadFile.ready()` too.
 */
class LoadFailedException(val path: String) : Exception("could not load: $path")

/**
 * A multi-path load did not get every file: [paths] are the ones that never arrived, in the order
 * they were asked for.
 *
 * The files that did arrive stay loaded — the list form reports what is missing instead of undoing
 * what worked, so a page can retry just those.
 */
class LoadsFailedException(val paths: List<String>) : Exception("could not load: ${paths.joinToString(", ")}")

/**
 * Handing a bundle id to the loader twice is an error upstream too — there it is `throw "LoadJS"`,
 * here a typed exception, so the failure is visible in Kotlin.
 */
class BundleAlreadyDefinedException(val bundleId: String) :
    IllegalStateException("bundle \"$bundleId\" was already handed to loadjs; call Loadjs.reset() to forget it")
