package io.github.shijianjs.ktjs.dom.webcomponent.attr

/**
 * The attribute name a `by attr` property stands for, when the property name in kebab case is not the
 * name the element wants.
 *
 * ```kotlin
 * @AttributeName("title")
 * var titleText: String by attr()
 * ```
 *
 * Read by `ktjs-dom-kit-ksp`, which writes the name into the `names` half of the element's generated
 * `…DefaultReference.ATTRIBUTES` — the table its reference registers, so a `by attr` without an
 * argument finds the attribute it stands for. Without this annotation the name is the property name
 * in kebab case (`maxCount` → `max-count`). `SOURCE`-retained: everything the runtime needs is already
 * in the generated table.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
public annotation class AttributeName(val value: String)
