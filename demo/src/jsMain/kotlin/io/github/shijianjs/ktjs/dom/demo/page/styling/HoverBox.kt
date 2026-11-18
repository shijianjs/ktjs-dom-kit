package io.github.shijianjs.ktjs.dom.demo.page.styling

import io.github.shijianjs.ktjs.demo.Palette
import io.github.shijianjs.ktjs.demo.RESET
import io.github.shijianjs.ktjs.dom.webcomponent.attr.attr
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.css.dsl.Pseudos
import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import web.components.CustomElementReference
import web.cssom.Border
import web.cssom.ClassName
import web.cssom.CSSStyleSheet
import web.cssom.LineStyle
import web.cssom.Padding
import web.cssom.PropertyName
import web.cssom.Transition
import web.cssom.TransitionTimingFunction
import web.cssom.ms
import web.cssom.px
import web.dom.TagName
import web.html.HTMLSpanElement
import web.html.HtmlTagName

private val BOX: ClassName = ClassName("hover-box")

/**
 * The box's own sheet: its resting surface, the transition, and the `:hover` state that changes it.
 *
 * A pseudo-class cannot be written inline — an inline declaration outranks any selector, so a hover
 * rule in `style` never fires. Both the base value and the hover value therefore live here, in a
 * sheet only this component adopts.
 */
private val BOX_SHEET: CSSStyleSheet = cssSheet {
    BOX {
        padding = Padding(12.px, 14.px)
        border = Border(1.px, LineStyle.solid, Palette.border)
        borderRadius = 8.px
        backgroundColor = Palette.surface
        color = Palette.text
        transition = Transition(PropertyName.backgroundColor, 150.ms, TransitionTimingFunction.ease)
        Pseudos.hover { backgroundColor = Palette.surfaceRaised }
    }
}

/**
 * `<demo-hover-box label="…">`: a box whose `:hover` state is painted by its own sheet.
 *
 * Shows the one case where a class is genuinely needed: inline styles cannot express `:hover`, and a
 * per-instance `style` would outrank the rule. Everything else about this element is still a field
 * and a named setter.
 */
class HoverBox : CustomElementAdapter() {
    private val label: HTMLSpanElement = HtmlTagName.span.createElement { classList.add(BOX) }

    init {
        attachStyledShadow(RESET, BOX_SHEET).append(label)
    }

    override fun connectedCallback() {
        applyInitialAttributes()
    }

    /** The text inside the box; called `labelText` because `HTMLElement.title` is the tooltip. */
    var labelText: String by attr("label", default = "Hover me") { change ->
        label.textContent = change.newValue
    }

    companion object : CustomElementReference<HoverBox>(HoverBox::class.js) {
        val NAME: TagName<HoverBox> = TagName("demo-hover-box")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<demo-hover-box>`; callers set [HoverBox.labelText]. */
fun newHoverBox(): HoverBox {
    HoverBox.define()
    return HoverBox.NAME.createElement()
}
