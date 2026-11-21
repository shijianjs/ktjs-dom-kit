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

/** `kind` → `KIND_CODEC`: the generated codec of an enum-typed property, next to its attribute constant. */
internal fun codecNameOf(propertyName: String): String = constantNameOf(propertyName) + "_CODEC"

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

/**
 * Where the element registry goes when the `ktjs.registryPackage` option is absent: the deepest
 * package the elements share, so the object sits above them all and `null` when they share nothing
 * (two unrelated top-level packages mean nobody agreed on a place).
 */
internal fun commonPackageOf(packages: List<String>): String? {
    if (packages.isEmpty()) return null
    val common = packages.map { it.split('.') }.reduce { left, right ->
        left.zip(right).takeWhile { (one, other) -> one == other }.map { it.first }
    }
    return if (common.isEmpty()) null else common.joinToString(".")
}

/**
 * The registry object's name, derived from the tag prefix: `demo-` → `DemoCustomElements`, `ktjs-` →
 * `KtjsCustomElements`, no prefix at all → `CustomElements`.
 *
 * One object per compilation, named after the family of tags it lists, so two modules of one site can
 * both be imported without colliding.
 */
internal fun registryNameOf(tagPrefix: String): String {
    val prefix = tagPrefix.split('-')
        .filter { it.isNotEmpty() }
        .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    return prefix + "CustomElements"
}
