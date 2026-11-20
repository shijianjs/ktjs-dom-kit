package io.github.shijianjs.ktjs.dom.webcomponent.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/** Registers [AttributeNamesProcessor]; the file under `META-INF/services` names this class. */
internal class AttributeNamesProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        AttributeNamesProcessor(environment.codeGenerator, environment.logger)
}
