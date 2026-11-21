package io.github.shijianjs.ktjs.dom.webcomponent.attr

import io.github.shijianjs.ktjs.dom.element.invoke
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The registry the processor writes after its last round: one object listing every element of the
 * compilation.
 *
 * Reading it initialises each element's companion, which is the reference, and the reference defines
 * its tag in `init` — so a site (or this test) can say "define everything this module ships" without
 * touching the companions one by one. Creating an instance is the assertion that the tag exists: an
 * undefined tag throws when constructed.
 */
class CustomElementsRegistryTest {

    @Test
    fun everyElementTheProcessorGeneratedIsListed() {
        assertEquals(
            listOf("ktjs-attr-probe", "ktjs-attribute-probe-child", "ktjs-attr-upgrade-probe"),
            KtjsCustomElements.TAGS,
        )
        assertEquals(3, KtjsCustomElements.ALL.size)
    }

    @Test
    fun readingTheRegistryDefinesTheTags() {
        KtjsCustomElements.ALL

        AttributeProbe.NAME()
        AttributeProbeChild.NAME()
        // `UpgradeProbe` keeps `register = false` — a test decides when its tag exists — and the list
        // inherits that decision instead of overriding it.
    }
}
