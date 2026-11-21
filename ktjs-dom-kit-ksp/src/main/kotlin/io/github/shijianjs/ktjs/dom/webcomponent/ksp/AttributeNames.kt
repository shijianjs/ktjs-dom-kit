package io.github.shijianjs.ktjs.dom.webcomponent.ksp

/**
 * The kebab-case rule the runtime applies to a property name, kept here as well.
 *
 * `io.github.shijianjs.ktjs.dom.webcomponent.attr.toAttributeName` is `internal` to the library and
 * lives in a Kotlin/JS module, which a JVM processor cannot use; `AttributeNameTest` on the library
 * side and `AttributeNamesTest` here pin the same cases so the two copies cannot drift silently.
 *
 * A dash goes before an upper-case letter that starts a word — after a lower-case letter, or in a
 * run of capitals that is followed by a lower-case one (`XMLHttp` → `xml-http`) — and everything is
 * lower-cased. Digits and existing dashes are left alone.
 */
internal fun attributeNameOf(propertyName: String): String {
    val name = StringBuilder(propertyName.length + 4)
    for (index in propertyName.indices) {
        val char = propertyName[index]
        if (!char.isUpperCase()) {
            name.append(char)
            continue
        }

        val previous = propertyName.getOrNull(index - 1)
        val next = propertyName.getOrNull(index + 1)
        val startsWord = previous != null && (!previous.isUpperCase() || (next != null && next.isLowerCase()))
        if (startsWord) name.append('-')
        name.append(char.lowercaseChar())
    }
    return name.toString()
}

/** `maxCount` → `MAX_COUNT`, `XMLHttp` → `XML_HTTP`: the generated constant that holds an attribute name. */
internal fun constantNameOf(propertyName: String): String =
    attributeNameOf(propertyName).replace('-', '_').uppercase()

/**
 * The tag of an element class whose name already carries the prefix, or the prefix followed by its
 * kebab case.
 *
 * Prefixing unconditionally doubles the mark both families actually use: `DemoCounter` and `KtjsHero`
 * are named after the prefix to begin with, so `demo-` + `demo-counter` would be the tag of neither.
 * A class whose name does not start that way — `StepBadge` in a `demo-` module — gets the prefix, which
 * is the case the option exists for.
 */
internal fun tagOf(prefix: String, className: String): String {
    val kebab = attributeNameOf(className)
    return if (prefix.isEmpty() || kebab.startsWith(prefix)) kebab else prefix + kebab
}
