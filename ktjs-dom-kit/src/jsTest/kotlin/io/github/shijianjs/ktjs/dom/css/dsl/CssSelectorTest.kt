package io.github.shijianjs.ktjs.dom.css.dsl

import io.github.shijianjs.ktjs.dom.css.cssText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import web.cssom.ClassName
import web.cssom.Display
import web.cssom.Margin
import web.cssom.px
import web.dom.TagName
import web.html.HtmlTagName

/**
 * A selector is a value: every test here compares the text it renders, because the text is exactly
 * what stylis receives. The last two tests go through `cssSheet`, which is how a selector is actually
 * used — a key, not a string that happens to look like one.
 */
class CssSelectorTest {
    private val ROW: ClassName = ClassName("row")
    private val SELECTED: ClassName = ClassName("is-selected")
    private val LABEL: ClassName = ClassName("label")

    private val BUTTON: TagName<web.html.HTMLButtonElement> = TagName("vaadin-button")

    @Test
    fun simpleSelectorsRenderTheirToken() {
        assertEquals("vaadin-button", "${tag(BUTTON)}")
        assertEquals("vaadin-button", "${tag("vaadin-button")}")
        assertEquals("div", "${tag(HtmlTagName.div)}")
        assertEquals(".row", "${className(ROW)}")
        assertEquals(".row", "${className("row")}")
        assertEquals("#main", "${id("main")}")
        assertEquals("*", "${Selectors.universal}")
        assertEquals("&", "${Selectors.nesting}")
        // `:host`, `:root` and `:scope` are pseudo-classes, not bare tokens: they live in `Pseudos`.
        assertEquals(":host", "${Pseudos.host}")
        assertEquals(":root", "${Pseudos.root}")
        assertEquals(":scope", "${Pseudos.scope}")
    }

    @Test
    fun attributesCarryOperatorQuoteAndCaseFlag() {
        assertEquals("[part]", "${attributeSelector("part")}")
        assertEquals("[part=\"label\"]", "${attributeSelector("part", "label")}")
        // Unquoted values are legal CSS, and some tools print them that way.
        assertEquals("[part=label]", "${attributeSelector("part", "label", quote = false)}")
        assertEquals("[part~=\"label\"]", "${attributeSelector("part", value = "label", operator = AttributeOperator.includes)}")
        assertEquals("[lang|=\"en\"]", "${attributeSelector("lang", value = "en", operator = AttributeOperator.dashMatch)}")
        assertEquals("[href^=\"https\"]", "${attributeSelector("href", value = "https", operator = AttributeOperator.prefix)}")
        assertEquals("[href\$=\".pdf\"]", "${attributeSelector("href", value = ".pdf", operator = AttributeOperator.suffix)}")
        assertEquals("[class*=\"wide\"]", "${attributeSelector("class", value = "wide", operator = AttributeOperator.substring)}")
        assertEquals("[data-route=\"grid\" i]", "${attributeSelector("data-route", value = "grid", caseFlag = AttributeCase.insensitive)}")
        assertEquals("[data-route=\"Grid\" s]", "${attributeSelector("data-route", value = "Grid", caseFlag = AttributeCase.sensitive)}")
    }

    @Test
    fun quotedAttributeValuesEscapeTheQuoteAndTheBackslash() {
        assertEquals("[title=\"say \\\"hi\\\"\"]", "${attributeSelector("title", "say \"hi\"")}")
        assertEquals("[path=\"a\\\\b\"]", "${attributeSelector("path", "a\\b")}")
    }

    @Test
    fun pseudosTakeAStringOrASelectorArgument() {
        assertEquals(":hover", "${pseudo(Pseudos.hover)}")
        assertEquals("::before", "${pseudo(Pseudos.before)}")
        assertEquals(":nth-child(2n+1)", "${pseudo(pseudoClass("nth-child"), "2n+1")}")
        assertEquals("::part(label)", "${pseudo(pseudoElement("part"), "label")}")
        assertEquals(":not(.is-selected)", "${pseudo(pseudoClass("not"), className(SELECTED))}")
        assertEquals(
            ":is(.row,.label)",
            "${pseudo(pseudoClass("is"), selectorList(className(ROW), className(LABEL)))}",
        )
    }

    @Test
    fun aVariantIsAPartWithNoAttachedParent() {
        // `Pseudos.*` hold the pseudo text alone; `&` belongs to the scope that attaches them to a
        // rule, so the same value is a clean part of any selector — no marker to strip.
        assertEquals(":hover", "${Pseudos.hover}")
        assertEquals("::before", "${Pseudos.before}")
        assertEquals(":focus-visible", "${Pseudos.focusVisible}")
        assertEquals(":hover", "${pseudo(Pseudos.hover)}")
        assertEquals(".row:first-child", "${compound(className(ROW), pseudo(Pseudos.firstChild))}")
    }

    @Test
    fun compoundsAndCombinatorsJoinTheParts() {
        assertEquals(".row.is-selected", "${compound(className(ROW), className(SELECTED))}")

        val row = className(ROW)
        val cell = className(LABEL)
        assertEquals(".row .label", "${row descendant cell}")
        assertEquals(".row > .label", "${row child cell}")
        assertEquals(".row + .label", "${row nextSibling cell}")
        assertEquals(".row ~ .label", "${row subsequentSibling cell}")
        assertEquals(".row || .label", "${row column cell}")

        // Infix is left-associative, so a chain reads like the selector it produces.
        assertEquals(".row > .label .deep", "${row child cell descendant className("deep")}")
    }

    @Test
    fun selectorListsAreCommaJoined() {
        assertEquals(".row,.label", "${selectorList(className(ROW), className(LABEL))}")
        assertEquals(
            ".row > .label,.row > #main",
            "${selectorList(className(ROW) child className(LABEL), className(ROW) child id("main"))}",
        )
    }

    @Test
    fun hostWithAnAttributeIsOneCompound() {
        // `:host([collapsed]) > button` — the shape a Vaadin component writes by hand today. The
        // attribute sits *inside* the pseudo's parentheses, so it is the pseudo's argument, not a
        // sibling simple selector.
        val hostCollapsed = pseudo(Pseudos.host, attributeSelector("collapsed"))
        assertEquals(":host([collapsed]) > button", "${hostCollapsed child tag("button")}")
    }

    @Test
    fun aBuiltSelectorBecomesARuleKey() {
        // A selector is a value first: name it, then use it as a key. (A `vararg` factory cannot take
        // a trailing block directly, the same as `many(…)`; naming the value is the clearer form.)
        val selected = compound(className(ROW), className(SELECTED))
        val nested = className(ROW) child className(LABEL)

        val text = cssText {
            selected {
                display = Display.block
            }
            nested {
                margin = Margin(8.px, 8.px)
            }
        }

        assertTrue(text.contains(".row.is-selected{display:block;}"), text)
        assertTrue(text.contains(".row>.label{margin:8px 8px;}"), text)
    }

    @Test
    fun asSelectorIsTheCssomValueTheRulesUse() {
        val built = className(ROW) child className(LABEL)
        // Combinators render with the spaces CSS writes; stylis compacts them inside a sheet.
        assertEquals(".row > .label", "${built.asSelector()}")
    }
}
