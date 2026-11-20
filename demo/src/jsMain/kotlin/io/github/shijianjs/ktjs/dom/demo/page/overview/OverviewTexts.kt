package io.github.shijianjs.ktjs.dom.demo.page.overview

import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus

/** Every string this page shows, in one language. */
internal class OverviewTexts(
    val title: String,
    val summary: String,
    val elementsLink: String,
    val customElementsLink: String,
    val apiLink: String,
    val purposeHeading: String,
    val body: String,
    val bullets: List<String>,
    val coordinatesHeading: String,
    val coordinatesNote: String,
    val sourceLabel: String,
    val nextHeading: String,
    val nextBody: String,
)
