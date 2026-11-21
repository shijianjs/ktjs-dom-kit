package io.github.shijianjs.ktjs.dom.webcomponent.ksp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The naming rules the processor applies, pinned against the cases `AttributeNameTest` on the library
 * side asserts — the runtime keeps its own copy (`toAttributeName`), and the two must agree or the
 * constant the element hands to `attr` would name a different attribute than the one `by attr { }` would
 * have derived on its own.
 *
 * The registry rules are here too: the object's name comes from the tag family, and its package from
 * what the elements share, so two modules of one site cannot land on each other.
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

    @Test
    fun thePrefixIsAddedUnlessTheClassNameAlreadyCarriesIt() {
        assertEquals("demo-step-badge", tagOf("demo-", "StepBadge"))
        assertEquals("demo-counter", tagOf("demo-", "DemoCounter"), "the mark is not doubled")
        assertEquals("demo-attribute-page", tagOf("demo-", "AttributePage"))
        assertEquals("ktjs-hero", tagOf("ktjs-", "KtjsHero"))
        assertEquals("step-badge", tagOf("", "StepBadge"), "an empty prefix adds nothing")
    }

    @Test
    fun anEnumPropertyGetsACodecNextToItsAttributeConstant() {
        assertEquals("KIND_CODEC", codecNameOf("kind"))
        assertEquals("INCREMENT_LABEL_CODEC", codecNameOf("incrementLabel"))
    }

    @Test
    fun theRegistryIsNamedAfterTheTagFamily() {
        assertEquals("DemoCustomElements", registryNameOf("demo-"))
        assertEquals("KtjsCustomElements", registryNameOf("ktjs-"))
        assertEquals("MyWcCustomElements", registryNameOf("my-wc-"))
        assertEquals("CustomElements", registryNameOf(""), "no prefix, no family name")
    }

    @Test
    fun theRegistryGoesToTheDeepestPackageTheElementsShare() {
        assertEquals(
            "io.github.shijianjs.ktjs.dom.demo.page",
            commonPackageOf(
                listOf(
                    "io.github.shijianjs.ktjs.dom.demo.page.attribute",
                    "io.github.shijianjs.ktjs.dom.demo.page.webcomponent",
                    "io.github.shijianjs.ktjs.dom.demo.page",
                ),
            ),
        )
        assertEquals("kotlin.test", commonPackageOf(listOf("kotlin.test")), "one element, its own package")
        assertNull(commonPackageOf(listOf("demo.page", "demo2.page")), "nothing in common")
        assertNull(commonPackageOf(emptyList()))
    }
}
