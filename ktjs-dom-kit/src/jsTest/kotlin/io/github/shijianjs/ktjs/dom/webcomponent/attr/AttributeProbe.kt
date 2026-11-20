package io.github.shijianjs.ktjs.dom.webcomponent.attr

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.enumAttributeCodec
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementReferenceAdapter
import kotlin.js.JsName

/** Enum for the custom-codec case; `kind` in the markup is the lower-cased constant name. */
internal enum class ProbeKind { Primary, Secondary }

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
    companion object : CustomElementReferenceAdapter<AttributeProbe>(AttributeProbe::class.js, "ktjs-attr-probe") {

        private val kindCodec: AttributeCodec<ProbeKind> = enumAttributeCodec(ProbeKind.entries)
    }
}