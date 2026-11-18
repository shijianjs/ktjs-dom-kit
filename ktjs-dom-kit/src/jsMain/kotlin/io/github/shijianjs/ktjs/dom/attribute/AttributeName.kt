package io.github.shijianjs.ktjs.dom.attribute

/**
 * `maxCount` → `max-count`: the attribute name a property gets when `attr` is not told one.
 *
 * A dash goes before an upper-case letter that starts a word — after a lower-case letter, or in a
 * run of capitals that is followed by a lower-case one (`XMLHttp` → `xml-http`) — and everything is
 * lower-cased. Digits and existing dashes are left alone.
 */
internal fun String.toAttributeName(): String {
    val name = StringBuilder(length + 4)
    for (index in indices) {
        val char = this[index]
        if (!char.isUpperCase()) {
            name.append(char)
            continue
        }

        val previous = getOrNull(index - 1)
        val next = getOrNull(index + 1)
        val startsWord = previous != null && (!previous.isUpperCase() || (next != null && next.isLowerCase()))
        if (startsWord) name.append('-')
        name.append(char.lowercaseChar())
    }
    return name.toString()
}
