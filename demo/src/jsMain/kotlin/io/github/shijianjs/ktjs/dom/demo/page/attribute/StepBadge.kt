package io.github.shijianjs.ktjs.dom.demo.page.attribute

import io.github.shijianjs.ktjs.demo.RESET
import io.github.shijianjs.ktjs.dom.attribute.attr
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import web.components.CustomElementReference
import web.dom.TagName
import web.html.HtmlTagName

/**
 * `<demo-step-badge>`: the smallest component that shows `attr`.
 *
 * The value lives in the attribute, not in a field: the getter decodes it, the setter encodes it, so
 * `setAttribute("step", "10")` from outside is visible immediately — that is what makes an attribute
 * a public API rather than a copy of one.
 */
class StepBadge : CustomElementAdapter() {
    private val label = HtmlTagName.span.createElement { textContent = "step = 1" }

    init {
        attachStyledShadow(RESET).append(label)
    }

    override fun connectedCallback() {
        applyInitialAttributes()
    }

    var step: Int by attr(default = 1) { change ->
        label.textContent = "step = ${change.newValue}"
    }

    companion object : CustomElementReference<StepBadge>(StepBadge::class.js) {
        val NAME: TagName<StepBadge> = TagName("demo-step-badge")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<demo-step-badge>`; callers configure it through [StepBadge.step]. */
fun newStepBadge(): StepBadge {
    StepBadge.define()
    return StepBadge.NAME.createElement()
}
