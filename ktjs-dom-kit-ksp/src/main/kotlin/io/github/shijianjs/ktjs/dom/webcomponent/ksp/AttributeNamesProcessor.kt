package io.github.shijianjs.ktjs.dom.webcomponent.ksp

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.File

private const val ADAPTER = "io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter"
private val REFERENCE_ADAPTER: ClassName =
    ClassName("io.github.shijianjs.ktjs.dom.webcomponent", "CustomElementReferenceAdapter")
private val ATTRIBUTES_CLASS: ClassName =
    ClassName("io.github.shijianjs.ktjs.dom.webcomponent", "CustomElementAttributes")
private val ATTRIBUTE_CODEC: ClassName =
    ClassName("io.github.shijianjs.ktjs.dom.attribute", "AttributeCodec")
private val ENUM_ATTRIBUTE_CODEC: MemberName =
    MemberName("io.github.shijianjs.ktjs.dom.attribute.types", "enumAttributeCodec")
private const val ATTRIBUTE_NAME_ANNOTATION = "AttributeName"

/** The array the reference hands to `observedAttributes`, in the element's companion object. */
private const val ALL = "ALL"

/** The tables the `attr` delegations read, in the element's companion object. */
private const val ATTRIBUTES = "ATTRIBUTES"

/** Default for the `ktjs.tagPrefix` option: every element in this account's libraries is a `ktjs-…` tag. */
private const val DEFAULT_TAG_PREFIX = "ktjs-"

/**
 * Generates, for every class extending `CustomElementAdapter`, the companion-object members its element
 * would otherwise write by hand: the attribute names, and the `…DefaultReference` the companion
 * extends.
 *
 * The browser reads `observedAttributes` once, at `customElements.define`, so the list has to exist
 * before the companion's `init` — before any instance and long before a delegate. That rules out
 * collecting the names from the delegates themselves (they are created per instance, in the
 * constructor). A Kotlin/JS class has no static initializer that could see its properties either.
 * The compiler is the only place that knows the declarations before anything runs, which is why this
 * processor exists.
 *
 * ```kotlin
 * class DemoCounterDefaultReference(
 *     name: String = "demo-counter",
 *     formAssociated: Boolean? = null,
 *     observedAttributes: ReadonlyArray<JsString>? = ALL,
 *     register: Boolean = true,
 *     attributes: CustomElementAttributes? = ATTRIBUTES,
 * ) : CustomElementReferenceAdapter<DemoCounter>(…) {
 *     companion object {
 *         const val VALUE: String = "value"
 *         const val INCREMENT_LABEL: String = "increment-label"
 *         val ALL: Array<String> = arrayOf(VALUE, INCREMENT_LABEL)
 *         val ATTRIBUTES: CustomElementAttributes = CustomElementAttributes(
 *             names = mapOf("value" to VALUE, "incrementLabel" to INCREMENT_LABEL),
 *         )
 *     }
 * }
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
 * the tag is the `ktjs.tagPrefix` option (default `ktjs-`) followed by the class name in kebab case,
 * unless the name already starts that way — and a companion that passes `name = "…"` overrides it.
 * Which properties are attributes at all is decided by `isDelegated()`: inside a `CustomElementAdapter`
 * the only delegated properties are `attr` ones. They have to be `public`, like the attributes they are.
 *
 * An enum-typed property also gets a codec (`KIND_CODEC`), so `by attr` needs no argument for it either.
 *
 * A class that extends another element gets that element's names and codecs merged into its own: the
 * browser reads `observedAttributes` per defined tag and knows nothing of Kotlin inheritance. A base
 * whose module never ran this processor has nothing to merge, and the subclass gets a warning.
 *
 * After the last round, one more file lists every element of the compilation ([writeRegistry]), so a
 * site or a test can define all tags by touching one object.
 */
