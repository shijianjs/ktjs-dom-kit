package io.github.shijianjs.ktjs.dom.demo.page.attribute

import io.github.shijianjs.ktjs.demo.style.Palette
import io.github.shijianjs.ktjs.demo.style.RESET
import io.github.shijianjs.ktjs.dom.webcomponent.attr.attr
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementReferenceAdapter
import web.cssom.CSSStyleSheet
import web.cssom.ClassName
import web.cssom.Padding
import web.cssom.px
import web.html.HtmlTagName

private val BADGE_CLASS: ClassName = ClassName("step-badge")

private val BADGE_SHEET: CSSStyleSheet = cssSheet {
    BADGE_CLASS {
        padding = Padding(6.px, 10.px, 6.px, 10.px)
        backgroundColor = Palette.surfaceRaised
        borderRadius = 6.px
        color = Palette.text
        fontSize = 13.px
    }
}

/**
 * `<demo-step-badge>`: the smallest component that shows `attr`.
 *
 * The value lives in the attribute, not in a field: the getter decodes it, the setter encodes it, so
 * `setAttribute("step", "10")` from outside is visible immediately — that is what makes an attribute
 * a public API rather than a copy of one.
 */
class StepBadge : CustomElementAdapter() {
    private val label = HtmlTagName.span {
        classList.add(BADGE_CLASS)
        textContent = "step = 1"
    }

    init {
        attachStyledShadow(RESET, BADGE_SHEET).append(label)
    }

    override fun connectedCallback() {
        applyInitialAttributes()
    }

    /** The value the badge shows; defaults to 1 and is read from the `step` attribute. */
    var step: Int by attr(default = 1) { change ->
        label.textContent = "step = ${change.newValue}"
    }

    companion object : CustomElementReferenceAdapter<StepBadge>(StepBadge::class.js, "demo-step-badge")
}

/** Creates a defined `<demo-step-badge>`; callers configure it through [StepBadge.step]. */
