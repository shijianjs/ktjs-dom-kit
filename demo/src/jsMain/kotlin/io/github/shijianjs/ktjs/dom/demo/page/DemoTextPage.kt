package io.github.shijianjs.ktjs.dom.demo.page

import io.github.shijianjs.ktjs.demo.Bilingual
import io.github.shijianjs.ktjs.demo.DemoPage
import io.github.shijianjs.ktjs.demo.Lang
import io.github.shijianjs.ktjs.demo.Page
import io.github.shijianjs.ktjs.demo.newPage
import web.html.HTMLElement

/**
 * Every node one page has to re-label when the language switches.
 *
 * A page registers the nodes it owns once, in its `init`, and [apply] rewrites their `textContent`
 * in place. Nothing is rebuilt, so scroll position, typed input and opened panels survive the
 * switch — which is exactly what the `Page` contract asks for.
 */
internal class TextBag {
    private val entries: MutableList<Pair<HTMLElement, Bilingual>> = mutableListOf()

    /** Registers [node] for [text] and returns it, so a call can sit inside a constructor chain. */
    fun <T : HTMLElement> bind(node: T, text: Bilingual): T {
        entries += node to text
        return node
    }

    /** Rewrites every registered node to [lang]. */
    fun apply(lang: Lang) {
        entries.forEach { (node, text) -> node.textContent = text.resolve(lang) }
    }
}

/**
 * The frame shared by the pages of this site: a [DemoPage] plus the title and summary it has to
 * translate.
 *
 * This is a base class because every page of the site *is* a page — title and summary are what makes
 * it one. The section shells a page composes are containers and stay outside the hierarchy.
 */
internal abstract class DemoTextPage(
    private val title: Bilingual,
    private val summary: Bilingual,
) : Page {
    val page: DemoPage = newPage()

    override val node: HTMLElement get() = page

    protected val texts: TextBag = TextBag()

    override fun applyTranslation(lang: Lang) {
        page.setTitle(title.resolve(lang))
        page.setSummary(summary.resolve(lang))
        texts.apply(lang)
    }
}
