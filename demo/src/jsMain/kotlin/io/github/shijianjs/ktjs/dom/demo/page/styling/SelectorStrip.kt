package io.github.shijianjs.ktjs.dom.demo.page.styling

import io.github.shijianjs.ktjs.demo.style.Palette
import io.github.shijianjs.ktjs.demo.style.RESET
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.css.dsl.CssSelector
import io.github.shijianjs.ktjs.dom.css.dsl.attributeSelector
import io.github.shijianjs.ktjs.dom.css.dsl.child
import io.github.shijianjs.ktjs.dom.css.dsl.className
import io.github.shijianjs.ktjs.dom.css.dsl.compound
import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import web.cssom.Border
import web.cssom.ClassName
import web.cssom.CSSStyleSheet
import web.cssom.Display
import web.cssom.LineStyle
import web.cssom.Padding
import web.cssom.integer
import web.cssom.px
import web.html.HTMLSpanElement
import web.html.HtmlTagName

/**
 * `<demo-selector-strip>`: three chips of one class, the middle one also carrying `is-selected` and
 * `aria-current`.
 *
 * The point is the sheet above: the middle chip is painted by a compound selector, and the row's
 * attribute rule finishes the job — both keys built from parts, both living only in this component's
 * own tree.
 */
class SelectorStrip : CustomElementAdapter() {
    private val chips: List<HTMLSpanElement> = List(3) { index ->
        HtmlTagName.span {
            classList.add(CHIP)
            textContent = "chip $index"
        }
    }

    private val row = HtmlTagName.div { classList.add(ROW) }

    init {
        chips[1].classList.add(SELECTED)
        chips[1].setAttribute("aria-current", "true")
        row.append(*chips.toTypedArray())
        attachStyledShadow(RESET, STRIP_SHEET).append(row)
    }

    /** Sets the three labels; the middle one is the selected chip. */
    fun setLabels(plain: String, selected: String) {
        chips[0].textContent = plain
        chips[2].textContent = plain
        chips[1].textContent = selected
    }

    companion object : SelectorStripDefaultReference() {
        private val CHIP: ClassName = ClassName("selector-chip")
        private val SELECTED: ClassName = ClassName("is-selected")
        private val ROW: ClassName = ClassName("selector-strip__row")
        /** `.selector-chip.is-selected` — one element carrying both classes. */
        private val SELECTED_CHIP: CssSelector = compound(className(CHIP), className(SELECTED))
        /** `.selector-strip__row > [aria-current]` — the selected chip, seen from the row. */
        private val CURRENT_CHIP_IN_ROW: CssSelector = className(ROW) child attributeSelector("aria-current")

        /**
         * The rule keys are values that were built, not pasted strings.
         *
         * Both come from `compound(…)` and the `child` combinator, so a typo in a class name is a compile
         * error instead of a rule that silently never matches.
         */
        private val STRIP_SHEET: CSSStyleSheet = cssSheet {
            ROW {
                display = Display.flex
            }

            CHIP {
                padding = Padding(4.px, 10.px)
                marginRight = 8.px
                border = Border(1.px, LineStyle.solid, Palette.border)
                borderRadius = 6.px
            }

            SELECTED_CHIP {
                borderColor = Palette.accent
                color = Palette.accent
                fontWeight = integer(600)
            }

            CURRENT_CHIP_IN_ROW {
                backgroundColor = Palette.surface
            }
        }
    }
}
