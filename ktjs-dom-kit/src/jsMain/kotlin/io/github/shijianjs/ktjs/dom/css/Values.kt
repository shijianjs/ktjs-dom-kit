package io.github.shijianjs.ktjs.dom.css

import js.reflect.unsafeCast
import web.cssom.PointerEvents

/**
 * Values cssom's unions do not list.
 *
 * `PointerEvents` (`web/cssom/PointerEvents.kt`) is generated from the SVG keyword set — `all`,
 * `fill`, `painted`, `stroke`, `visible`, … — and `pointer-events: none` is missing from it, although
 * it is the value that "this decoration must not catch the pointer" needs. Same treatment as a
 * missing factory: a typed constant here, not a cast at the call site.
 */
val pointerEventsNone: PointerEvents = unsafeCast("none")
