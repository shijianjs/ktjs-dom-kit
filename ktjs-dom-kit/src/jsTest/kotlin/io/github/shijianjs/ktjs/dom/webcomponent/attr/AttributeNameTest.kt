package io.github.shijianjs.ktjs.dom.webcomponent.attr

import kotlin.test.Test
import kotlin.test.assertEquals

/** The name a property gets when `attr` is not told one. */
class AttributeNameTest {

    @Test
    fun camelCaseBecomesKebabCase() {
        assertEquals("max-count", "maxCount".toAttributeName())
        assertEquals("logo-src", "logoSrc".toAttributeName())
        assertEquals("section-title", "sectionTitle".toAttributeName())
    }

    @Test
    fun namesThatAreAlreadyLowercaseAreLeftAlone() {
        assertEquals("text", "text".toAttributeName())
        assertEquals("h1", "h1".toAttributeName())
        assertEquals("logo-src", "logo-src".toAttributeName())
        assertEquals("kind2-value", "kind2Value".toAttributeName())
    }

    @Test
    fun aRunOfCapitalsEndsAtTheLastOne() {
        assertEquals("xml-http", "XMLHttp".toAttributeName())
        assertEquals("id", "ID".toAttributeName())
    }
}
