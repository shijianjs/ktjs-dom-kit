package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.attribute.types.BooleanAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.IntAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.TokenListAttributeCodec
import io.github.shijianjs.ktjs.dom.element.createElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import web.html.HTMLElement
import web.html.HtmlTagName

/**
 * The wrapper-facing half of `attr`: an [Attribute] definition applied to elements the kit does not
 * own (here: plain `div`s, the same shape a wrapper element is). Nothing registers and nothing is
 * cached, so every assertion is just "the attribute is the value".
 *
 * There is no `by` variant on purpose: wrapper elements are `external` classes, so the only way to
 * give them a named property is an extension property — and Kotlin does not delegate those.
 */
class WrapperAttributeTest {

    private val Count = Attribute("data-count", IntAttributeCodec)
    private val Tags = Attribute("data-tags", TokenListAttributeCodec)
    private val Flagged = Attribute("data-flagged", BooleanAttributeCodec)

    private fun element(): HTMLElement = HtmlTagName.div.createElement()

    @Test
    fun aDefinitionReadsAndWritesWithoutAnyState() {
        val node = element()
        assertEquals(0, node.readAttribute(Count))

        node.writeAttribute(Count, 7)
        assertEquals("7", node.getAttribute("data-count"))
        assertEquals(7, node.readAttribute(Count))

        node.setAttribute("data-count", "12")
        assertEquals(12, node.readAttribute(Count))
    }

    @Test
    fun anUndecodableValueFallsBackToAbsent() {
        val node = element()
        node.setAttribute("data-count", "not a number")
        assertEquals(0, node.readAttribute(Count))

        node.writeAttribute(Flagged, false)
        assertNull(node.getAttribute("data-flagged"))
        assertFalse(node.readAttribute(Flagged))
    }

    @Test
    fun oneDefinitionServesEveryElementThatCarriesIt() {
        val first = element()
        val second = element()
        first.writeAttribute(Tags, listOf("a"))
        second.writeAttribute(Tags, listOf("b"))
        assertEquals(listOf("a"), first.readAttribute(Tags))
        assertEquals(listOf("b"), second.readAttribute(Tags))
    }

    @Test
    fun aTokenListAttributeNormalisesTheText() {
        val node = element()
        node.setAttribute("data-tags", "  a   b a ")
        assertEquals(listOf("a", "b"), node.readAttribute(Tags))

        node.writeAttribute(Tags, listOf("c", "c", "a"))
        assertEquals("c a", node.getAttribute("data-tags"))

        node.writeAttribute(Tags, emptyList())
        assertNull(node.getAttribute("data-tags"))
        assertEquals(emptyList(), node.readAttribute(Tags))
    }

    @Test
    fun aTokenListViewMutatesTheAttributeInPlace() {
        val node = element()
        val tokens = node.tokens(Tags)

        assertFalse(tokens.contains("a"))
        tokens.add("a", "b")
        assertEquals("a b", node.getAttribute("data-tags"))
        assertTrue(tokens.contains("b"))

        // adding what is already there changes nothing, not even the text
        tokens.add("a")
        assertEquals("a b", node.getAttribute("data-tags"))

        tokens.remove("a")
        assertEquals("b", node.getAttribute("data-tags"))

        assertTrue(tokens.toggle("c"))
        assertEquals("b c", node.getAttribute("data-tags"))
        assertFalse(tokens.toggle("c"))
        assertEquals("b", node.getAttribute("data-tags"))
        assertTrue(tokens.toggle("c", present = true))
        assertEquals("b c", node.getAttribute("data-tags"))

        tokens.clear()
        assertNull(node.getAttribute("data-tags"))
        assertTrue(tokens.values.isEmpty())
    }

    @Test
    fun aTokenListSplitsTokensGivenTogether() {
        val node = element()
        node.tokens(Tags).add("a b", "c")
        assertEquals("a b c", node.getAttribute("data-tags"))
        assertEquals(listOf("a", "b", "c"), node.tokens(Tags).values)
    }

    @Test
    fun aTextWrittenFromOutsideIsVisibleImmediately() {
        val node = element()
        val tokens = node.tokens(Tags)
        tokens.add("a")
        node.setAttribute("data-tags", "b")
        assertEquals(listOf("b"), tokens.values)
        assertEquals("b", tokens.raw)
    }
}
