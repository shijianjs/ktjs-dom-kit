package io.github.shijianjs.ktjs.demo.nav

class Entry(
    val id: String,
    val group: Group,
    val icon: String? = null,
    val create: () -> Page,
)
