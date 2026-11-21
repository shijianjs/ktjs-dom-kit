package io.github.shijianjs.ktjs.dom.webcomponent.attr

import io.github.shijianjs.ktjs.dom.element.invoke
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A hierarchy where the base declares attributes and has no tag of its own.
 *
 * The leaf's `observedAttributes` still has to list what the base declared, because the browser reads
 * that array per defined tag; the base's own generated reference exists only to be named by the leaf.
 */
class SubclassedProbeTest {

    @Test
    fun theChildsListsAreTheBasesPlusItsOwn() {
        val child = SubclassedProbeChild.NAME()

        assertEquals(listOf("caption", "extra"), SubclassedProbeChildDefaultReference.ALL.toList())
        assertEquals(SubclassedProbeChildDefaultReference.ALL.toList(), child.attributeNames())
    }

    @Test
    fun anAttributeTheBaseDeclaredReachesTheChildsProperty() {
        val child = SubclassedProbeChild.NAME()
        child.applyInitialAttributes()

        assertEquals("base", child.caption)

        child.setAttribute("caption", "from outside")

        assertEquals("from outside", child.caption)
    }

    @Test
    fun theOwnAttributeKeepsItsDefaultAndItsCodec() {
        val child = SubclassedProbeChild.NAME()
        child.applyInitialAttributes()

        assertEquals(3, child.extra)

        child.setAttribute("extra", "12")

        assertEquals(12, child.extra)
    }
}
