package io.github.shijianjs.ktjs.dom.webcomponent

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec

/**
 * What an element's `by attr` declarations add up to: which attribute each property stands for, and
 * which codec reads and writes it when the built-ins cannot.
 *
 * `ktjs-dom-kit-ksp` writes one of these into the element's generated `…DefaultReference`, as the
 * `attributes` argument the reference registers at `customElements.define` time — before any instance,
 * which is when the delegates ask for both answers. The class exists so the two tables travel together:
 *
 * ```kotlin
 * companion object {
 *     const val TITLE_TEXT: String = "title"
 *     const val KIND: String = "kind"
 *     val KIND_CODEC: AttributeCodec<ActionKind> = enumAttributeCodec(ActionKind.entries)
 *     val ATTRIBUTES: CustomElementAttributes = CustomElementAttributes(
 *         names = mapOf("titleText" to TITLE_TEXT, "kind" to KIND),
 *         codecs = mapOf("kind" to KIND_CODEC),
 *     )
 * }
 * ```
 *
 * A reference written by hand passes `null` and keeps the previous behaviour: the property name in
 * kebab case, and the built-in codec for the property's type.
 *
 * Why the tables are needed at all: a delegated property cannot see its own class at runtime, only the
 * property name it is declared under, and Kotlin/JS erases generics — so neither the attribute name of
 * `titleText` nor the enum behind `kind` can be recovered from a delegate, while the compiler knows both
 * before anything runs.
 */
class CustomElementAttributes(
    /** Kotlin property name → attribute name; a property missing here keeps the kebab-case spelling. */
    val names: Map<String, String>,
    /** Kotlin property name → codec, for a `by attr` whose type has no built-in codec (an enum). */
    val codecs: Map<String, AttributeCodec<*>> = emptyMap(),
)
