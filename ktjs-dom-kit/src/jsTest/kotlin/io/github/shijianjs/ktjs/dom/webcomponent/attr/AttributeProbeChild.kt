package io.github.shijianjs.ktjs.dom.webcomponent.attr

/**
 * A custom element built on another one, to check what the processor does with inheritance: a tag is
 * its own `customElements.define`, and the browser reads its `observedAttributes` from
 * `AttributeProbeChild`, not from the class it extends — so the parent's eleven attributes have to be
 * listed again. They are not written again: the processor merges the parent's generated `ALL` and
 * `ATTRIBUTES` into this class's, and a child that adds one attribute ends up with twelve.
 *
 * The tag follows the class name (`ktjs-attribute-probe-child`), the reference is a one-liner, and
 * `extra` is reached from the markup exactly like a parent's attribute is.
 */
internal class AttributeProbeChild : AttributeProbe() {

    var extra: Int by attr(default = 7) { changes.add(it) }

    companion object : AttributeProbeChildDefaultReference()
}
