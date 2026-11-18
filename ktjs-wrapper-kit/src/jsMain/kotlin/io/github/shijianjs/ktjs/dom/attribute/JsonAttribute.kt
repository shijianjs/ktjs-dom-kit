package io.github.shijianjs.ktjs.dom.attribute

import js.reflect.unsafeCast
import web.html.HTMLElement

/**
 * One **structured** attribute of one element: the text is JSON, and the value is whatever that JSON
 * parses to.
 *
 * Wrapper components that take a list or a configuration object cannot spell it as a scalar: Vaadin's
 * LCD-style components (`items`, `selected-items`, a chart's `option`, an upload's `files`) all read
 * a JSON attribute. This is the typed door to that text — serialization is the browser's own `JSON`,
 * not a Kotlin serialization library, so a value that came out of a wrapper (`Array<MenuBarItem>`)
 * goes in and comes back out unchanged.
 *
 * ```kotlin
 * val MenuBarElement.itemsJson: JsonAttribute get() = jsonAttribute("items")
 *
 * menuBar.itemsJson.value = items                                  // JSON.stringify
 * val items = menuBar.itemsJson.decode<Array<MenuBarItem>>()       // JSON.parse + cast
 * menuBar.itemsJson.clear()                                        // attribute removed
 * ```
 *
 * [decode] is a cast, not a validator: JSON has no type to check against, so a mismatched call
 * returns the parsed structure under the wrong static type. Keep [decode] next to the component whose
 * attribute it is (as above), so the cast lives in one declared place rather than at call sites.
 *
 * Absent and malformed are both `null`, matching [Attribute.valueOrNull]: an attribute holding broken
 * JSON is treated as "no value", never as an exception.
 */
class JsonAttribute(
    /** The element whose attribute this object reads and writes. */
    val element: HTMLElement,
    /** Attribute name as it appears in markup and CSS selectors, e.g. `items`. */
    val name: String,
) {

    /** The attribute text exactly as the browser stores it, `null` when the attribute is absent. */
    val raw: String? get() = element.getAttribute(name)

    /** Whether the element carries the attribute at all. */
    val present: Boolean get() = element.hasAttribute(name)

    /** The parsed value; assigning stringifies it, and `null` removes the attribute. */
    var value: JsAny?
        get() = raw?.let(::parse)
        set(value) {
            if (value == null) {
                clear()
            } else {
                element.setAttribute(name, JSON.stringify(value))
            }
        }

    /** The parsed value as [T]; `null` when the attribute is absent or its text is not JSON. */
    inline fun <reified T : JsAny> decode(): T? = value?.unsafeCast<T>()

    /** Removes the attribute. */
    fun clear() {
        element.removeAttribute(name)
    }

    private fun parse(text: String): JsAny? = try {
        JSON.parse(text)
    } catch (_: Throwable) {
        null
    }
}

/** This element's JSON attribute [name]; see [JsonAttribute]. */
fun HTMLElement.jsonAttribute(name: String): JsonAttribute = JsonAttribute(this, name)

/**
 * The browser's `JSON`, as the two calls an attribute needs.
 *
 * `kotlin-browser` does not wrap it, and a local declaration here keeps the dependency at
 * `kotlin-stdlib` instead of pulling a serialization library for two functions.
 */
private external object JSON {
    fun stringify(value: JsAny?): String
    fun parse(text: String): JsAny
}
