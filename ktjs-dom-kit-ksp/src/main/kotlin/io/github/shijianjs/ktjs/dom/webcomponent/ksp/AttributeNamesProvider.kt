package io.github.shijianjs.ktjs.dom.webcomponent.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Registers [AttributeNamesProcessor]; the file under `META-INF/services` names this class.
 *
 * The whole environment is handed over, not just the generator and logger: the processor reads the
 * `ktjs.tagPrefix` option from it, which is how one build declares that every tag in its module starts
 * with `demo-` or `ktjs-` instead of annotating every class.
 */
internal class AttributeNamesProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        AttributeNamesProcessor(environment)
}
