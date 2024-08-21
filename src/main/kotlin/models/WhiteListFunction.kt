package io.github.klahap.fraplin.models

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import io.github.klahap.fraplin.models.WhiteListFunction.Relative.Companion.addRelatives
import io.github.klahap.fraplin.models.WhiteListFunction.Relative.Fun.Companion.addRelative
import io.github.klahap.fraplin.models.WhiteListFunction.Relative.Module.Companion.groupByModule
import io.github.klahap.fraplin.util.addInterface
import io.github.klahap.fraplin.util.addObject
import io.github.klahap.fraplin.util.addProperty
import io.github.klahap.fraplin.util.toCamelCase
import kotlinx.serialization.Serializable

@Serializable
data class WhiteListFunction(
    val name: String,
    val isPublic: Boolean = false,
) {
    private fun toRelative() = Relative.Module(
        fullName = name,
        path = name.split('.'),
        isPublic = isPublic,
    )

    companion object {
        private const val WHITELIST_FUNCTIONS_INTERFACE = "WhiteListFun"
        private const val WHITELIST_FUNCTIONS_PRIVATE_INTERFACE = "${WHITELIST_FUNCTIONS_INTERFACE}Private"
        private const val WHITELIST_FUNCTIONS_PUBLIC_INTERFACE = "${WHITELIST_FUNCTIONS_INTERFACE}Public"

        fun FileSpec.Builder.addWhiteListFunction(
            packageName: String,
            functions: Collection<WhiteListFunction>,
        ) {
            addInterface(WHITELIST_FUNCTIONS_PRIVATE_INTERFACE) {
                addSuperinterface(ClassName(packageName, WHITELIST_FUNCTIONS_INTERFACE))
            }
            addInterface(WHITELIST_FUNCTIONS_PUBLIC_INTERFACE) {
                addSuperinterface(ClassName(packageName, WHITELIST_FUNCTIONS_INTERFACE))
            }
            addInterface(WHITELIST_FUNCTIONS_INTERFACE) {
                addModifiers(KModifier.SEALED)
                addProperty("name", String::class, KModifier.ABSTRACT)
                with(CodeGenContext(packageName = packageName)) {
                    addRelatives(functions.map { it.toRelative() })
                }
            }
        }
    }

    private data class CodeGenContext(
        val packageName: String,
    )

    private interface Relative {
        val fullName: String
        val isPublic: Boolean

        companion object {
            context(CodeGenContext)
            fun TypeSpec.Builder.addRelatives(allRelatives: Collection<Relative>) {
                val moduleMap = allRelatives.filterIsInstance<Module>().groupByModule().toMutableMap()
                allRelatives.filterIsInstance<Fun>().sortedBy { it.name }.forEach { fn ->
                    val childs = moduleMap.remove(fn.prettyName) ?: emptyList()
                    addRelative(fn, childs)
                }
                moduleMap.entries.sortedBy { it.key }.forEach { (moduleName, childs) ->
                    addInterface(moduleName) {
                        addRelatives(childs)
                    }
                }
            }
        }

        data class Fun(
            override val fullName: String,
            override val isPublic: Boolean,
        ) : Relative {
            val name get() = fullName.split('.').last()
            val prettyName get() = name.toCamelCase(capitalized = true)

            companion object {
                context(CodeGenContext)
                fun TypeSpec.Builder.addRelative(fn: Fun, childs: Collection<Relative>) {
                    addObject(fn.prettyName) {
                        addSuperinterface(
                            ClassName(
                                packageName,
                                if (fn.isPublic) WHITELIST_FUNCTIONS_PUBLIC_INTERFACE
                                else WHITELIST_FUNCTIONS_PRIVATE_INTERFACE
                            ),
                        )
                        addModifiers(KModifier.DATA)
                        addProperty("name", String::class) {
                            initializer("%S", fn.fullName)
                            addModifiers(KModifier.OVERRIDE)
                        }
                        addRelatives(childs)
                    }
                }
            }
        }

        data class Module(
            override val fullName: String,
            override val isPublic: Boolean,
            val path: List<String>,
        ) : Relative {
            init {
                assert(path.size > 1)
            }

            val prettyRootName get() = path.first().toCamelCase(capitalized = true)

            private fun dropRoot() = if (path.size <= 2)
                Fun(fullName = fullName, isPublic = isPublic)
            else
                Module(fullName = fullName, isPublic = isPublic, path = path.drop(1))

            companion object {
                fun Iterable<Module>.groupByModule() =
                    groupBy({ it.prettyRootName }, { it.dropRoot() })
            }
        }
    }
}
