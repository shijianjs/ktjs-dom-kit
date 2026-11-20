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

    var label: String by attr("upgrade-label") { changes.add(it) }

    companion object : CustomElementReferenceAdapter<UpgradeProbe>(
        UpgradeProbe::class.js,
        "ktjs-attr-upgrade-probe",
        observedAttributes = arrayOf("upgrade-label"),
        register = false,
    )
}

/**
 * A custom element with one delegate per feature under test.
 *
 * Every callback appends to [changes], so a test can assert what was reported and in which order.
 * Two callbacks do more than record: `mirror` writes another attribute, `echo` writes itself back.
 */
internal class AttributeProbe : CustomElementAdapter() {

    val changes: MutableList<AttributeChange<*>> = mutableListOf()

    var text: String by attr { changes.add(it) }

    var count: Int by attr { changes.add(it) }

    @JsName("maxCount")
    var maxCount: Int by attr(default = 10) { changes.add(it) }

    var ratio: Double by attr(default = 1.5) { changes.add(it) }

    @JsName("flagged")
    var flagged: Boolean by attr { changes.add(it) }

    var explicit: String by attr("data-explicit") { changes.add(it) }

    @JsName("publicName")
    var internalName: String by attr { changes.add(it) }

    @JsName("kind")
    var kind: ProbeKind by attr(codec = kindCodec, default = ProbeKind.Primary) { changes.add(it) }

    var second: Int by attr("second", default = 5) { changes.add(it) }

    var mirror: String by attr("mirror") { change ->
        changes.add(change)
        if (change.newValue.isNotEmpty()) second = change.newValue.length
    }

    var echo: String by attr("echo") { change ->
        changes.add(change)
        // Writing the same value back must not re-enter this delegate.
        echo = change.newValue
    }

    override fun connectedCallback() {
        applyInitialAttributes()
    }

    // Registering is the companion's `init`: `AttributeProbe.NAME` defines the tag on first read, and
    // `AttributeProbe.NAME()` creates a detached probe — no `probeTag()` / `newProbe()` helpers.
    //
    // `observedAttributes` repeats the names the properties above stand for. The browser reads it
    // once, at `define`, and only those attributes reach `attributeChangedCallback`; this list and
    // the `by attr` declarations are the same fact written twice until a generated one replaces it.
    companion object : CustomElementReferenceAdapter<AttributeProbe>(
        AttributeProbe::class.js,
        "ktjs-attr-probe",
        observedAttributes = arrayOf(
            "text",
            "count",
            "max-count",
            "ratio",
            "flagged",
            "data-explicit",
            "internal-name",
            "kind",
            "second",
            "mirror",
            "echo",
        ),
    ) {

        private val kindCodec: AttributeCodec<ProbeKind> = enumAttributeCodec(ProbeKind.entries)
    }
}