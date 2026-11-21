package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import web.html.HTMLElement
import web.html.HtmlTagName

/**
 * An append-only log of what an example did, newest line last — **as an element**.
 *
 * The examples fire events faster than a reader can follow, so the box keeps the [capacity] newest
 * lines only: older lines are removed, not hidden. The scroll container is the log itself, so a long
 * page never grows a second scrollbar because of the log.
 *
 * Created through its own tag: `EventLog.NAME { show(6, "No events yet") }`.
 */
class EventLog : CustomElementAdapter() {
    /** The log is its own root node. */
    val root: HTMLElement get() = this

    private var capacity: Int = 6

    /** Drawn until the first line arrives, so an untouched log reads as "nothing yet". */
    private var idleLine: HTMLElement? = null

    private var built = false

    /** Sets the box up once: the class, the capacity and the optional placeholder line. */
    fun show(capacity: Int = 6, placeholder: String? = null) {
        if (built) return
        built = true
        this.capacity = capacity
        classList.add(EVENT_LOG)
        if (placeholder != null) {
            val line = HtmlTagName.div { classList.add(EVENT_LINE); textContent = placeholder }
            this.idleLine = line
            append(line)
        }
    }

    /**
     * Rewrites the line shown while no event has arrived.
     *
     * A log that is still untouched has to follow a language switch like every other node; once a line
     * arrived the placeholder is gone, and this is a no-op.
     */
    fun placeholder(text: String) {
        val line = idleLine ?: return
        line.textContent = text
    }

    /** Appends one line, dropping the oldest ones past the capacity. */
    fun add(line: String) {
        idleLine?.remove()
        idleLine = null
        append(HtmlTagName.div { classList.add(EVENT_LINE); textContent = line })
        while (childElementCount > capacity) {
            firstElementChild?.remove()
        }
        scrollTop = scrollHeight.toDouble()
    }

    /** Drops every line. */
    fun clear() {
        idleLine = null
        replaceChildren()
    }

    companion object : EventLogDefaultReference()
}
