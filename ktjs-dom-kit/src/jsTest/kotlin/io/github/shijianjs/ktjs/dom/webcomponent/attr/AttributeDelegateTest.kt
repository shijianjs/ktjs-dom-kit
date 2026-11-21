package io.github.shijianjs.ktjs.dom.webcomponent.attr

import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import js.reflect.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import web.dom.document
import web.html.HtmlSource

/**
 * The `attr` delegate against a real custom element in a real browser: the attribute is the only
 * state, so every case is checked from both sides — the Kotlin property and `getAttribute`.
 *
 * Changes made outside a property setter arrive through the browser's synchronous
 * `attributeChangedCallback`, so a write is reported before the line that made it returns.
 * [CustomElementAdapter.applyInitialAttributes] is checked separately: it is the startup catch-up
 * (and the path a name missing from `observedAttributes` would rely on).
 */
class AttributeDelegateTest {

    /**
     * The generated `observedAttributes` array must be the same list the properties register, in the
     * same order. A name the processor missed would not fail anywhere else: the property would still
     * read the attribute, and only a later external write to it would go unreported.
     */
    @Test
    fun theGeneratedListMatchesTheDeclarations() {
        val probe = AttributeProbe.NAME()

        assertEquals(AttributeProbeDefaultReference.ALL.toList(), probe.attributeNames())
    }

    @Test
    fun thePropertyIsTheAttribute() {
        val probe = AttributeProbe.NAME()

        assertEquals("", probe.text)
        assertFalse(probe.hasAttribute("text"))

        probe.text = "hello"

        assertEquals("hello", probe.getAttribute("text"))
        assertEquals("hello", probe.text)

        probe.setAttribute("text", "from the outside")

        assertEquals("from the outside", probe.text)
    }

    @Test
    fun theSetterReportsTheNewValue() {
        val probe = AttributeProbe.NAME()
        probe.text = "hello"

        val change = probe.changes.single()
        assertEquals("text", change.name)
        // the first report has no previous value, so old equals new
        assertEquals("hello", change.oldValue)
        assertEquals("hello", change.newValue)
        assertEquals("hello", change.oldRaw)
        assertEquals("hello", change.newRaw)
        assertFalse(change.isRemoval)
    }

    @Test
    fun writingTheSameValueTwiceReportsOnce() {
        val probe = AttributeProbe.NAME()

        probe.text = "same"
        probe.text = "same"

        assertEquals(1, probe.changes.size)
    }

    @Test
    fun anExternalWriteIsReportedByTheNextSync() {
        val probe = AttributeProbe.NAME()
        probe.applyInitialAttributes()
        probe.changes.clear()

        probe.setAttribute("text", "outside")
        assertEquals("outside", probe.text)

        probe.applyInitialAttributes()

        val change = probe.changes.single()
        assertEquals("text", change.name)
        assertEquals("outside", change.newValue)
        assertEquals("outside", change.newRaw)
    }

    /**
     * The write with no manual sync and no microtask: the element's `attributeChangedCallback` runs
     * synchronously inside `setAttribute`, so the callback has already seen the change.
     */
    @Test
    fun anExternalWriteIsReportedSynchronously() {
        val probe = AttributeProbe.NAME()
        probe.applyInitialAttributes()
        probe.changes.clear()

        probe.setAttribute("text", "from outside")

        val change = probe.changes.single()
        assertEquals("text", change.name)
        assertEquals("", change.oldValue)
        assertEquals("from outside", change.newValue)
        assertNull(change.oldRaw)
        assertEquals("from outside", change.newRaw)
    }

    /** The same for a removal, and the property reads back the default the codec declares. */
    @Test
    fun anExternalRemovalIsReportedSynchronously() {
        val probe = AttributeProbe.NAME()
        probe.maxCount = 42
        probe.changes.clear()

        probe.removeAttribute("max-count")

        val change = probe.changes.single()
        assertEquals("max-count", change.name)
        assertTrue(change.isRemoval)
        assertEquals(42, change.oldValue)
        assertEquals(10, change.newValue)
        assertNull(change.newRaw)
    }

    /**
     * Markup written before the class existed: the parser leaves the tag as an unknown element, and
     * `customElements.define` reports every observed attribute it was carrying, once. The probe is
     * built with `register = false`, so this test — not the companion's `init` — is what defines the
     * tag; if a previous test already did, the parser upgrades the element on construction instead
     * and makes the same single report.
     */
    @Test
    fun attributesOnMarkupUpgradedLaterAreReported() {
        document.body.innerHTML = HtmlSource("<ktjs-attr-upgrade-probe upgrade-label='late'></ktjs-attr-upgrade-probe>")
        val element = document.body.firstElementChild
        try {
            defineIfMissing(UpgradeProbe.NAME, UpgradeProbe)

            val probe = element.unsafeCast<UpgradeProbe>()
            assertEquals("late", probe.label)

            val change = probe.changes.single()
            assertEquals("upgrade-label", change.name)
            assertEquals("late", change.newValue)
            assertEquals("late", change.newRaw)
        } finally {
            document.body.innerHTML = HtmlSource("")
        }
    }

