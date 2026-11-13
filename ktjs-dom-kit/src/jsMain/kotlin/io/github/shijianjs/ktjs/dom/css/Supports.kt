package io.github.shijianjs.ktjs.dom.css

import js.reflect.unsafeCast
import kotlin.js.JsAny
import web.cssom.PropertyName

/**
 * An `@supports` condition, modelled the way `web.cssom.MediaQuery` is: an opaque value built only by
 * typed factories, with `and` / `or` / `not` composition.
 *
 * cssom types media queries (`MediaQuery(value)`, `MediaType`, `maxWidth`, `MediaQuery.and/or/not`)
 * but has nothing for supports conditions — see `MediaQuery.kt` for the shape copied here. A
 * declaration condition takes a `PropertyName<T>` and a value of exactly that type, so a typo in
 * either is a compile error instead of a string that silently never matches.
 */
external interface SupportsQuery

inline fun SupportsQuery(value: String): SupportsQuery = unsafeCast(value)

infix fun SupportsQuery.and(
    other: SupportsQuery,
): SupportsQuery = SupportsQuery("$this and $other")

infix fun SupportsQuery.or(
    other: SupportsQuery,
): SupportsQuery = SupportsQuery("$this or $other")

fun not(
    query: SupportsQuery,
): SupportsQuery = SupportsQuery("not $query")

/** `(<property>:<value>)` — the parenthesised form CSS requires inside a supports condition. */
inline fun <T : JsAny> supportsDeclaration(
    property: PropertyName<T>,
    value: T,
): SupportsQuery = SupportsQuery("($property:$value)")
