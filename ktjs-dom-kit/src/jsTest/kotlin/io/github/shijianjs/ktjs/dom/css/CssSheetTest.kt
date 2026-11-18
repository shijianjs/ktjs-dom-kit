package io.github.shijianjs.ktjs.dom.css

import csstype.fallback
import io.github.shijianjs.ktjs.dom.css.dsl.Pseudo
import io.github.shijianjs.ktjs.dom.css.dsl.Pseudos
import io.github.shijianjs.ktjs.dom.css.dsl.RadialGradientShape
import io.github.shijianjs.ktjs.dom.css.dsl.pseudo
import io.github.shijianjs.ktjs.dom.css.dsl.pseudos
import io.github.shijianjs.ktjs.dom.css.dsl.colorStop
import io.github.shijianjs.ktjs.dom.css.dsl.and as supportsAnd
import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.css.dsl.not as supportsNot
import io.github.shijianjs.ktjs.dom.css.dsl.or as supportsOr
import io.github.shijianjs.ktjs.dom.css.dsl.radialGradient
import io.github.shijianjs.ktjs.dom.css.dsl.supports
import io.github.shijianjs.ktjs.dom.css.dsl.supportsDeclaration
import io.github.shijianjs.ktjs.dom.css.dsl.webkitBackgroundClip
import io.github.shijianjs.ktjs.dom.css.dsl.webkitBackgroundClipName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import web.cssom.BackgroundClip
import web.cssom.ClassName
import web.cssom.Color
import web.cssom.Display
import web.cssom.NamedColor
import web.cssom.None
import web.cssom.Position
import web.cssom.PropertyName
import web.cssom.and
import web.cssom.atrule.MediaType
import web.cssom.atrule.maxWidth
import web.cssom.many
import web.cssom.pct
import web.cssom.px
import web.cssom.stop
import web.cssom.`var`
import web.cssom.CustomPropertyName

/**
 * The serializer contract: what goes into the rules object, and what has to come out of it.
 *
 * These run in Chromium because the package that does the work (`@emotion/serialize` + stylis) is
 * resolved by webpack, exactly as it is in the site bundle.
 */
class CssSheetTest {

    private val cardClass = ClassName("card")

    @Test
    fun nestedSelectorsAreResolvedAgainstTheirParent() {
        val text = cssText {
            cardClass {
                display = Display.block
                pseudo(Pseudos.hover) { display = fallback(Display.grid, Display.flex) }
            }
        }

        assertEquals(".card{display:block;}.card:hover{display:grid;display:flex;}", text)
    }

    @Test
    fun atRulesUseTheWrappersOwnKeywordsAndQueries() {
        val text = cssText {
            cardClass {
                pseudo(Pseudos.before) { content = web.cssom.Content() }
            }
            // csstype.RuleBuilder.@media + cssom's typed media query pieces; no "@media" literal.
            `@media`(MediaType.screen and maxWidth(576.px)) {
                cardClass { display = Display.block }
            }
            // SupportsQuery mirrors MediaQuery: typed declarations, typed composition.
            supports(
                supportsDeclaration(webkitBackgroundClipName, BackgroundClip.text) supportsOr
                    supportsDeclaration(PropertyName.backgroundClip, BackgroundClip.text),
            ) {
                cardClass { backgroundClip = BackgroundClip.text }
            }
        }

        assertTrue(text.contains(".card::before{content:'';}"), text)
        assertTrue(text.contains("@media screen and (max-width:576px){.card{display:block;}}"), text)
        assertTrue(
            text.contains(
                "@supports (-webkit-background-clip:text) or (background-clip:text)" +
                    "{.card{background-clip:text;}}",
            ),
            text,
        )
    }

