package io.github.shijianjs.ktjs.dom.webcomponent.attr

import io.github.shijianjs.ktjs.dom.element.invoke
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A custom element that extends another one.
 *
 * A tag is its own `customElements.define`: the browser reads `observedAttributes` from
 * [AttributeProbeChild], and an instance creates its delegates from the declarations of both classes.
 * So the child's generated lists have to be the parent's plus its own — and they are, because the
 * processor merged them, not because anyone wrote them twice.
 */
class AttributeProbeChildTest {

    @Test
    fun theGeneratedListsAreTheParentsPlusThisClassOwn() {
        val child = AttributeProbeChild.NAME()

        assertEquals(
            AttributeProbeDefaultReference.ALL.toList() + "extra",
            AttributeProbeChildDefaultReference.ALL.toList(),
        )
        assertEquals(AttributeProbeChildDefaultReference.ALL.toList(), child.attributeNames())
    }

    @Test
    fun anAttributeTheParentDeclaredReachesTheChildsProperty() {
        val child = AttributeProbeChild.NAME()
        child.applyInitialAttributes()
        child.changes.clear()

        child.setAttribute("data-explicit", "from outside")

        assertEquals("from outside", child.explicit)
        assertEquals(listOf("data-explicit"), child.changes.map { it.name })
    }

    @Test
    fun theCodecTheParentGeneratedReadsTheChildsAttribute() {
        val child = AttributeProbeChild.NAME()
        child.applyInitialAttributes()
        child.changes.clear()

        child.setAttribute("kind", "secondary")

        assertEquals(ProbeKind.Secondary, child.kind)
    }

    @Test
    fun theOwnAttributeIsListedToo() {
        val child = AttributeProbeChild.NAME()
        child.applyInitialAttributes()

        assertEquals(7, child.extra)

        child.setAttribute("extra", "42")

        assertEquals(42, child.extra)
    }
}
