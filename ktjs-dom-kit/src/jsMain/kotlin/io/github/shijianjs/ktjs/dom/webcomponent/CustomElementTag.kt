package io.github.shijianjs.ktjs.dom.webcomponent

/**
 * The tag a custom element is defined under, when it is not the class name in kebab case behind the
 * configured prefix.
 *
 * ```kotlin
 * @CustomElementTag("ktjs-hero")
 * class KtjsHero : CustomElementAdapter() { … }
 * ```
 *
 * Read by `ktjs-dom-kit-ksp`, which writes the tag into the element's generated
 * `…DefaultReference` — the class the companion object extends — so the literal appears once, on the
 * class, instead of also in the companion's constructor. Without this annotation the tag is the
 * `ktjs.tagPrefix` option (default `ktjs-`) followed by the class name in kebab case
 * (`DemoCounter` → `demo-counter`, prefix `demo-` → `demo-counter`). `SOURCE`-retained: the generated
 * reference holds everything the runtime needs.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class CustomElementTag(val value: String)
