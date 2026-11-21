package io.github.shijianjs.ktjs.dom.webcomponent.attr

import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import kotlin.js.JsName

/** Enum for the generated-codec case; `kind` in the markup is the lower-cased constant name. */
internal enum class ProbeKind { Primary, Secondary }

/**
 * An element whose tag is defined *after* the markup exists, to check the upgrade path: the parser
 * leaves the tag as an unknown element, and `customElements.define` reports the attributes it was
 * carrying. `register = false` on the generated reference keeps its `init` from defining the tag, so a
 * test decides when that happens; `name` is passed because this class is not named after its tag.
 */
internal class UpgradeProbe : CustomElementAdapter() {

    val changes: MutableList<AttributeChange<*>> = mutableListOf()

    @AttributeName("upgrade-label")
    var label: String by attr { changes.add(it) }

    companion object : UpgradeProbeDefaultReference(name = "ktjs-attr-upgrade-probe", register = false)
}

/**
 * A custom element with one delegate per feature under test.
 *
 * Every callback appends to [changes], so a test can assert what was reported and in which order.
 * Two callbacks do more than record: `mirror` writes another attribute, `echo` writes itself back.
 *
 * Neither class names an attribute in code. `ktjs-dom-kit-ksp` generates the `…DefaultReference` the
 * companion extends — with the tag, the `observedAttributes` array, and the `ATTRIBUTES` tables
 * (`names` and, for the enum property, `codecs`) — so `attr` with no arguments still finds the right
 * attribute, including `explicit` → `data-explicit` and `kind` → `enumAttributeCodec`.
 */
internal open class AttributeProbe : CustomElementAdapter() {

    val changes: MutableList<AttributeChange<*>> = mutableListOf()

    var text: String by attr { changes.add(it) }

    var count: Int by attr { changes.add(it) }

    @JsName("maxCount")
    var maxCount: Int by attr(default = 10) { changes.add(it) }

    var ratio: Double by attr(default = 1.5) { changes.add(it) }

    @JsName("flagged")
    var flagged: Boolean by attr { changes.add(it) }

    @AttributeName("data-explicit")
    var explicit: String by attr { changes.add(it) }

    @JsName("publicName")
    var internalName: String by attr { changes.add(it) }

    @JsName("kind")
    var kind: ProbeKind by attr(default = ProbeKind.Primary) { changes.add(it) }

    var second: Int by attr(default = 5) { changes.add(it) }

    var mirror: String by attr { change ->
        changes.add(change)
        if (change.newValue.isNotEmpty()) second = change.newValue.length
    }

    var echo: String by attr { change ->
        changes.add(change)
        // Writing the same value back must not re-enter this delegate.
        echo = change.newValue
    }

    // Registering is the companion's `init`: `AttributeProbe.NAME` defines the tag on first read, and
    // `AttributeProbe.NAME()` creates a detached probe — no `probeTag()` / `newProbe()` helpers.
    companion object : AttributeProbeDefaultReference(name = "ktjs-attr-probe")
}
