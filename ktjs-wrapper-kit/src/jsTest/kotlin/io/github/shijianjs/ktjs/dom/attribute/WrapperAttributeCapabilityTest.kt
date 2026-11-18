package io.github.shijianjs.ktjs.dom.attribute

import js.array.jsArrayOf
import js.reflect.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import web.dom.document
import web.html.HTMLElement
import web.html.HtmlTagName

/** A wrapper's vocabulary: upstream literals become `sealed external interface` values. */
private sealed external interface Orientation

private fun Orientation(token: String): Orientation = token.unsafeCast<Orientation>()

private object Orientations {
    val horizontal: Orientation = Orientation("horizontal")
    val vertical: Orientation = Orientation("vertical")
    val all: List<Orientation> = listOf(horizontal, vertical)
}

private val Orientation.token: String get() = unsafeCast<String>()

/** The shapes a wrapper needs on top of scalars, each as an extension property on the element. */
private val HTMLElement.orientation: Attribute<Orientation>
    get() = literalAttribute("orientation", Orientations.all) { it.token }

private val HTMLElement.ariaLabelAttr: Attribute<String> get() = attribute("aria-label")

private val HTMLElement.items: JsonAttribute get() = jsonAttribute("items")

/**
 * The capabilities a wrapper layer needs beyond "read and write a text attribute".
 *
 * Scalars and token lists were already covered; a wrapper site additionally needs the
 * absent-versus-empty distinction (its attributes are optional), a typed vocabulary instead of bare
 * strings, structured JSON for list-taking components, and reactivity to writes it did not make
 * ([Attribute.observe]).
 */
class WrapperAttributeCapabilityTest {

    private fun element(): HTMLElement = document.createElement(HtmlTagName.div)

    @Test
    fun anAbsentAttributeIsNotAnEmptyOne() {
        val node = element()
        val label = node.ariaLabelAttr

        assertNull(label.valueOrNull)
        assertEquals("", label.value)

        label.value = ""
        assertEquals("", node.getAttribute("aria-label"))
        assertEquals("", label.valueOrNull)

        label.setOrClear(null)
        assertNull(node.getAttribute("aria-label"))
        assertNull(label.valueOrNull)
    }

    @Test
    fun setOrClearWritesAValueOrRemovesIt() {
        val node = element()
        val label = node.ariaLabelAttr

        label.setOrClear("Close")
        assertEquals("Close", node.getAttribute("aria-label"))

        label.setOrClear(null)
        assertFalse(label.present)
    }

    @Test
    fun aLiteralAttributeOnlyAcceptsItsVocabulary() {
        val node = element()
        val orientation = node.orientation
        assertEquals(Orientations.horizontal, orientation.value)
        assertFalse(orientation.present)

        orientation.value = Orientations.vertical
        assertEquals("vertical", node.getAttribute("orientation"))
        assertEquals(Orientations.vertical, orientation.value)

        // Decoding is case-insensitive; anything outside the table reads as the first value.
        node.setAttribute("orientation", "VERTICAL")
        assertEquals(Orientations.vertical, orientation.value)
        node.setAttribute("orientation", "diagonal")
        assertEquals(Orientations.horizontal, orientation.value)
    }

    @Test
    fun aJsonAttributeRoundTripsThroughTheBrowsersOwnJson() {
        val node = element()
        val items = node.items
        assertNull(items.value)
        assertFalse(items.present)

        items.value = jsArrayOf("one", "two")
        assertEquals("[\"one\",\"two\"]", node.getAttribute("items"))
        assertEquals(2, items.decode<Array<String>>()?.size)
        assertEquals("two", items.decode<Array<String>>()?.get(1))

        items.clear()
        assertNull(node.getAttribute("items"))
        assertNull(items.decode<Array<String>>())
    }

    @Test
    fun brokenJsonReadsAsAbsentInsteadOfThrowing() {
        val node = element()
        val items = node.items
        node.setAttribute("items", "{not json")

        assertNull(items.value)
        assertNull(items.decode<Array<String>>())
        assertEquals("{not json", items.raw)
    }

    @Test
    fun aWatchSurvivesUntilItIsDisconnected() {
        val node = element()
        val watch = node.observeAttribute("theme") {}
        watch.disconnect()
        // Disconnecting twice is legal: the component that owns the watch may release it on both
        // `disconnectedCallback` and `adoptedCallback`.
        watch.disconnect()
    }
}
