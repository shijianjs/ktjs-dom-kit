package io.github.shijianjs.ktjs.dom.css

import js.reflect.unsafeCast
import web.cssom.Color
import web.cssom.Gradient
import web.cssom.Length
import web.cssom.LinearColorStop

/**
 * `radial-gradient(…)`, written the way cssom writes its own gradient factories (`Gradient.kt`:
 * `unsafeCast("linear-gradient(${stops.joinToString(",")})")`) — cssom ships linear, repeating-linear,
 * conic and angular shapes, but not the radial ones.
 *
 * Everything that is a value stays typed: the stops are `LinearColorStop`, the position is a
 * `Length`, and only the shape keywords are enums here (cssom has no type for them). Add overloads
 * as needed instead of falling back to a string at the call site.
 */
fun radialGradient(
    vararg stops: LinearColorStop,
): Gradient =
    unsafeCast("radial-gradient(${stops.joinToString(",")})")

fun radialGradient(
    shape: RadialGradientShape,
    vararg stops: LinearColorStop,
): Gradient =
    unsafeCast("radial-gradient(${shape.keyword},${stops.joinToString(",")})")

fun radialGradient(
    shape: RadialGradientShape,
    size: RadialGradientSize,
    vararg stops: LinearColorStop,
): Gradient =
    unsafeCast("radial-gradient(${shape.keyword} ${size.keyword},${stops.joinToString(",")})")

fun radialGradient(
    shape: RadialGradientShape,
    atX: Length,
    atY: Length,
    vararg stops: LinearColorStop,
): Gradient =
    unsafeCast("radial-gradient(${shape.keyword} at $atX $atY,${stops.joinToString(",")})")

/**
 * A colour stop with no position — cssom's `stop(color, value)` always appends one, but a gradient
 * spells its first and last stop without them all the time.
 */
fun colorStop(color: Color): LinearColorStop = unsafeCast("$color")

/** The `<ending-shape>` keyword; cssom has no type for it. */
enum class RadialGradientShape(internal val keyword: String) {
    Circle("circle"),
    Ellipse("ellipse"),
}

/** The `<size>` keyword; cssom has no type for it. */
enum class RadialGradientSize(internal val keyword: String) {
    ClosestSide("closest-side"),
    ClosestCorner("closest-corner"),
    FarthestSide("farthest-side"),
    FarthestCorner("farthest-corner"),
}
