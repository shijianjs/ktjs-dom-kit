package io.github.shijianjs.ktjs.dom.webcomponent.attr

/**
 * The attribute name a `by attr` property stands for, when it is not the property name in kebab case.
 *
 * ```kotlin
 * @AttributeName("title")
 * var titleText: String by attr(KtjsHeroAttributes.TITLE_TEXT)
 * ```
 *
 * The annotation is read by `ktjs-dom-kit-ksp`, which generates the class's attribute constants and
 * the `observedAttributes` array from it — so this literal, the generated constant and the browser's
 * attribute are the same string, written once. It is `SOURCE`-retained: nothing reads it at runtime,
 * and the name the delegate uses comes from the generated constant passed to [attr].
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
public annotation class AttributeName(val value: String)
