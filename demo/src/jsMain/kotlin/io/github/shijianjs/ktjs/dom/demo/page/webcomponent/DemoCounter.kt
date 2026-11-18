package io.github.shijianjs.ktjs.dom.demo.page.webcomponent

import io.github.shijianjs.ktjs.demo.Palette
import io.github.shijianjs.ktjs.demo.RESET
import io.github.shijianjs.ktjs.dom.webcomponent.attr.attr
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.vaadin.button.createButton
import web.components.CustomElementReference
import web.cssom.AlignItems
import web.cssom.ClassName
import web.cssom.CSSStyleSheet
import web.cssom.Display
import web.cssom.FontWeight
import web.cssom.integer
import web.cssom.px
import web.dom.TagName
import web.dom.clickEvent
import web.events.addHandler
import web.html.HTMLSpanElement
import web.html.HtmlTagName

/** Class names this file's markup and sheet agree on; both live here, next to their rule. */
private val COUNTER: ClassName = ClassName("counter")

private val VALUE: ClassName = ClassName("counter__value")

private val INCREMENT: ClassName = ClassName("counter__increment")

/** The counter's own sheet: it is adopted once and shared by every instance. */
private val COUNTER_SHEET: CSSStyleSheet = cssSheet {
    COUNTER {
        display = Display.flex
        alignItems = AlignItems.center
        columnGap = 12.px
        color = Palette.text
    }

    VALUE {
        fontSize = 26.px
        fontWeight = FontWeight.bold
        minWidth = 32.px
    }

    INCREMENT { fontSize = 14.px }
}

/**
 * `<demo-counter value="0">`: one instance of a custom element.
 *
 * Everything the element owns lives in its shadow tree — the sheet is adopted once for all instances,
 * the value is an `attr`, and the click handler only touches the field it is responsible for. Two
 * instances of this tag on one page share the sheet, not their state.
 */
class DemoCounter : CustomElementAdapter() {
    private val valueElement: HTMLSpanElement = HtmlTagName.span.createElement { classList.add(VALUE) }

    private val incrementElement = createButton().apply {
        classList.add(INCREMENT)
        textContent = "+"
    }

    init {
        val root = HtmlTagName.div.createElement { classList.add(COUNTER) }
        root.append(valueElement, incrementElement)
        attachStyledShadow(RESET, COUNTER_SHEET).append(root)

        incrementElement.clickEvent.addHandler { value += 1 }
    }

    override fun connectedCallback() {
        applyInitialAttributes()
    }

    /** The current count, stored in the `value` attribute so it survives a reconnect. */
    var value: Int by attr("value", default = 0) { change ->
        valueElement.textContent = change.newValue.toString()
    }

    /** Text on the increment button; kept as an attribute so the page can translate it. */
    var incrementLabel: String by attr("increment-label", default = "+") { change ->
        incrementElement.textContent = change.newValue
    }

    companion object : CustomElementReference<DemoCounter>(DemoCounter::class.js) {
        val NAME: TagName<DemoCounter> = TagName("demo-counter")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<demo-counter>`; callers set [DemoCounter.value] and [DemoCounter.incrementLabel]. */
fun newCounter(): DemoCounter {
    DemoCounter.define()
    return DemoCounter.NAME.createElement()
}
