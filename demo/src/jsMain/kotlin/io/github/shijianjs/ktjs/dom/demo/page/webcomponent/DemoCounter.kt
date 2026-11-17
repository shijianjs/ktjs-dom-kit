package io.github.shijianjs.ktjs.dom.demo.page.webcomponent

import io.github.shijianjs.ktjs.demo.Palette
import io.github.shijianjs.ktjs.demo.RESET
import io.github.shijianjs.ktjs.dom.attribute.attr
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import web.components.CustomElementReference
import web.cssom.ClassName
import web.cssom.CSSStyleSheet
import web.cssom.Display
import web.cssom.Padding
import web.cssom.px
import web.dom.TagName
import web.dom.clickEvent
import web.events.addHandler
import web.html.HtmlTagName

private val COUNTER_SHEET: CSSStyleSheet = cssSheet {
    ".counter" {
        display = Display.flex
        padding = Padding(10.px, 12.px, 10.px, 12.px)
        backgroundColor = Palette.surface
    }

    ".counter__value" {
        marginRight = 12.px
        fontSize = 16.px
        color = Palette.text
    }
}

/**
 * `<demo-counter>`: a component that owns DOM, keeps its own state and reads one attribute.
 *
 * Structure is built in the constructor and never rebuilt; [setCount] touches only the label it is
 * responsible for. Two instances of the tag are two independent counters sharing one class-level
 * sheet.
 */
class DemoCounter : CustomElementAdapter() {
    private val valueLabel = HtmlTagName.span.createElement {
        classList.add(ClassName("counter__value"))
        textContent = "0"
    }

    private val stepButton = HtmlTagName.button.createElement {
        textContent = "+"
    }

    private var count = 0

    init {
        stepButton.clickEvent.addHandler { setCount(count + step) }

        val box = HtmlTagName.div.createElement {
            classList.add(ClassName("counter"))
        }
        box.append(valueLabel, stepButton)

        attachStyledShadow(RESET, COUNTER_SHEET).append(box)
    }

    override fun connectedCallback() {
        applyInitialAttributes()
    }

    /** How much one click adds; defaults to 1 and is read from the `step` attribute. */
    var step: Int by attr(default = 1)

    private fun setCount(next: Int) {
        count = next
        valueLabel.textContent = next.toString()
    }

    companion object : CustomElementReference<DemoCounter>(DemoCounter::class.js) {
        val NAME: TagName<DemoCounter> = TagName("demo-counter")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<demo-counter>`; [step] is written as the attribute when given. */
fun newCounter(step: Int? = null): DemoCounter {
    DemoCounter.define()
    return DemoCounter.NAME.createElement {
        if (step != null) this.step = step
    }
}
