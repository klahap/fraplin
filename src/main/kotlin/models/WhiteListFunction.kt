package io.github.klahap.fraplin.models

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeVariableName.Companion.invoke
import io.github.klahap.fraplin.models.WhiteListFunction.Relative.Companion.addRelatives
import io.github.klahap.fraplin.models.WhiteListFunction.Relative.Fun.Companion.addRelative
import io.github.klahap.fraplin.models.WhiteListFunction.Relative.Module.Companion.groupByModule
import io.github.klahap.fraplin.util.*
import kotlinx.serialization.Serializable

@Serializable
data class WhiteListFunction(
    val name: String,
    val isPublic: Boolean = false,
    val args: List<Arg>,
) {
    val hasArgs get() = args.isNotEmpty()

    @Serializable
    data class Arg(
        val name: String,
        val type: Type,
    ) {
        val prettyName get() = name.toCamelCase(capitalized = true)

        enum class Type(val elementType: TypeName) {
            STRING(ClassName("kotlin", "String")),
            STRING_NULLABLE(ClassName("kotlin", "String").copy(nullable = true)),
            BOOLEAN(ClassName("kotlin", "Boolean")),
            BOOLEAN_NULLABLE(ClassName("kotlin", "Boolean").copy(nullable = true)),
            LONG(ClassName("kotlin", "Long")),
            LONG_NULLABLE(ClassName("kotlin", "Long").copy(nullable = true)),
            DOUBLE(ClassName("kotlin", "Double")),
            DOUBLE_NULLABLE(ClassName("kotlin", "Double").copy(nullable = true)),
            ANY(ClassName("kotlinx.serialization.json", "JsonElement")),
            ANY_NULLABLE(ClassName("kotlinx.serialization.json", "JsonElement").copy(nullable = true));

            context(CodeGenContext)
            fun toJsonElementFieldClass(): ClassName = jsonElementField(this)

            companion object {
                fun fromPythonType(type: String?) = when (val value = type?.replace(" ", "")?.takeIfNotBlank()) {
                    "str" -> STRING
                    "str|None" -> STRING_NULLABLE
                    "bool" -> BOOLEAN
                    "bool|None" -> BOOLEAN_NULLABLE
                    "int" -> LONG
                    "int|None" -> LONG_NULLABLE
                    "float" -> DOUBLE
                    "float|None" -> DOUBLE_NULLABLE
                    null -> ANY_NULLABLE
                    else -> if (value.endsWith("|None")) ANY_NULLABLE else ANY
                }
            }
        }
    }

    private fun toRelative() = Relative.Module(
        origin = this,
        path = name.split('.'),
        isPublic = isPublic,
    )

    companion object {

        fun FileSpec.Builder.addWhiteListFunction(
            packageName: String,
            functions: Collection<WhiteListFunction>,
        ) {
            val context = CodeGenContext(packageName = packageName)

            addObject("WhiteListFun") {
                with(context) {
                    addRelatives(functions.map { it.toRelative() })
                }
            }
        }
    }

    private data class CodeGenContext(
        val packageName: String,
    ) {
        private fun getJsonElementField(vararg names: String) =
            ClassName("$packageName.util", "JsonElementField", *names)

        val jsonElementField get() = getJsonElementField()
        fun jsonElementField(type: Arg.Type) = when (type) {
            Arg.Type.STRING -> "String"
            Arg.Type.STRING_NULLABLE -> "StringNullable"
            Arg.Type.BOOLEAN -> "Boolean"
            Arg.Type.BOOLEAN_NULLABLE -> "BooleanNullable"
            Arg.Type.LONG -> "Long"
            Arg.Type.LONG_NULLABLE -> "LongNullable"
            Arg.Type.DOUBLE -> "Double"
            Arg.Type.DOUBLE_NULLABLE -> "DoubleNullable"
            Arg.Type.ANY -> "Any"
            Arg.Type.ANY_NULLABLE -> "AnyNullable"
        }.let { getJsonElementField(it) }

        private fun getWhiteListFun(vararg names: String) = ClassName(packageName, "IWhiteListFun", *names)
        val whiteListFunWithArgs = getWhiteListFun("Args", "With")
        val whiteListFunWithoutArgs = getWhiteListFun("Args", "Without")
        val whiteListFunPrivate = getWhiteListFun("Scope", "Private")
        val whiteListFunPublic = getWhiteListFun("Scope", "Public")
    }

    private interface Relative {
        val origin: WhiteListFunction
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
            override val origin: WhiteListFunction,
            override val isPublic: Boolean,
        ) : Relative {
            val name get() = origin.name.split('.').last()
            val prettyName get() = name.toCamelCase(capitalized = true)

            companion object {
                context(CodeGenContext)
                fun TypeSpec.Builder.addRelative(fn: Fun, childs: Collection<Relative>) {
                    addObject(fn.prettyName) {
                        addSuperinterface((if (fn.isPublic) whiteListFunPublic else whiteListFunPrivate))
                        addSuperinterface(
                            (if (fn.origin.hasArgs)
                                whiteListFunWithArgs.parameterizedBy(
                                    ClassName("", fn.prettyName).nestedClass("Arg").parameterizedBy(STAR)
                                )
                            else
                                whiteListFunWithoutArgs),
                        )
                        addModifiers(KModifier.DATA)
                        addProperty("name", String::class) {
                            initializer("%S", fn.origin.name)
                            addModifiers(KModifier.OVERRIDE)
                        }
                        if (fn.origin.hasArgs)
                            addFunction("getArgs") {
                                addModifiers(KModifier.OVERRIDE)
                                returns(
                                    ClassName("kotlin.collections", "Map").parameterizedBy(
                                        ClassName("kotlin", "String"),
                                        ClassName("kotlinx.serialization.json", "JsonElement"),
                                    )
                                )
                                addParameter(
                                    "block", LambdaTypeName.get(
                                        receiver = ClassName("", "Arg").parameterizedBy(STAR),
                                        returnType = ClassName("kotlinx.serialization.json", "JsonElement"),
                                    )
                                )
                                if (fn.origin.hasArgs)
                                    addCode {
                                        add("return listOf(")
                                        fn.origin.args.forEach { arg ->
                                            add("Arg.${arg.prettyName}.name to block(Arg.${arg.prettyName}),\n")
                                        }
                                        add(").toMap()")
                                    }
                                else
                                    addCode { add("return emptyMap()") }
                            }
                        if (fn.origin.hasArgs)
                            addInterface("Arg") {
                                addModifiers(KModifier.SEALED)
                                addTypeVariable(TypeVariableName("T"))
                                addSuperinterface(jsonElementField.parameterizedBy(TypeVariableName("T")))
                                fn.origin.args.forEach { arg ->
                                    addObject(arg.prettyName) {
                                        addModifiers(KModifier.DATA)
                                        addSuperinterface(ClassName("", "Arg").parameterizedBy(arg.type.elementType))
                                        addSuperinterface(arg.type.toJsonElementFieldClass())
                                        addProperty("name", String::class) {
                                            initializer("%S", arg.name)
                                            addModifiers(KModifier.OVERRIDE)
                                        }
                                    }
                                }
                            }
                        addRelatives(childs)
                    }
                }
            }
        }

        data class Module(
            override val origin: WhiteListFunction,
            override val isPublic: Boolean,
            val path: List<String>,
        ) : Relative {
            init {
                assert(path.size > 1)
            }

            val prettyRootName get() = path.first().toCamelCase(capitalized = true)

            private fun dropRoot() = if (path.size <= 2)
                Fun(origin = origin, isPublic = isPublic)
            else
                Module(origin = origin, isPublic = isPublic, path = path.drop(1))

            companion object {
                fun Iterable<Module>.groupByModule() =
                    groupBy({ it.prettyRootName }, { it.dropRoot() })
            }
        }
    }
}