internal class AttributeNamesProcessor(
    environment: SymbolProcessorEnvironment,
    private val codeGenerator: CodeGenerator = environment.codeGenerator,
    private val logger: KSPLogger = environment.logger,
    private val tagPrefix: String = environment.options[TAG_PREFIX_OPTION] ?: DEFAULT_TAG_PREFIX,
    private val registryPackage: String? = environment.options[REGISTRY_PACKAGE_OPTION],
    /** The name of the one object listing the elements; `ktjs.registryName` names it after the module. */
    private val registryName: String? = environment.options[REGISTRY_NAME_OPTION],
) : SymbolProcessor {

    /** Qualified names already written in this compilation; a class must produce one file, not one per round. */
    private val generated: MutableSet<String> = mutableSetOf()

    /** What was generated, for the registry written once at the end; plain data, the KS nodes are gone by then. */
    private val elements: MutableList<Generated> = mutableListOf()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // One pass to find each element, so that the second pass can merge a parent's tables into a
        // child's even when the parent's file comes later in the same round.
        val found = elementsIn(resolver)
        for (source in found.values) generate(source, found, resolver)
        return emptyList()
    }

    override fun finish() {
        writeRegistry()
    }

    private fun elementsIn(resolver: Resolver): Map<String, ElementSource> {
        val found = LinkedHashMap<String, ElementSource>()
        for (file in resolver.getAllFiles().toList()) {
            // A second round sees the files this processor just wrote; their symbols are already
            // covered, and generating them again would collide on the file name.
            if (file.filePath.isGenerated()) continue
            for (declaration in file.declarations) collect(declaration, file, found)
        }
        return found
    }

    private fun collect(declaration: KSDeclaration, file: KSFile, into: MutableMap<String, ElementSource>) {
        if (declaration !is KSClassDeclaration) return
        if (!declaration.isCompanionObject) {
            val qualifiedName = declaration.qualifiedName?.asString()
            if (qualifiedName != null && qualifiedName !in into && declaration.extendsAdapter()) {
                into[qualifiedName] = ElementSource(declaration, file)
            }
        }
        for (member in declaration.declarations) collect(member, file, into)
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

    private fun generate(source: ElementSource, found: Map<String, ElementSource>, resolver: Resolver) {
        val declaration = source.declaration
        val qualifiedName = declaration.qualifiedName?.asString() ?: return
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
            AttributeName(
                property = propertyName,
                attribute = property.stringAnnotation(ATTRIBUTE_NAME_ANNOTATION) ?: attributeNameOf(propertyName),
                enumType = property.enumType(),
            )
        }
        val duplicated = names.groupBy { it.attribute }.filterValues { it.size > 1 }
        if (duplicated.isNotEmpty()) {
            logger.error(
                "Two properties of '$qualifiedName' map to the same attribute: " +
                    duplicated.entries.joinToString { (attribute, owners) ->
                        "'$attribute' (${owners.joinToString { it.property }})"
                    } + ". Give one of them an explicit @AttributeName.",
                declaration,
            )
            return
        }
        val overridden = properties.filter { it.findOverridee()?.isDelegated() == true }
        if (overridden.isNotEmpty()) {
            logger.error(
                overridden.joinToString { "'$qualifiedName.${it.simpleName.asString()}'" } +
                    " overrides a delegated property. An `override` does not replace the delegate of the base " +
                    "class: its initializer still runs, so the attribute would have two delegates — the base's " +
                    "`onChange` fires for a property it no longer reads, and `writeDefaults` writes the " +
                    "attribute twice. Declare the attribute in one of the two classes.",
                declaration,
            )
            return
        }
        val hidden = properties.filter { it.getVisibility() != Visibility.PUBLIC }
        if (hidden.isNotEmpty()) {
            logger.error(
                hidden.joinToString { "'$qualifiedName.${it.simpleName.asString()}' (${it.getVisibility()})" } +
                    " is not public. An attribute is public by nature: markup, `getAttribute` and any code " +
                    "holding the element reach it, and hiding only the Kotlin half splits one value in two. " +
                    "Declare it `public`, or stop making it an attribute.",
                declaration,
            )
            return
        }

        val tag = tagOf(tagPrefix, attributeNameOf(simpleName))
        val referenceName = "${simpleName}DefaultReference"
        val parent = generatedParentOf(declaration, found, resolver)
        val internal = declaration.getVisibility() != Visibility.PUBLIC

        val spec = file(
            packageName = packageName,
            referenceName = referenceName,
            elementClass = elementClass,
            tag = tag,
            names = names,
            parent = parent,
            internal = internal,
        )
        spec.writeTo(codeGenerator, Dependencies(aggregating = false, source.file))
        if (declaration.isRegistryEntry()) {
            elements.add(Generated(qualifiedName, packageName, elementClass, internal, source.file))
        }
    }

    /**
     * Whether the registry may name this class.
     *
     * An entry is the element's *companion object*, because a class name in expression position means
     * its companion and that is the reference the list holds. So a class without a companion is out —
     * a base that is only ever subclassed has no tag of its own — and so is an `abstract` one, which
     * nothing could instantiate. Both still get their `…DefaultReference` file: a subclass merges
     * their tables into its own, and the generated code of that subclass names them.
     */
    private fun KSClassDeclaration.isRegistryEntry(): Boolean {
        if (classKind != ClassKind.CLASS || Modifier.ABSTRACT in modifiers) return false
        val companion = declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.isCompanionObject } ?: return false
        return companion.hasReferenceSupertype("${simpleName.asString()}DefaultReference")
    }

    /**
     * Whether a companion object's supertype is a reference.
     *
     * The `…DefaultReference` this round generates for the class cannot be resolved yet — it is written
     * after the symbol pass — so the name the companion `extends` spells is the first answer, and only
     * a reference written by hand, or one from another module, is resolved and walked.
     */
    private fun KSClassDeclaration.hasReferenceSupertype(expectedSimpleName: String): Boolean =
        superTypes.any { reference ->
            (reference.element as? KSClassifierReference)?.referencedName() == expectedSimpleName ||
                reference.extendsReferenceAdapter()
        }

    private fun KSTypeReference.extendsReferenceAdapter(): Boolean {
        val declaration = resolve().declaration as? KSClassDeclaration ?: return false
        return declaration.extendsReferenceAdapter(depth = 0)
    }

    private fun KSClassDeclaration.extendsReferenceAdapter(depth: Int): Boolean {
        if (depth > 8) return false
        return superTypes.any { reference ->
            val declaration = reference.resolve().declaration
            declaration.qualifiedName?.asString() == REFERENCE_ADAPTER_NAME ||
                (declaration is KSClassDeclaration && declaration.extendsReferenceAdapter(depth + 1))
        }
    }

    /**
     * The generated reference of the nearest ancestor that has one — the class whose tables this element
     * has to carry, because the browser reads `observedAttributes` per defined tag.
     *
     * An ancestor in this compilation is known by the fact that it is one of [found]; one in another
     * module only if its `…DefaultReference` is on the classpath, which is exactly the condition for
     * extending it. `DemoPage` is neither: it is an element written by hand, and a page gets nothing
     * merged from it.
     *
     * An ancestor that declares attributes but has no generated reference is the case this cannot fix:
     * the processor never ran in its module, so its names exist nowhere to merge. The walk continues,
     * and [warnUncovered] leaves a warning — the alternative is a subclass that silently accepts no
     * markup for the attributes its base declares.
     */
    private fun generatedParentOf(
        declaration: KSClassDeclaration,
        found: Map<String, ElementSource>,
        resolver: Resolver,
    ): ClassName? {
        val queue = ArrayDeque(listOf(declaration))
        val visited = mutableSetOf<String>()
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (reference in current.superTypes) {
                val superDeclaration = reference.resolve().declaration as? KSClassDeclaration ?: continue
                val superName = superDeclaration.qualifiedName?.asString() ?: continue
                if (ADAPTER == superName || !visited.add(superName)) continue

                val superPackage = superDeclaration.packageName.asString()
                val superSimple = superDeclaration.simpleName.asString()
                val candidate = ClassName(superPackage, "${superSimple}DefaultReference")
                val generatedHere = superName in found
                val generatedElsewhere = superName != ADAPTER &&
                    resolver.getClassDeclarationByName(resolver.getKSNameFromString(candidate.canonicalName)) != null
                if (generatedHere || generatedElsewhere) return candidate
                warnUncovered(declaration, superDeclaration)
                queue.add(superDeclaration)
            }
        }
        return null
    }

    /** Says it out loud when a base class has attributes but no module ever generated its tables. */
    private fun warnUncovered(child: KSClassDeclaration, base: KSClassDeclaration) {
        if (!base.extendsAdapter()) return
        if (base.declarations.filterIsInstance<KSPropertyDeclaration>().none { it.isDelegated() }) return
        logger.warn(
            "'${base.qualifiedName?.asString()}' declares attributes but has no generated reference: its " +
                "module does not run ktjs-dom-kit-ksp. '${child.qualifiedName?.asString()}' therefore cannot " +
                "observe them, and markup that writes one is dropped without a word. Add the processor to that " +
                "module, or declare the attributes on the element itself.",
            child,
        )
    }

    private fun KSPropertyDeclaration.enumType(): ClassName? {
        val resolved = type.resolve().declaration as? KSClassDeclaration ?: return null
        if (resolved.classKind != ClassKind.ENUM_CLASS) return null
        val qualifiedName = resolved.qualifiedName?.asString() ?: return null
        val packageName = resolved.packageName.asString()
        val nested = qualifiedName.removePrefix(if (packageName.isEmpty()) "" else "$packageName.")
        return ClassName(packageName, *nested.split('.').toTypedArray())
    }

    private fun KSDeclaration.stringAnnotation(shortName: String): String? = annotations
        .firstOrNull { it.shortName.asString() == shortName }
        ?.arguments
        ?.firstOrNull()
        ?.value as? String

    private fun file(
        packageName: String,
        referenceName: String,
        elementClass: ClassName,
        tag: String,
        names: List<AttributeName>,
        parent: ClassName?,
        internal: Boolean,
    ): FileSpec {
        val modifiers = if (internal) listOf(KModifier.INTERNAL) else emptyList()
        return FileSpec.builder(packageName, referenceName)
            .indent(INDENT)
            .addFileComment("Generated by ktjs-dom-kit-ksp from the element's declarations. Do not edit.")
            .addType(referenceType(referenceName, elementClass, tag, names, parent, modifiers))
            .build()
    }

    private fun referenceType(
        referenceName: String,
        elementClass: ClassName,
        tag: String,
        names: List<AttributeName>,
        parent: ClassName?,
        modifiers: List<KModifier>,
    ): TypeSpec {
        val constructor = FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("name", STRING).defaultValue("%S", tag).build())
            .addParameter(
                ParameterSpec.builder("formAssociated", BOOLEAN.copy(nullable = true)).defaultValue("null").build()
            )
            .addParameter(
                ParameterSpec.builder("observedAttributes", READONLY_JS_STRINGS.copy(nullable = true))
                    .defaultValue(ALL)
                    .build()
            )
            .addParameter(ParameterSpec.builder("register", BOOLEAN).defaultValue("true").build())
            .addParameter(
                ParameterSpec.builder("attributes", ATTRIBUTES_CLASS.copy(nullable = true))
                    .defaultValue(ATTRIBUTES)
                    .build()
            )

        return TypeSpec.classBuilder(referenceName)
            // `open`: the element's companion object extends it, and that subclass is the whole point.
            .addModifiers(modifiers + KModifier.OPEN)
            .addKdoc(
                "The reference of [%T]: its tag, its observed attributes and its attribute tables.\n\n" +
                    "The companion object extends this and nothing else; every parameter defaults to what the\n" +
                    "element declares, and passing one overrides it — `name = …` when the class name does not\n" +
                    "spell the tag, `register = false` when something else decides whether the tag exists.\n",
                elementClass,
            )
            .primaryConstructor(constructor.build())
            .superclass(REFERENCE_ADAPTER.parameterizedBy(elementClass))
            .addSuperclassConstructorParameter("clazz = %T::class.js", elementClass)
            .addSuperclassConstructorParameter("name = name")
            .addSuperclassConstructorParameter("formAssociated = formAssociated")
            .addSuperclassConstructorParameter("observedAttributes = observedAttributes")
            .addSuperclassConstructorParameter("register = register")
            .addSuperclassConstructorParameter("attributes = attributes")
            .addType(companion(elementClass, names, parent))
            .build()
    }

    /**
     * The static half of the element, all in one companion object: the attribute names, the array the
     * browser reads once, the tables a delegate reads per instance, and a codec per enum property.
     */
    private fun companion(elementClass: ClassName, names: List<AttributeName>, parent: ClassName?): TypeSpec {
        val builder = TypeSpec.companionObjectBuilder().addKdoc(
            "The attributes of [%T], generated from its delegated properties.\n\n" +
                "[ALL] is what the reference hands to `observedAttributes`; [ATTRIBUTES] is how a `by attr`\n" +
                "property without arguments finds the attribute it stands for and the codec that converts it.\n",
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
        for (name in names.filter { it.enumType != null }) {
            val enumType = name.enumType ?: continue
            builder.addProperty(
                PropertySpec.builder(codecNameOf(name.property), ATTRIBUTE_CODEC.parameterizedBy(enumType))
                    .initializer("%M(%T.entries)", ENUM_ATTRIBUTE_CODEC, enumType)
                    .build()
            )
        }
        builder.addProperty(
            PropertySpec.builder(ALL, ARRAY_OF_STRING).initializer(allArray(names, parent)).build()
        )
        builder.addProperty(
            PropertySpec.builder(ATTRIBUTES, ATTRIBUTES_CLASS).initializer(tables(names, parent)).build()
        )
        return builder.build()
    }

    private fun allArray(names: List<AttributeName>, parent: ClassName?): CodeBlock {
        val own = if (names.isEmpty()) {
            CodeBlock.of("emptyArray()")
        } else {
            CodeBlock.of("arrayOf(%L)", names.joinToString(", ") { constantNameOf(it.property) })
        }
        return when {
            parent == null -> own
            names.isEmpty() -> CodeBlock.of("%T.$ALL", parent)
            else -> CodeBlock.of("%T.$ALL + %L", parent, own)
        }
    }

    private fun tables(names: List<AttributeName>, parent: ClassName?): CodeBlock {
        val codecs = names.filter { it.enumType != null }
        return CodeBlock.builder()
            .add("%T(\n", ATTRIBUTES_CLASS)
            .indent()
            .add("names = %L,\n", merged(parent, "names", names) { it.property to constantNameOf(it.property) })
            .add("codecs = %L,\n", merged(parent, "codecs", codecs) { it.property to codecNameOf(it.property) })
            .unindent()
            .add(")")
            .build()
    }

    /** `mapOf(…)`, or the parent's table plus it, or the parent's alone when this class adds nothing. */
    private fun merged(
        parent: ClassName?,
        field: String,
        names: List<AttributeName>,
        entry: (AttributeName) -> Pair<String, String>,
    ): CodeBlock {
        val own = if (names.isEmpty()) {
            CodeBlock.of("emptyMap()")
        } else {
            CodeBlock.builder()
                .add("mapOf(\n")
                .indent()
                .apply { names.forEach { add("%S to %L,\n", entry(it).first, entry(it).second) } }
                .unindent()
                .add(")")
                .build()
        }
        return when {
            parent == null -> own
            names.isEmpty() -> CodeBlock.of("%T.$ATTRIBUTES.$field", parent)
            else -> CodeBlock.of("%T.$ATTRIBUTES.$field + %L", parent, own)
        }
    }

    /**
     * One file per compilation, listing every element the processor generated for — written after the
     * last round, because the list is only complete then.
     *
     * Reading it initialises every `…DefaultReference`, and their `init` is what calls
     * `customElements.define`: `KtjsCustomElements.ALL` is a "define everything this module ships" in
     * one reference, and `TAGS` is the same list as strings, for markup, routing and tests.
     *
     * Only classes a list entry can name are in it: an element whose companion object is its reference,
     * and not an `abstract` one. A base that is only ever subclassed keeps its generated reference for
     * its subclasses to merge from, but has no tag and no entry.
     */
    private fun writeRegistry() {
        if (elements.isEmpty()) return
        val packageName = registryPackage ?: commonPackageOf(elements.map { it.packageName }) ?: run {
            logger.info(
                "ktjs-dom-kit-ksp: elements live in unrelated packages, so no registry was generated. " +
                    "Set the `ktjs.registryPackage` option to choose one."
            )
            return
        }
        if (packageName.isBlank()) return

        val name = registryName ?: registryNameOf(tagPrefix)
        val internal = elements.any { it.internal }
        val ordered = elements.sortedBy { it.qualifiedName }
        val registry = TypeSpec.objectBuilder(name)
            .apply { if (internal) addModifiers(KModifier.INTERNAL) }
            .addKdoc(
                "Every custom element generated in this compilation.\n\n" +
                    "Each entry of [ALL] is an element's companion object — its reference — so reading the list\n" +
                    "initialises every reference, and a reference defines its tag in its `init`: `ALL` is\n" +
                    "\"define everything this module ships\". [TAGS] is the same list as the strings the browser knows\n" +
                    "the tags by, for markup, routing and tests.\n"
            )
            .addProperty(
                PropertySpec.builder("ALL", LIST.parameterizedBy(REFERENCE_ADAPTER.parameterizedBy(STAR)))
                    .initializer(
                        CodeBlock.builder()
                            .add("listOf<%T>(\n", REFERENCE_ADAPTER.parameterizedBy(STAR))
                            .indent()
                            .apply { ordered.forEach { add("%T,\n", it.elementClass) } }
                            .unindent()
                            .add(")\n")
                            .build()
                    )
                    .build()
            )
            .addProperty(
                PropertySpec.builder("TAGS", LIST.parameterizedBy(STRING))
                    .getter(FunSpec.getterBuilder().addStatement("return ALL.map { it.tag }").build())
                    .build()
            )
            .build()

        val files = elements.map { it.file }.distinct().toTypedArray()
        FileSpec.builder(packageName, name)
            .indent(INDENT)
            .addFileComment("Generated by ktjs-dom-kit-ksp. Do not edit.")
            .addType(registry)
            .build()
            .writeTo(codeGenerator, Dependencies(aggregating = true, *files))
    }

    private fun String.isGenerated(): Boolean =
        contains("${File.separator}build${File.separator}generated${File.separator}")

    private class ElementSource(val declaration: KSClassDeclaration, val file: KSFile)

    private data class AttributeName(val property: String, val attribute: String, val enumType: ClassName? = null)

    private class Generated(
        val qualifiedName: String,
        val packageName: String,
        /** The element class: in expression position, its name is its companion object, the reference. */
        val elementClass: ClassName,
        val internal: Boolean,
        val file: KSFile,
    )

    private companion object {
        const val TAG_PREFIX_OPTION = "ktjs.tagPrefix"
        const val REGISTRY_PACKAGE_OPTION = "ktjs.registryPackage"
        const val REGISTRY_NAME_OPTION = "ktjs.registryName"

        /** The repositories' Kotlin style: four spaces, like the sources next to the generated files. */
        const val INDENT = "    "

        val ARRAY_OF_STRING: TypeName = ClassName("kotlin", "Array").parameterizedBy(STRING)

        /** For a name-only comparison: a reference from another module is resolved, type arguments are not. */
        val REFERENCE_ADAPTER_NAME: String = REFERENCE_ADAPTER.canonicalName

        val READONLY_JS_STRINGS: TypeName =
            ClassName("js.array", "ReadonlyArray").parameterizedBy(ClassName("kotlin.js", "JsString"))
    }
}
