package io.github.shijianjs.ktjs.dom.element

import web.cssom.ClassName
import web.dom.document
import web.html.HTMLElement
import web.html.HtmlTagName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private val CARD: ClassName = ClassName("card")

class CreateElementTest {
    @Test
    fun createElementCarriesTheTagsElementType() {
        val anchor = HtmlTagName.a.createElement {
            href = "/docs/intro"
            textContent = "Get started"
        }
        assertEquals("a", anchor.tagName.lowercase())
        assertEquals("/docs/intro", anchor.getAttribute("href"))
        assertEquals("Get started", anchor.textContent)
    }

    @Test
    fun anEmptyBlockCreatesABareElement() {
        val div = HtmlTagName.div.createElement()
        assertEquals("div", div.tagName.lowercase())
        assertNull(div.firstElementChild)
        assertEquals("", div.textContent)
    }

    @Test
    fun createElementCanBuildInsideAnotherDocument() {
        val other = document.implementation.createHTMLDocument("")
        val span = HtmlTagName.span.createElement(other)
        assertSame(other, span.ownerDocument)
    }

    @Test
    fun invokeIsCreateElementSpelledOnTheTag() {
        val section = HtmlTagName.section {
            classList.add(CARD)
            textContent = "Body"
        }
        assertEquals("section", section.tagName.lowercase())
        assertEquals("Body", section.textContent)
        assertTrue(section.classList.contains(CARD))
    }

    @Test
    fun unaryPlusAppendsToTheEnclosingElementAndReturnsTheChild() {
        lateinit var heading: HTMLElement
        val card = HtmlTagName.div {
            heading = +HtmlTagName.h2 { textContent = "Title" }
            assertEquals(1, childElementCount)
            assertSame(this, heading.parentNode)
        }
        assertEquals("h2", heading.tagName.lowercase())
        assertEquals("Title", heading.textContent)
        assertEquals(1, card.childElementCount)
        assertSame(card, heading.parentNode)
    }

    @Test
    fun unaryPlusKeepsInsertionOrder() {
        val list = HtmlTagName.ul {
            +HtmlTagName.li { textContent = "one" }
            +HtmlTagName.li { textContent = "two" }
            +HtmlTagName.li { textContent = "three" }
        }
        assertEquals(3, list.childElementCount)
        assertEquals("one", list.children.item(0)?.textContent)
        assertEquals("two", list.children.item(1)?.textContent)
        assertEquals("three", list.children.item(2)?.textContent)
    }

    @Test
    fun unaryPlusNestsEachLevelUnderItsOwnParent() {
        val card = HtmlTagName.div {
            +HtmlTagName.section {
                +HtmlTagName.h3 { textContent = "Nested" }
            }
        }
        val section = card.firstElementChild
        assertNotNull(section)
        assertEquals("section", section.tagName.lowercase())
        assertSame(card, section.parentNode)
        val heading = section.firstElementChild
        assertNotNull(heading)
        assertEquals("Nested", heading.textContent)
        assertSame(section, heading.parentNode)
    }
}
