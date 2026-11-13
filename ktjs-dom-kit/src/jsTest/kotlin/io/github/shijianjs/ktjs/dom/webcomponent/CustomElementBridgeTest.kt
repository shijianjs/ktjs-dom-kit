package io.github.shijianjs.ktjs.dom.webcomponent

import io.github.shijianjs.ktjs.dom.attribute.newProbe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import web.dom.document

/**
 * [stripBridgesFrom] deletes the compiler-generated forwarders; the native spelling then has to work
 * on the element itself. Each call here would recurse forever if its bridge were left in place.
 */
class CustomElementBridgeTest {

    @Test
    fun theNativeSpellingWorksOnTheElement() {
        val probe = newProbe()
        val child = document.createElement("span")

        probe.appendChild(child)
        assertEquals(1, probe.children.length)
        assertSame(child, probe.firstElementChild)
        assertEquals(1, probe.querySelectorAll("span").length)
        assertSame(child, probe.querySelector("span"))

        probe.append(document.createElement("i"))
        assertEquals(2, probe.childNodes.length)

        probe.remove()
        assertNull(probe.parentNode)
    }
}