    @Test
    fun pseudoValuesInvokeLikeTheWrappersOwnHelpers() {
        val text = cssText {
            cardClass {
                Pseudos.hover { display = Display.block }
                // Parentheses: the trailing lambda has to bind to Pseudo.invoke, not to many's vararg.
                (many(Pseudos.hover, Pseudos.focusVisible)) { display = Display.flex }
            }
        }

        assertTrue(text.contains(".card:hover{display:block;}"), text)
        assertTrue(text.contains(".card:hover,.card:focus-visible{display:flex;}"), text)
    }

    @Test
    fun severalStatesShareOneBlock() {
        val text = cssText {
            cardClass {
                pseudos(Pseudos.hover, Pseudos.focusVisible) { display = Display.block }
            }
        }

        assertEquals(".card:hover,.card:focus-visible{display:block;}", text)
    }

    @Test
    fun manyJoinsPseudoVariantsAsWellAsSelectors() {
        val variant: Pseudo = many(Pseudos.hover, Pseudos.focusVisible)
        val text = cssText {
            cardClass {
                pseudo(variant) { display = Display.block }
            }
        }

        // The variant is the pseudo alone; `&` is added when it is attached to a rule, not stored in it.
        assertEquals(":hover,:focus-visible", "$variant")
        assertEquals(".card:hover,.card:focus-visible{display:block;}", text)
    }

    @Test
    fun supportsConditionsComposeLikeMediaQueries() {
        val query = supportsDeclaration(PropertyName.backgroundClip, BackgroundClip.text) supportsAnd
            supportsNot(
                supportsDeclaration(PropertyName.backgroundClip, BackgroundClip.borderBox),
            )

        val text = cssText {
            supports(query) { cardClass { display = Display.block } }
        }

        assertEquals(
            "@supports (background-clip:text) and not (background-clip:border-box)" +
                "{.card{display:block;}}",
            text,
        )
    }

    @Test
    fun sheetsCarryTheSameRulesTheySerializeTo() {
        val sheet = cssSheet {
            cardClass {
                display = Display.block
                "&:focus-visible" { display = Display.block }
            }
        }

        assertEquals(2, sheet.cssRules.length)
        assertTrue(sheet.cssRules.item(0)?.cssText?.startsWith(".card {") == true)
        assertTrue(sheet.cssRules.item(1)?.cssText?.startsWith(".card:focus-visible") == true)
    }

    @Test
    fun oneClassIsOneTopLevelKey() {
        // A rules object is a JSON object: writing the same `ClassName` twice keeps only the last
        // block, so a class's rules belong inside a single block (nested keys, not repeated blocks).
        val text = cssText {
            cardClass { display = Display.block }
            cardClass { display = Display.flex }
        }

        assertEquals(".card{display:flex;}", text)
    }

    @Test
    fun theCssPackageFillsInWhatCssomMisses() {
        val text = cssText {
            cardClass {
                position = Position.absolute
                // cssom types `pointer-events` as `PointerEvents?`, whose companions are the SVG
                // keywords; `none` is typed separately as `web.cssom.None`, a subtype of both
                // `NoneType` and `PointerEvents`.
                pointerEvents = None.none
                background = radialGradient(
                    RadialGradientShape.Circle,
                    colorStop(`var`(CustomPropertyName<Color>("--glow"), NamedColor.black)),
                    stop(Color.transparent, 70.pct),
                )
                backgroundClip = BackgroundClip.text
                webkitBackgroundClip(BackgroundClip.text)
            }
        }

        assertTrue(text.contains("pointer-events:none;"), text)
        assertTrue(text.contains("background:radial-gradient(circle,var(--glow,#000000),transparent 70%);"), text)
        // `webkitBackgroundClip` writes the camelCase `WebkitBackgroundClip` key, which
        // `@emotion/serialize` hyphenates to `-webkit-…`; a kebab-case key is rejected in development.
        assertTrue(text.contains("-webkit-background-clip:text;"), text)
        assertTrue(text.contains("background-clip:text;"), text)
    }

    @Test
    fun theTopLevelIsTheSelector() {
        val text = cssText {
            cardClass { display = Display.block }
        }

        assertEquals(".card{display:block;}", text)
    }
}
