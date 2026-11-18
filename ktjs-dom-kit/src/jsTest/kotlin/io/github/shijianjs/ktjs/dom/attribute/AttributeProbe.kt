package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.attribute.types.enumAttributeCodec
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import kotlin.js.JsName
import web.components.CustomElementReference
import web.dom.TagName

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

    companion object : CustomElementReference<AttributeProbe>(AttributeProbe::class.js) {
        val NAME: TagName<AttributeProbe> = TagName("ktjs-attr-probe")

        private val kindCodec: AttributeCodec<ProbeKind> = enumAttributeCodec(ProbeKind.entries)
    }
}

/** The probe's tag, defined once; `defineIfMissing` makes repeated calls harmless. */
internal fun probeTag(): TagName<AttributeProbe> {
    defineIfMissing(AttributeProbe.NAME, AttributeProbe.Companion)
    return AttributeProbe.NAME
}

/** A detached probe: attributes can be set and read without the element being in the document. */
internal fun newProbe(): AttributeProbe = probeTag().createElement()
