package io.github.shijianjs.ktjs.dom.webcomponent.ksp

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.File

private const val ADAPTER = "io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter"
private val REFERENCE_ADAPTER: ClassName =
    ClassName("io.github.shijianjs.ktjs.dom.webcomponent", "CustomElementReferenceAdapter")
private const val ATTRIBUTE_NAME_ANNOTATION = "AttributeName"
private const val TAG_ANNOTATION = "CustomElementTag"

/** The property the element hands to `observedAttributes`. */
private const val ALL = "ALL"

/** The property-name → attribute-name table the `attr` delegations read. */
private const val BY_PROPERTY = "BY_PROPERTY"

/** Default for the `ktjs.tagPrefix` option: every element in this account's libraries is a `ktjs-…` tag. */
private const val DEFAULT_TAG_PREFIX = "ktjs-"

/**
 * Generates, for every class extending `CustomElementAdapter`, the two declarations its element would
 * otherwise write by hand: the attribute names and the reference its companion extends.
 *
 * The browser reads `observedAttributes` once, at `customElements.define`, so the list has to exist
 * before the companion's `init` — before any instance and long before a delegate. That rules out
 * collecting the names from the delegates themselves (they are created per instance, in the
 * constructor). A Kotlin/JS class has no static initializer that could see its properties either.
 * The compiler is the only place that knows the declarations before anything runs, which is why this
 * processor exists.
 *
 * ```kotlin
 * internal object DemoCounterAttributes {
 *     const val VALUE: String = "value"
 *     const val INCREMENT_LABEL: String = "increment-label"
 *     val ALL: Array<String> = arrayOf(VALUE, INCREMENT_LABEL)
 *     val BY_PROPERTY: Map<String, String> = mapOf("value" to VALUE, "incrementLabel" to INCREMENT_LABEL)
 * }
 *
 * internal class DemoCounterDefaultReference(
 *     name: String = "demo-counter",
 *     formAssociated: Boolean? = null,
 *     observedAttributes: ReadonlyArray<JsString>? = DemoCounterAttributes.ALL,
 *     register: Boolean = true,
 *     attributeNames: Map<String, String>? = DemoCounterAttributes.BY_PROPERTY,
 * ) : CustomElementReferenceAdapter<DemoCounter>(…)
 * ```
 *
 * The element then writes neither an attribute name nor a reference:
 *
 * ```kotlin
 * var incrementLabel: String by attr(default = "+")
 *
 * companion object : DemoCounterDefaultReference()
 * ```
 *
 * A name comes from `@AttributeName("…")` when the attribute is not the property name in kebab case;
 * the tag comes from `@CustomElementTag("…")` when it is not the class name in kebab case behind the
 * configured prefix (`ktjs.tagPrefix`, default `ktjs-`). Which properties are attributes at all is
 * decided by `isDelegated()`: inside a `CustomElementAdapter` the only delegated properties are `attr`
 * ones.
 */
