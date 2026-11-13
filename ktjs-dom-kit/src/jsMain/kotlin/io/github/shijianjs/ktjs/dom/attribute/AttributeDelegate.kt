package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The delegate behind one `attr` property.
 *
 * It keeps the last value it reported only to tell changes apart from repeated writes; the value a
 * getter returns always comes from the DOM.
 */
internal class AttributeDelegate<T>(
    private val element: CustomElementAdapter,
    private val name: String,
    private val default: T?,
    private val codec: AttributeCodec<T>,
    private val onChange: (AttributeChange<T>) -> Unit,
) : ReadWriteProperty<CustomElementAdapter, T> {

    private var lastRaw: String? = null
    private var lastValue: T? = null
    private var reported: Boolean = false
    private var notifying: Boolean = false

    override fun getValue(thisRef: CustomElementAdapter, property: KProperty<*>): T =
        valueOf(thisRef.getAttribute(name))

    override fun setValue(thisRef: CustomElementAdapter, property: KProperty<*>, value: T) {
        val encoded = encodeAssigned(value)
        if (encoded == null) thisRef.removeAttribute(name) else thisRef.setAttribute(name, encoded)
        sync()
    }

    /**
     * The attribute value for a value the setter was handed.
     *
     * Kotlin can only pass a real [T], but JavaScript passes whatever it has, and does: a custom
     * element's property is what React 19 assigns every matching JSX prop to, so
     * `<ktjs-action kind="secondary">` arrives here as the **string** `"secondary"` even though the
     * property's type is the enum. A string is therefore taken as the attribute's raw form — the text
     * `setAttribute` would have stored — and only a value of the property's own type goes through
     * [codec]. For `T = String` the two are the same thing, since that codec is the identity.
     */
    private fun encodeAssigned(value: T): String? =
        if (value is String) value else codec.encode(value)

    /**
     * Reads the attribute again and reports it when the value differs from the last report.
     *
     * This is the only path that calls [onChange] — setter, attribute observer and
     * [CustomElementAdapter.applyInitialAttributes] all end up here — so the callback cannot be
     * invoked twice for one change, and nothing here writes the attribute back.
     */
    internal fun sync() {
        // A callback that writes its own property would otherwise re-enter in the middle of a report.
        if (notifying) return

        val raw = element.getAttribute(name)
        val value = valueOf(raw)
        if (reported && lastValue == value) {
            lastRaw = raw
            return
        }

        val change = AttributeChange(
            name = name,
            oldValue = if (reported) lastValue ?: value else value,
            newValue = value,
            oldRaw = if (reported) lastRaw else raw,
            newRaw = raw,
        )
        lastRaw = raw
        lastValue = value
        reported = true

        notifying = true
        try {
            onChange(change)
        } finally {
            notifying = false
        }
    }

    /** Writes [default] to a missing attribute; used by `applyInitialAttributes(writeDefaults = true)`. */
    internal fun writeDefault() {
        val value = default ?: return
        if (element.getAttribute(name) != null) return
        val encoded = codec.encode(value)
        if (encoded == null) element.removeAttribute(name) else element.setAttribute(name, encoded)
    }

    private fun valueOf(raw: String?): T = when (raw) {
        null -> default ?: codec.absent
        else -> codec.decode(raw) ?: default ?: codec.absent
    }
}