    @Test
    fun markupAttributesAreReportedOnConnect() {
        AttributeProbe.NAME // must be defined before the parser sees the tag
        document.body.innerHTML = HtmlSource("<ktjs-attr-probe text='from markup' max-count='3' flagged></ktjs-attr-probe>")

        val probe = document.body.firstElementChild.unsafeCast<AttributeProbe>()
        try {
            assertEquals("from markup", probe.text)
            assertEquals(3, probe.maxCount)
            assertTrue(probe.flagged)

            val reported = probe.changes.associateBy { it.name }
            assertEquals("from markup", reported.getValue("text").newValue)
            assertEquals("from markup", reported.getValue("text").newRaw)
            // the first report has no previous value, so both sides carry the markup's value
            assertEquals("from markup", reported.getValue("text").oldValue)
            assertEquals("from markup", reported.getValue("text").oldRaw)
        } finally {
            document.body.innerHTML = HtmlSource("")
        }
    }

    @Test
    fun removingAnAttributeGoesBackToTheDefault() {
        val probe = AttributeProbe.NAME()
        probe.setAttribute("max-count", "42")
        probe.applyInitialAttributes()
        probe.changes.clear()

        probe.removeAttribute("max-count")
        probe.applyInitialAttributes()

        val change = probe.changes.single()
        assertTrue(change.isRemoval)
        assertEquals(42, change.oldValue)
        assertEquals(10, change.newValue)
        assertEquals(10, probe.maxCount)
    }

    @Test
    fun booleanIsPresenceNotTheValue() {
        val probe = AttributeProbe.NAME()

        probe.setAttribute("flagged", "false")
        probe.applyInitialAttributes()
        assertTrue(probe.flagged)

        probe.flagged = false
        assertFalse(probe.hasAttribute("flagged"))
        assertFalse(probe.flagged)

        probe.setAttribute("flagged", "")
        probe.applyInitialAttributes()
        assertTrue(probe.flagged)

        probe.flagged = true
        assertEquals("", probe.getAttribute("flagged"))
    }

    @Test
    fun numbersAreConvertedAndUnparsableOnesFallBack() {
        val probe = AttributeProbe.NAME()

        probe.maxCount = 42
        assertEquals("42", probe.getAttribute("max-count"))
        assertEquals(42, probe.maxCount)

        probe.setAttribute("max-count", "not a number")
        probe.applyInitialAttributes()
        assertEquals(10, probe.maxCount)

        probe.count = 0
        assertEquals("0", probe.getAttribute("count"))
        assertEquals(0, probe.count)
    }

    @Test
    fun doublesRoundTrip() {
        val probe = AttributeProbe.NAME()

        probe.ratio = 2.25
        assertEquals("2.25", probe.getAttribute("ratio"))
        assertEquals(2.25, probe.ratio)

        probe.setAttribute("ratio", "0.5")
        probe.applyInitialAttributes()
        assertEquals(0.5, probe.ratio)
    }

    @Test
    fun aCustomCodecMapsTheEnum() {
        val probe = AttributeProbe.NAME()
        assertEquals(ProbeKind.Primary, probe.kind)

        probe.setAttribute("kind", "secondary")
        probe.applyInitialAttributes()
        assertEquals(ProbeKind.Secondary, probe.kind)

        probe.kind = ProbeKind.Secondary
        assertEquals("secondary", probe.getAttribute("kind"))

        probe.setAttribute("kind", "nonsense")
        probe.applyInitialAttributes()
        assertEquals(ProbeKind.Primary, probe.kind)
    }

    @Test
    fun theAttributeNameComesFromThePropertyOrTheExplicitName() {
        val probe = AttributeProbe.NAME()
        probe.setAttribute("max-count", "7")
        probe.setAttribute("data-explicit", "explicit")
        probe.setAttribute("internal-name", "kotlin name")
        probe.applyInitialAttributes()

        assertEquals(7, probe.maxCount)
        assertEquals("explicit", probe.explicit)
        assertEquals("kotlin name", probe.internalName)
    }

