package io.github.shijianjs.ktjs.dom.demo.page.attribute

import io.github.shijianjs.ktjs.dom.element.invoke

internal object Densities {
    val compact: Density = Density("compact")
    val comfortable: Density = Density("comfortable")
    val all: List<Density> = listOf(compact, comfortable)
}
