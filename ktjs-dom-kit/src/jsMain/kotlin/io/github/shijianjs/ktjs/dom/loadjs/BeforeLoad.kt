package io.github.shijianjs.ktjs.dom.loadjs

import web.html.HTMLElement

/**
 * Upstream's `before` hook: called for every file right before its element is inserted, with the path
 * as written and the `<script>` / `<link>` / `<img>` that was created for it — the place to set
 * attributes such as `crossOrigin`.
 *
 * Returning `false` skips the loader's own insertion into `<head>` (upstream's "bypass the default
 * DOM insertion mechanism"); every other value, `null` included, leaves the insertion to the loader.
 * An element that is not inserted fires no `load` / `error` event, so `Loadjs(…)` never returns — the
 * caller owns that element and its events from then on.
 */
typealias BeforeLoad = (path: String, element: HTMLElement) -> Boolean?
