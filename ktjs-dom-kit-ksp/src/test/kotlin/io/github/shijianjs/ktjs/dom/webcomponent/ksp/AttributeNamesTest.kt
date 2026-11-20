package io.github.shijianjs.ktjs.dom.webcomponent.ksp

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The two naming rules the processor applies, pinned against the cases `AttributeNameTest` on the
 * library side asserts — the runtime keeps its own copy (`toAttributeName`), and the two must agree
 * or the constant the element hands to `attr` would name a different attribute than the one
 * `by attr { }` would have derived on its own.
 */
class AttributeNamesTest {

    @Test
    fun aCamelCaseNameBecomesKebabCase() {
        assertEquals("max-count", attributeNameOf("maxCount"))
        assertEquals("logo-src", attributeNameOf("logoSrc"))
        assertEquals("section-title", attributeNameOf("sectionTitle"))
    }

    @Test
    fun lowerCaseAndDashesAreLeftAlone() {
        assertEquals("text", attributeNameOf("text"))
        assertEquals("h1", attributeNameOf("h1"))
        assertEquals("logo-src", attributeNameOf("logo-src"))
        assertEquals("kind2-value", attributeNameOf("kind2Value"))
    }

    @Test
    fun aRunOfCapitalsBreaksBeforeItsLastLetter() {
        assertEquals("xml-http", attributeNameOf("XMLHttp"))
        assertEquals("id", attributeNameOf("ID"))
    }

    @Test
    fun aConstantIsThePropertyNameInUpperSnakeCase() {
        assertEquals("TEXT", constantNameOf("text"))
        assertEquals("MAX_COUNT", constantNameOf("maxCount"))
        assertEquals("XML_HTTP", constantNameOf("XMLHttp"))
        assertEquals("ID", constantNameOf("ID"))
    }
}
