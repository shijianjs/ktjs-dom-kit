package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.element.createElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import web.html.HTMLElement
import web.html.HtmlTagName

/** An extension property on `HTMLElement` itself — the shape a wrapper type would carry. */
private val HTMLElement.count: Attribute<Int> get() = intAttribute("data-count")

private val HTMLElement.tags: TokenListAttribute get() = tokenListAttribute("data-tags")

/**
 * The wrapper-facing half of `attr`: an [Attribute] (or [TokenListAttribute]) bound to an element the
 * kit does not own (here: plain `div`s, the same shape a wrapper element is). Nothing registers and
 * nothing is cached, so every assertion is just "the attribute is the value".
 *
 * There is no `by` variant on purpose: wrapper elements are `external` classes, so the only way to
 * give them a named property is an extension property — and Kotlin does not delegate those. The
 * extension property hands out the operation object instead.
 */
class WrapperAttributeTest {

    private fun element(): HTMLElement = HtmlTagName.div.createElement()

    @Test
    fun anAttributeReadsAndWritesTheElement() {
        val node = element()
        val count = node.count
        assertEquals(0, count.value)
        assertFalse(count.present)

        count.value = 7
        assertEquals("7", node.getAttribute("data-count"))
        assertEquals(7, count.value)
        assertTrue(count.present)

        node.setAttribute("data-count", "12")
        assertEquals(12, count.value)
    }

    @Test
    fun clearingRemovesTheAttribute() {
        val node = element()
        val count = node.count
        count.value = 3

        count.clear()
        assertNull(node.getAttribute("data-count"))
        assertFalse(count.present)
        assertEquals(0, count.value)
    }

    @Test
    fun anUndecodableValueFallsBackToAbsent() {
        val node = element()
        val count = node.count
        node.setAttribute("data-count", "not a number")
        assertEquals(0, count.value)
        assertEquals("not a number", count.raw)

        val flagged = node.booleanAttribute("data-flagged")
        flagged.value = false
        assertNull(node.getAttribute("data-flagged"))
        assertFalse(flagged.value)
    }

    @Test
    fun eachObjectIsBoundToOneElement() {
        val first = element()
        val second = element()
        first.count.value = 1
        second.count.value = 2
        assertEquals(1, first.count.value)
        assertEquals(2, second.count.value)
    }

    @Test
    fun aCustomCodecIsAnOrdinaryAttribute() {
        val node = element()
        val upper = node.attribute(
            "data-name",
            object : AttributeCodec<String> {
                override fun encode(value: String): String? = value.uppercase()
                override fun decode(value: String): String? = value
                override val absent: String = ""
            },
        )

        upper.value = "abc"
        assertEquals("ABC", node.getAttribute("data-name"))
        assertEquals("ABC", upper.value)

        node.setAttribute("data-name", "mixed")
        assertEquals("mixed", upper.value)
    }

    @Test
    fun aTokenListNormalisesTheText() {
        val node = element()
        node.setAttribute("data-tags", "  a   b a ")
        assertEquals(listOf("a", "b"), node.tags.values)

        node.tags.value = listOf("c", "c", "a")
        assertEquals("c a", node.getAttribute("data-tags"))

        node.tags.value = emptyList()
        assertNull(node.getAttribute("data-tags"))
        assertEquals(emptyList(), node.tags.values)
    }

    @Test
    fun aTokenListMutatesTheAttributeInPlace() {
        val node = element()
        val tokens = node.tokenListAttribute("data-tags")

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
        node.tokenListAttribute("data-tags").add("a b", "c")
        assertEquals("a b c", node.getAttribute("data-tags"))
        assertEquals(listOf("a", "b", "c"), node.tokenListAttribute("data-tags").values)
    }

    @Test
    fun aTextWrittenFromOutsideIsVisibleImmediately() {
        val node = element()
        val tokens = node.tokenListAttribute("data-tags")
        tokens.add("a")
        node.setAttribute("data-tags", "b")
        assertEquals(listOf("b"), tokens.values)
        assertEquals("b", tokens.raw)
    }
}
