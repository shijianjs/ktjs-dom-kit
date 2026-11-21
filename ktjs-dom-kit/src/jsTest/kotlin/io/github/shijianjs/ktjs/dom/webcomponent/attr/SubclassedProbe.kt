package io.github.shijianjs.ktjs.dom.webcomponent.attr

import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter

/**
 * A base element that is only ever subclassed: no companion object, so no tag of its own — and still a
 * source of attributes, because [SubclassedProbeChild]'s generated tables merge them in.
 *
 * This is the shape a hierarchy of near-identical elements takes: the base holds the declarations,
 * the leaves differ in tag, markup and a declaration or two. The processor still writes
 * `SubclassedProbeBaseDefaultReference` for it — that file is what the child's generated code names —
 * but the class appears in no registry, because a registry entry *is* an element's companion object
 * and this class has none.
 */
internal open class SubclassedProbeBase : CustomElementAdapter() {

    var caption: String by attr(default = "base")

}

/** The leaf: a tag, one more attribute, and everything the base declared. */
internal class SubclassedProbeChild : SubclassedProbeBase() {

    var extra: Int by attr(default = 3)

    companion object : SubclassedProbeChildDefaultReference()
}
