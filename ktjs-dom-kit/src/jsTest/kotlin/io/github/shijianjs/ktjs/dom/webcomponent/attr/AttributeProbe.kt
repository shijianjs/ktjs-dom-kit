package io.github.shijianjs.ktjs.dom.webcomponent.attr

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.enumAttributeCodec
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementReferenceAdapter
import kotlin.js.JsName

/** Enum for the custom-codec case; `kind` in the markup is the lower-cased constant name. */
internal enum class ProbeKind { Primary, Secondary }

/**
 * An element whose tag is defined *after* the markup exists, to check the upgrade path: the parser
 * leaves the tag as an unknown element, and `customElements.define` reports the attributes it was
 * carrying. `register = false` keeps the reference from defining the tag in its `init`, so a test
 * decides when that happens.
 */
internal class UpgradeProbe : CustomElementAdapter() {

    val changes: MutableList<AttributeChange<*>> = mutableListOf()

    @AttributeName("upgrade-label")
    var label: String by attr(UpgradeProbeAttributes.LABEL) { changes.add(it) }

    companion object : CustomElementReferenceAdapter<UpgradeProbe>(
        UpgradeProbe::class.js,
        "ktjs-attr-upgrade-probe",
        observedAttributes = UpgradeProbeAttributes.ALL,
        register = false,
    )
}

/**
 * A custom element with one delegate per feature under test.
 *
 * Every callback appends to [changes], so a test can assert what was reported and in which order.
 * Two callbacks do more than record: `mirror` writes another attribute, `echo` writes itself back.
 *
 * Both this class and [UpgradeProbe] name their attributes through the `…Attributes` object the
 * processor generates from these declarations: the generated constants are what `attr` receives and
 * `ALL` is what the browser reads as `observedAttributes`, so a name is authored once — as the
 * property name, or as `@AttributeName` where the attribute is spelled differently.
 */
internal class AttributeProbe : CustomElementAdapter() {

    val changes: MutableList<AttributeChange<*>> = mutableListOf()

    var text: String by attr(AttributeProbeAttributes.TEXT) { changes.add(it) }

    var count: Int by attr(AttributeProbeAttributes.COUNT) { changes.add(it) }

    @JsName("maxCount")
    var maxCount: Int by attr(AttributeProbeAttributes.MAX_COUNT, default = 10) { changes.add(it) }

    var ratio: Double by attr(AttributeProbeAttributes.RATIO, default = 1.5) { changes.add(it) }

    @JsName("flagged")
    var flagged: Boolean by attr(AttributeProbeAttributes.FLAGGED) { changes.add(it) }

    @AttributeName("data-explicit")
    var explicit: String by attr(AttributeProbeAttributes.EXPLICIT) { changes.add(it) }

    @JsName("publicName")
    var internalName: String by attr(AttributeProbeAttributes.INTERNAL_NAME) { changes.add(it) }

    @JsName("kind")
    var kind: ProbeKind by attr(
        AttributeProbeAttributes.KIND,
        codec = kindCodec,
        default = ProbeKind.Primary,
    ) { changes.add(it) }

    var second: Int by attr(AttributeProbeAttributes.SECOND, default = 5) { changes.add(it) }

    var mirror: String by attr(AttributeProbeAttributes.MIRROR) { change ->
        changes.add(change)
        if (change.newValue.isNotEmpty()) second = change.newValue.length
    }

    var echo: String by attr(AttributeProbeAttributes.ECHO) { change ->
        changes.add(change)
        // Writing the same value back must not re-enter this delegate.
        echo = change.newValue
    }

    override fun connectedCallback() {
        applyInitialAttributes()
    }

    // Registering is the companion's `init`: `AttributeProbe.NAME` defines the tag on first read, and
    // `AttributeProbe.NAME()` creates a detached probe — no `probeTag()` / `newProbe()` helpers.
    companion object : CustomElementReferenceAdapter<AttributeProbe>(
        AttributeProbe::class.js,
        "ktjs-attr-probe",
        observedAttributes = AttributeProbeAttributes.ALL,
    ) {

        private val kindCodec: AttributeCodec<ProbeKind> = enumAttributeCodec(ProbeKind.entries)
    }
}
