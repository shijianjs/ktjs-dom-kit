package io.github.shijianjs.ktjs.dom.webcomponent.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.File

private const val ADAPTER = "io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter"
private const val ATTRIBUTE_NAME_ANNOTATION = "AttributeName"

/** The property the element hands to `observedAttributes`. */
private const val ALL = "ALL"

/**
 * Derives the observed attribute names of every custom element in the compilation and writes them
 * out as an `object` next to the class.
 *
 * The browser reads `observedAttributes` once, at `customElements.define`, so the list has to exist
 * before the companion's `init` — before any instance and long before a delegate. That rules out
 * collecting the names from the delegates themselves (they are created per instance, in the
 * constructor). A Kotlin/JS class has no static initializer that could see its properties either.
 * The compiler is the only place that knows the declarations before anything runs, which is why this
 * processor exists: for every class extending `CustomElementAdapter` it generates
 *
 * ```kotlin
 * internal object KtjsHeroAttributes {
 *     const val LOGO_SRC: String = "logo-src"
 *     val ALL: Array<String> = arrayOf(LOGO_SRC)
 * }
 * ```
 *
 * The element then uses the constants twice, never the literal:
 *
 * ```kotlin
 * var logoSrc: String by attr(KtjsHeroAttributes.LOGO_SRC)
 *
 * companion object : CustomElementReferenceAdapter<KtjsHero>(
 *     KtjsHero::class.js, "ktjs-hero",
 *     observedAttributes = KtjsHeroAttributes.ALL,
 * )
 * ```
 *
 * A name comes from `@AttributeName("…")` on the property when the attribute is not the property
 * name in kebab case; otherwise it is that kebab case, the same rule `attr` applies at runtime.
 * Which properties are attributes at all is decided by `isDelegated()`: inside a
 * `CustomElementAdapter` the only delegated properties are `attr` ones.
 */
internal class AttributeNamesProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    /** Qualified names already written in this compilation; a class must produce one file, not one per round. */
    private val generated: MutableSet<String> = mutableSetOf()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (file in resolver.getAllFiles().toList()) {
            // A second round sees the files this processor just wrote; their symbols are already
            // covered, and generating them again would collide on the file name.
            if (file.filePath.contains("${File.separator}build${File.separator}generated${File.separator}")) continue
            for (declaration in file.declarations) {
                visit(declaration, file)
            }
        }
        return emptyList()
    }

    private fun visit(declaration: KSDeclaration, file: KSFile) {
        if (declaration !is KSClassDeclaration) return
        if (declaration.isCompanionObject) return

        if (declaration.extendsAdapter()) generate(declaration, file)

        declaration.declarations.forEach { visit(it, file) }
    }

    /** Whether this class — directly or through a base class in the same compilation — is an adapter. */
    private fun KSClassDeclaration.extendsAdapter(depth: Int = 0): Boolean {
        if (depth > 8) return false
        return superTypes.any { reference ->
            val superDeclaration = reference.resolve().declaration
            when {
                superDeclaration.qualifiedName?.asString() == ADAPTER -> true
                superDeclaration is KSClassDeclaration -> superDeclaration.extendsAdapter(depth + 1)
                else -> false
            }
        }
    }

    private fun generate(declaration: KSClassDeclaration, file: KSFile) {
        val properties = declaration.declarations
            .filterIsInstance<KSPropertyDeclaration>()
            .filter { it.isDelegated() }
            .toList()
        if (properties.isEmpty()) return

        val qualifiedName = declaration.qualifiedName?.asString()
        if (qualifiedName == null) {
            logger.error("A custom element has no qualified name; its attribute names cannot be generated.", declaration)
            return
        }
        if (!generated.add(qualifiedName)) return

        val packageName = declaration.packageName.asString()
        val objectName = "${declaration.simpleName.asString()}Attributes"
        val names = properties.map { property ->
            val explicit = property.annotations
                .firstOrNull { it.shortName.asString() == ATTRIBUTE_NAME_ANNOTATION }
                ?.arguments
                ?.firstOrNull()
                ?.value as? String
            val propertyName = property.simpleName.asString()
            AttributeName(propertyName, explicit ?: attributeNameOf(propertyName))
        }
        val duplicates = names.groupBy { it.attribute }.filterValues { it.size > 1 }
        if (duplicates.isNotEmpty()) {
            logger.error(
                "Two properties of '$qualifiedName' map to the same attribute: " +
                    duplicates.entries.joinToString { (attribute, owners) ->
                        "'$attribute' (${owners.joinToString { it.property }})"
                    } + ". Give one of them an explicit @AttributeName.",
                declaration,
            )
            return
        }

        val spec = fileFor(packageName, objectName, names)
        val dependencies = Dependencies(aggregating = false, file)
        spec.writeTo(codeGenerator, dependencies)
    }

    private fun fileFor(packageName: String, objectName: String, names: List<AttributeName>): FileSpec {
        val builder = TypeSpec.objectBuilder(objectName)
            .addModifiers(KModifier.INTERNAL)
            .addKdoc(
                "The attribute names of [%L], generated from its delegated properties.\n\n" +
                    "Use the constants for the `attr(…)` call and [ALL] for `observedAttributes`; " +
                    "the literal then exists in exactly one place.\n",
                objectName.removeSuffix("Attributes"),
            )

        for (name in names) {
            builder.addProperty(
                PropertySpec.builder(constantNameOf(name.property), STRING)
                    .addModifiers(KModifier.CONST)
                    .initializer("%S", name.attribute)
                    .build()
            )
        }

        builder.addProperty(
            PropertySpec.builder(ALL, ARRAY.parameterizedBy(STRING))
                .initializer(CodeBlock.of("arrayOf(%L)", names.joinToString(", ") { constantNameOf(it.property) }))
                .build()
        )

        return FileSpec.builder(packageName, objectName)
            .addFileComment("Generated by ktjs-dom-kit-ksp from the `attr` declarations. Do not edit.")
            .addType(builder.build())
            .build()
    }

    private data class AttributeName(val property: String, val attribute: String)

    private companion object {
        val ARRAY: ClassName = ClassName("kotlin", "Array")
    }
}