internal class AttributeNamesProcessor(
    environment: SymbolProcessorEnvironment,
    private val codeGenerator: CodeGenerator = environment.codeGenerator,
    private val logger: KSPLogger = environment.logger,
    private val tagPrefix: String = environment.options[TAG_PREFIX_OPTION] ?: DEFAULT_TAG_PREFIX,
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
        val qualifiedName = declaration.qualifiedName?.asString()
        if (qualifiedName == null) {
            logger.error("A custom element has no qualified name; its attribute names cannot be generated.", declaration)
            return
        }
        if (!generated.add(qualifiedName)) return

        val properties = declaration.declarations
            .filterIsInstance<KSPropertyDeclaration>()
            .filter { it.isDelegated() }
            .toList()
        val packageName = declaration.packageName.asString()
        val simpleName = declaration.simpleName.asString()
        val elementClass = ClassName(packageName, simpleName)
        val names = properties.map { property ->
            val propertyName = property.simpleName.asString()
            AttributeName(propertyName, property.stringAnnotation(ATTRIBUTE_NAME_ANNOTATION) ?: attributeNameOf(propertyName))
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

        val tag = declaration.stringAnnotation(TAG_ANNOTATION) ?: tagOf(tagPrefix, attributeNameOf(simpleName))
        val attributesName = "${simpleName}Attributes"
        val referenceName = "${simpleName}DefaultReference"
        val spec = file(
            packageName = packageName,
            elementClass = elementClass,
            attributesName = attributesName,
            referenceName = referenceName,
            tag = tag,
            names = names,
            modifiers = declaration.visibilityModifiers(),
        )
        spec.writeTo(codeGenerator, Dependencies(aggregating = false, file))
    }

    private fun KSDeclaration.stringAnnotation(shortName: String): String? = annotations
        .firstOrNull { it.shortName.asString() == shortName }
        ?.arguments
        ?.firstOrNull()
        ?.value as? String

    /**
     * The generated types are as visible as the element: a public companion cannot extend an `internal`
     * supertype, and an `internal` element does not leak a public one.
     */
    private fun KSDeclaration.visibilityModifiers(): List<KModifier> = when (getVisibility()) {
        Visibility.PUBLIC -> emptyList()
        else -> listOf(KModifier.INTERNAL)
    }

    private fun file(
        packageName: String,
        elementClass: ClassName,
        attributesName: String,
        referenceName: String,
        tag: String,
        names: List<AttributeName>,
        modifiers: List<KModifier>,
    ): FileSpec {
        val builder = FileSpec.builder(packageName, referenceName)
            .addFileComment("Generated by ktjs-dom-kit-ksp from the element's declarations. Do not edit.")

        if (names.isNotEmpty()) {
            builder.addType(attributesObject(attributesName, elementClass, names, modifiers))
        }

        return builder.addType(referenceType(referenceName, elementClass, attributesName, tag, names, modifiers))
            .build()
    }

    private fun attributesObject(
        attributesName: String,
        elementClass: ClassName,
        names: List<AttributeName>,
        modifiers: List<KModifier>,
    ): TypeSpec {
        val builder = TypeSpec.objectBuilder(attributesName)
            .addModifiers(modifiers)
            .addKdoc(
                "The attributes of [%T], generated from its delegated properties.\n\n" +
                    "[ALL] is what the element hands to `observedAttributes`; [BY_PROPERTY] is how a\n" +
                    "`by attr` property without a name finds the attribute it stands for.\n",
                elementClass,
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
            PropertySpec.builder(ALL, ARRAY_OF_STRING)
                .initializer(CodeBlock.of("arrayOf(%L)", names.joinToString(", ") { constantNameOf(it.property) }))
                .build()
        )

        builder.addProperty(
            PropertySpec.builder(BY_PROPERTY, MAP.parameterizedBy(STRING, STRING))
                .initializer(
                    CodeBlock.of(
                        "mapOf(%L)",
                        names.joinToString(", ") { "\"${it.property}\" to ${constantNameOf(it.property)}" },
                    )
                )
                .build()
        )

        return builder.build()
    }

    private fun referenceType(
        referenceName: String,
        elementClass: ClassName,
        attributesName: String,
        tag: String,
        names: List<AttributeName>,
        modifiers: List<KModifier>,
    ): TypeSpec {
        val attributesClass = ClassName(elementClass.packageName, attributesName)
        val hasAttributes = names.isNotEmpty()

        val constructor = FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("name", STRING).defaultValue("%S", tag).build())
            .addParameter(
                ParameterSpec.builder("formAssociated", BOOLEAN.copy(nullable = true)).defaultValue("null").build()
            )
            .addParameter(
                ParameterSpec.builder("observedAttributes", READONLY_JS_STRINGS.copy(nullable = true))
                    .defaultValue(if (hasAttributes) "%T.$ALL" else "null", attributesClass)
                    .build()
            )
            .addParameter(ParameterSpec.builder("register", BOOLEAN).defaultValue("true").build())
            .addParameter(
                ParameterSpec.builder("attributeNames", MAP.parameterizedBy(STRING, STRING).copy(nullable = true))
                    .defaultValue(if (hasAttributes) "%T.$BY_PROPERTY" else "null", attributesClass)
                    .build()
            )

        return TypeSpec.classBuilder(referenceName)
            // `open`: the element's companion object extends it, and that subclass is the whole point.
            .addModifiers(modifiers + KModifier.OPEN)
            .addKdoc(
                "The reference of [%T]: its tag, its observed attributes and its attribute-name table.\n\n" +
                    "The companion object extends this and nothing else; every parameter defaults to what the\n" +
                    "element declares, and passing one overrides it (`%L(register = false)` for a tag that is\n" +
                    "defined later).\n",
                elementClass,
                referenceName,
            )
            .primaryConstructor(constructor.build())
            .superclass(REFERENCE_ADAPTER.parameterizedBy(elementClass))
            .addSuperclassConstructorParameter("clazz = %T::class.js", elementClass)
            .addSuperclassConstructorParameter("name = name")
            .addSuperclassConstructorParameter("formAssociated = formAssociated")
            .addSuperclassConstructorParameter("observedAttributes = observedAttributes")
            .addSuperclassConstructorParameter("register = register")
            .addSuperclassConstructorParameter("attributeNames = attributeNames")
            .build()
    }

    private data class AttributeName(val property: String, val attribute: String)

    private companion object {
        const val TAG_PREFIX_OPTION = "ktjs.tagPrefix"

        val ARRAY_OF_STRING: TypeName = ClassName("kotlin", "Array").parameterizedBy(STRING)
        val READONLY_JS_STRINGS: TypeName =
            ClassName("js.array", "ReadonlyArray").parameterizedBy(ClassName("kotlin.js", "JsString"))
    }
}
