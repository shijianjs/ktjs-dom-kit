package io.github.shijianjs.ktjs.dom.demo.page.styling

import io.github.shijianjs.ktjs.demo.Palette
import io.github.shijianjs.ktjs.demo.RESET
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
import web.cssom.PropertyName
import web.cssom.ms
import web.cssom.Transition
import web.cssom.TransitionTimingFunction
import web.cssom.px
import web.dom.TagName
import web.html.HtmlTagName

/**
 * The component's own rules: the hover state and its transition.
 *
 * `:hover` cannot be expressed inline — an inline `background-color` would win over the pseudo-class
 * — so this one lives in the component's sheet, adopted by every instance of the tag.
 */
private val HOVER_SHEET: CSSStyleSheet = cssSheet {
    ".hover-box" {
        display = Display.block
        padding = Padding(14.px, 16.px, 14.px, 16.px)
        backgroundColor = Palette.surface
        transition = Transition(
            PropertyName.backgroundColor,
            150.ms,
            TransitionTimingFunction.ease,
        )
    }

    ".hover-box:hover" {
        backgroundColor = Palette.surfaceRaised
    }
}

/**
 * `<demo-hover-box>`: one box whose colour changes on hover.
 *
 * The point of the example is where the rule lives: in a sheet adopted by this component's shadow
 * root, not in a document-level `<style>` and not inline on the node.
 */
class HoverBox : CustomElementAdapter() {
    private val box = HtmlTagName.div.createElement {
        classList.add(ClassName("hover-box"))
    }

    init {
        attachStyledShadow(RESET, HOVER_SHEET).append(box)
    }

    var text: String
        get() = box.textContent.orEmpty()
        set(value) {
            box.textContent = value
        }

    companion object : CustomElementReference<HoverBox>(HoverBox::class.js) {
        val NAME: TagName<HoverBox> = TagName("demo-hover-box")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<demo-hover-box>`. */
fun newHoverBox(text: String): HoverBox {
    HoverBox.define()
    return HoverBox.NAME.createElement { this.text = text }
}