    @Test
    fun jsNameRenamesThePropertyNotTheAttribute() {
        val probe = AttributeProbe.NAME()
        probe.internalName = "value"
        probe.text = "plain"
        assertEquals("plain", probe.text)

        val asJs = probe.unsafeCast<dynamic>()
        assertEquals("value", asJs.publicName as String?)
        assertEquals("value", probe.getAttribute("internal-name"))
        // without `@JsName` Kotlin/JS emits only mangled accessor methods, no JS property at all
        assertEquals(null, asJs.text as String?)
    }

    /**
     * React 19 assigns every matching JSX prop to the property that exists on a custom element, so
     * `<ktjs-action kind="secondary">` reaches this setter as a **string** even though the property's
     * type is the enum — and a `tinted="false"`-style boolean prop as one too. A string is the
     * attribute's raw form, so it is written verbatim instead of being handed to the codec.
     */
    @Test
    fun aStringAssignedFromJavaScriptIsTheAttributesRawValue() {
        val probe = AttributeProbe.NAME()
        probe.applyInitialAttributes()
        probe.changes.clear()
        val asJs = probe.unsafeCast<dynamic>()

        asJs.kind = "secondary"
        assertEquals("secondary", probe.getAttribute("kind"))
        assertEquals(ProbeKind.Secondary, probe.kind)

        asJs.maxCount = "7"
        assertEquals("7", probe.getAttribute("max-count"))
        assertEquals(7, probe.maxCount)

        // HTML boolean semantics are unchanged: it is the attribute's presence that counts.
        asJs.flagged = "false"
        assertEquals("false", probe.getAttribute("flagged"))
        assertTrue(probe.flagged)

        assertEquals(listOf("kind", "max-count", "flagged"), probe.changes.map { it.name })
    }

    @Test
    fun everyDelegateReportsItsOwnAttribute() {
        val probe = AttributeProbe.NAME()
        probe.applyInitialAttributes()

        assertEquals(
            listOf("text", "count", "max-count", "ratio", "flagged", "data-explicit", "internal-name", "kind",
                "second", "mirror", "echo"),
            probe.changes.map { it.name },
        )
    }

    @Test
    fun aCallbackMayWriteAnotherAttribute() {
        val probe = AttributeProbe.NAME()
        probe.applyInitialAttributes()
        probe.changes.clear()

        probe.mirror = "abcd"

        assertEquals("4", probe.getAttribute("second"))
        assertEquals(listOf("mirror", "second"), probe.changes.map { it.name })
    }

    @Test
    fun aCallbackWritingItsOwnPropertyDoesNotReenter() {
        val probe = AttributeProbe.NAME()

        probe.echo = "once"

        assertEquals(listOf("echo"), probe.changes.map { it.name })
        assertEquals("once", probe.getAttribute("echo"))
    }

    @Test
    fun applyingInitialAttributesTwiceChangesNothing() {
        val probe = AttributeProbe.NAME()
        probe.setAttribute("text", "value")

        probe.applyInitialAttributes()
        probe.applyInitialAttributes()

        assertEquals(1, probe.changes.count { it.name == "text" })
    }

    @Test
    fun reconnectingDoesNotRepeatTheReports() {
        val probe = AttributeProbe.NAME()
        document.body.appendChild(probe)
        try {
            val reported = probe.changes.size
            assertTrue(reported > 0)

            document.body.removeChild(probe)
            document.body.appendChild(probe)

            assertEquals(reported, probe.changes.size)
        } finally {
            document.body.removeChild(probe)
        }
    }

    @Test
    fun anAttributeChangeWhileDisconnectedIsStillReported() {
        val probe = AttributeProbe.NAME()
        document.body.appendChild(probe)
        document.body.removeChild(probe)
        probe.changes.clear()

        probe.setAttribute("text", "detached")
        probe.applyInitialAttributes()

        assertEquals("detached", probe.changes.single().newValue)
    }

    @Test
    fun defaultsStayOutOfTheMarkupUnlessAskedFor() {
        val probe = AttributeProbe.NAME()

        probe.applyInitialAttributes()
        assertNull(probe.getAttribute("max-count"))

        probe.applyInitialAttributes(writeDefaults = true)
        assertEquals("10", probe.getAttribute("max-count"))
    }

    @Test
    fun theInitialValueIsNotWrittenBack() {
        val probe = AttributeProbe.NAME()
        probe.applyInitialAttributes()

        assertNull(probe.getAttribute("ratio"))
        assertNull(probe.getAttribute("kind"))
        assertFalse(probe.hasAttribute("flagged"))
    }

    @Test
    fun theIntegerSetterReportsTheDecodedValues() {
        val probe = AttributeProbe.NAME()
        probe.applyInitialAttributes()
        probe.changes.clear()

        probe.count = 3

        val change = probe.changes.single()
        assertEquals(0, change.oldValue)
        assertEquals(3, change.newValue)
        assertEquals("3", change.newRaw)
    }
}
