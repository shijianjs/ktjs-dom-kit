package io.github.shijianjs.ktjs.dom.attribute

import js.array.jsArrayOf
import js.objects.unsafeJso
import web.html.HTMLElement
import web.mutation.MutationObserver
import web.mutation.MutationObserverInit

/**
 * A live attribute subscription, from [Attribute.observe] or [observeAttribute].
 *
 * It wraps a `MutationObserver`, which the browser keeps calling until it is told to stop, so a
 * component that subscribes has to release it — `disconnect()` in `disconnectedCallback` is the
 * counterpart of `attr`'s automatic one-observer-per-element bookkeeping.
 */
fun interface AttributeWatch {

    /** Stops reporting; the callback is never called again. Safe to call twice. */
    fun disconnect()
}

/**
 * Calls [onChange] whenever [name] changes on this element, with the new attribute text (`null` when
 * it was removed).
 *
 * The raw form of [Attribute.observe]: no codec, no decoding, just the text. Use it for an attribute
 * whose meaning the caller interprets itself, and remember to [AttributeWatch.disconnect].
 *
 * ```kotlin
 * val watch = element.observeAttribute("theme") { raw -> repaint(raw ?: "") }
 * // … later
 * watch.disconnect()
 * ```
 */
fun HTMLElement.observeAttribute(name: String, onChange: (String?) -> Unit): AttributeWatch =
    observeRawAttribute(this, name) { raw, _ -> onChange(raw) }

/**
 * The shared implementation: one `MutationObserver` per subscription, filtered to a single attribute.
 *
 * `attributeFilter` keeps unrelated attribute writes out of the callback, so a chatty component does
 * not wake this subscriber for every class change. The observer is never notified twice for the same
 * text: equal values are dropped here rather than by every caller.
 */
internal fun observeRawAttribute(
    element: HTMLElement,
    name: String,
    onChange: (raw: String?, oldRaw: String?) -> Unit,
): AttributeWatch {
    var last: String? = element.getAttribute(name)
    var reported = false

    val observer = MutationObserver { _, _ ->
        val current = element.getAttribute(name)
        if (reported && current == last) return@MutationObserver
        val previous = if (reported) last else current
        last = current
        reported = true
        onChange(current, previous)
    }
    observer.observe(
        element,
        unsafeJso<MutationObserverInit> {
            attributes = true
            attributeFilter = jsArrayOf(name)
        },
    )

    return AttributeWatch {
        observer.disconnect()
        // Records queued before disconnecting would otherwise run the callback once more.
        observer.takeRecords()
    }
}
