package demo.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.junit.UsePlaywright
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The generated references, checked through the site's own elements.
 *
 * Every element in this demo extends `…DefaultReference()` and names no attribute in code, so this is
 * the end-to-end proof that the generated tag, the generated `observedAttributes` and the generated
 * `BY_PROPERTY` table all land where the runtime expects them: the tag is the one the markup and the
 * router use, the list is the one the browser reads, and an external `setAttribute` — the case a
 * synchronous `attributeChangedCallback` misses when a name is not registered — repaints the element.
 *
 * Elements are found by walking shadow roots: a page's content sits in its own shadow, a widget's in
 * the widget's, so a plain `document.querySelector` sees none of them.
 */
@UsePlaywright(DemoOptions::class)
class GeneratedReferenceTest(private val browser: Browser) {

    /** `find('demo-counter')` for an expression body: a depth-first walk of every open shadow root. */
    private val deepFind = """
        const walk = (root, tag) => {
          for (const node of root.querySelectorAll('*')) {
            if (node.tagName.toLowerCase() === tag) return node;
            if (node.shadowRoot) { const hit = walk(node.shadowRoot, tag); if (hit) return hit; }
          }
          return null;
        };
        const find = (tag) => walk(document, tag);
    """.trimIndent()

    @Test
    fun generatedReferencesDriveTheDemoElements() {
        val page = browser.newPage()
        page.onPageError { error -> println("PAGEERROR: $error") }
        page.setDefaultTimeout(7_000.0)

        page.navigate(DemoServer.baseUrl + "#/custom-elements")
        page.waitForTag("demo-counter")
        // The list comes from `DemoCounterAttributes.ALL`, which the processor built from `value` and
        // `incrementLabel`; nothing in the element's own file names either of them.
        assertEquals(
            listOf("value", "increment-label"),
            page.evaluate("() => customElements.get('demo-counter').observedAttributes"),
        )
        assertEquals("0", page.textOf("demo-counter", ".counter__value"))
        // A write from the outside, answered before `setAttribute` returns: no wait, no microtask.
        page.setAttribute("demo-counter", "value", "7")
        assertEquals("7", page.textOf("demo-counter", ".counter__value"))

        // `labelText` stands for `label` through `@AttributeName`, and `BY_PROPERTY` is what tells
        // the delegate that; `step` needs no annotation, the kebab case is the same string.
        page.navigate(DemoServer.baseUrl + "#/stylesheets")
        page.waitForTag("demo-hover-box")
        page.setAttribute("demo-hover-box", "label", "from outside")
        assertEquals("from outside", page.textOf("demo-hover-box", ".hover-box"))

        page.navigate(DemoServer.baseUrl + "#/attributes")
        page.waitForTag("demo-step-badge")
        page.setAttribute("demo-step-badge", "step", "42")
        assertEquals("step = 42", page.textOf("demo-step-badge", ".step-badge"))
    }

    private fun Page.waitForTag(tag: String) {
        val deadline = System.currentTimeMillis() + 7_000
        while (System.currentTimeMillis() < deadline) {
            if (evaluate("() => { $deepFind return !!find('$tag'); }") == true) return
            Thread.sleep(50)
        }
        error("<$tag> never appeared")
    }

    private fun Page.textOf(tag: String, selector: String): String =
        evaluate("() => { $deepFind return find('$tag').shadowRoot.querySelector('$selector').textContent; }") as String

    private fun Page.setAttribute(tag: String, name: String, value: String) {
        evaluate("() => { $deepFind find('$tag').setAttribute('$name', '$value'); }")
    }
}
