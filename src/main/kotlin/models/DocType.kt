package com.fraplin.models

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.fraplin.util.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject


data class DocType(
    val module: String?,
    val docTypeName: String,
    val docTypeType: Type,
    val fields: List<DocField>,
) {
    enum class Type(val creatable: Boolean) {
        NORMAL(creatable = true),
        SINGLE(creatable = false),
        CHILD(creatable = true),
    }


    val prettyModule = module?.toSnakeCase()
    val prettyName = docTypeName.toCamelCase(capitalized = true)
    fun getPackageName(context: CodeGenContext) = listOfNotNull(
        context.packageName, "doctype", prettyModule
    ).joinToString(separator = ".")

    fun getRelativePath() = listOfNotNull(
        "doctype", prettyModule, "$prettyName.kt"
    ).joinToString(separator = "/")

    fun getClassName(context: CodeGenContext) = ClassName(packageName = getPackageName(context), prettyName)
    fun getLinkClassName(context: CodeGenContext) = ClassName(packageName = getPackageName(context), prettyName, "Link")
    fun getLinkJsonTypeClassName(context: CodeGenContext) =
        ClassName(packageName = getPackageName(context), prettyName, "Link", "JsonType")

    fun getLinkSerializerClassName(context: CodeGenContext, nullable: Boolean) =
        ClassName(
            packageName = getPackageName(context),
            prettyName,
            "Link",
            if (nullable) "NullableSerializer" else "Serializer",
        )

    context(FileSpec.Builder)
    fun addDummyCode(context: CodeGenContext) {
        clazz(prettyName) {
            addAnnotation(context.annotationFrappeDocType) {
                addMember("docTypeName = %S", docTypeName)
            }
            addDocTypeLinkClass(
                docType = ClassName("", prettyName),
                context = context,
            )
            addSuperinterface(context.getFrappeDocType(docTypeType))
        }
    }

    fun getCode(context: CodeGenContext) = fileBuilder(
        packageName = getPackageName(context),
        fileName = prettyName,
    ) {
        dataClass(name = prettyName) {
            addAnnotation(context.annotationSerializable)
            addAnnotation(context.annotationFrappeDocType) {
                addMember("docTypeName = %S", docTypeName)
            }
            addSuperinterface(context.getFrappeDocType(docTypeType))
            primaryConstructor {
                fields.forEach { field ->
                    addParameter(field.getParameterSpec(
                        parent = this@DocType,
                        context = context,
                        withAnnotations = true,
                        mutable = false,
                        forceNullable = true,
                    ) {})
                    addProperty(
                        field.getPropertySpec(
                            parent = this@DocType,
                            context, mutable = false,
                            forceNullable = true,
                        )
                    )
                }
            }
            addDocTypeLinkClass(
                docType = ClassName("", prettyName),
                context = context,
            )
            clazz(name = "Builder") {
                val builderDataName = "_builderData".makeDifferent(fields.map { it.prettyFieldName })
                addSuperinterface(
                    context.docTypeBuilder.parameterizedBy(
                        ClassName("", prettyName),
                        ClassName("", "Builder"),
                    )
                )
                addProperty(
                    name = builderDataName,
                    type = ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                        String::class.asTypeName(),
                        JsonElement::class.asTypeName(),
                    )
                ) {
                    addModifiers(KModifier.PRIVATE)
                    initializer("mutableMapOf()")
                }
                addFunction("get") {
                    addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
                    addParameter("field", String::class.asTypeName())
                    returns(JsonElement::class.asTypeName().copy(nullable = true))
                    addStatement("return %L[field]", builderDataName)
                }
                addFunction("set") {
                    addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
                    addParameter("field", String::class.asTypeName())
                    addParameter("value", JsonElement::class.asTypeName())
                    addStatement("%L[field] = value", builderDataName)
                }
                addFunction("remove") {
                    addModifiers(KModifier.OVERRIDE)
                    addParameter("field", String::class.asTypeName())
                    returns(JsonElement::class.asTypeName().copy(nullable = true))
                    addStatement("return %L.remove(field)", builderDataName)
                }
                addFunction("clear") {
                    addModifiers(KModifier.OVERRIDE)
                    addStatement("%L.clear()", builderDataName)
                }
                addFunction("build") {
                    addModifiers(KModifier.OVERRIDE)
                    returns(JsonObject::class.asTypeName())
                    addStatement("return JsonObject(%L)", builderDataName)
                }

                fields.forEach { field ->
                    if (field is DocField.Table) {
                        val child = context.docTypes[field.option]!!.getClassName(context)
                        val builder = context.getFrappeDocTableBuilder(child)
                        addFunction(field.prettyFieldName) {
                            addParameter(
                                "block",
                                LambdaTypeName.get(receiver = builder, returnType = Unit::class.asTypeName())
                            )
                            addStatement(
                                "%L[%S] = %T().apply(block).build()",
                                builderDataName,
                                field.fieldName,
                                builder
                            )
                        }
                    }
                    val baseType = field.getBaseTypeName(
                        parent = this@DocType,
                        context = context,
                        mutable = false,
                    )
                    addProperty(
                        name = field.prettyFieldName,
                        type = baseType.copy(nullable = true)
                    ) {
                        mutable(true)
                        delegate(
                            "%T<%T, Builder>(%S, %L)",
                            context.docFieldBuilderDelegation,
                            baseType,
                            field.fieldName,
                            field.toJsonElementTypeInitString(context, mutable = false)
                        )
                    }
                }
            }

            fields.filterIsInstance<DocField.Select>().forEach { field ->
                addType(field.getEnumSpec(context))
            }
        }
    }

    context(FileSpec.Builder)
    fun addHelperCode(context: CodeGenContext) {

        val className = getClassName(context)
        val builder = className.nestedClass("Builder")
        val tableBuilder = context.getFrappeDocTableBuilder(getClassName(context))
        val reqFields = fields.filter { it.required }

        fun FunSpec.Builder.addReqParams() {
            reqFields.forEach { field ->
                addParameter(
                    field.getParameterSpec(
                        parent = this@DocType,
                        context = context,
                        withAnnotations = false,
                        mutable = true,
                        forceNullable = false,
                    ) {})
            }
        }
        addFunction("load$prettyName") {
            receiver(context.frappeSiteService)
            addModifiers(KModifier.SUSPEND)
            returns(className)
            if (docTypeType != Type.SINGLE) {
                addParameter("name", String::class.asTypeName())
                addStatement("return this.load(docType=%T::class, name=name)", className)
            } else
                addStatement("return this.load(docType=%T::class)", className)
        }
        if (docTypeType != Type.SINGLE)
            addFunction("load${prettyName}OrNull") {
                receiver(context.frappeSiteService)
                addModifiers(KModifier.SUSPEND)
                returns(className.copy(nullable = true))
                addParameter("name", String::class.asTypeName())
                addStatement("return this.loadOrNull(docType=%T::class, name=name)", className)
            }
        if (docTypeType != Type.SINGLE)
            addFunction("delete${prettyName}") {
                receiver(context.frappeSiteService)
                addModifiers(KModifier.SUSPEND)
                addParameter("name", String::class.asTypeName())
                addStatement("return this.delete(docType=%T::class, name=name)", className)
            }
        addFunction("update$prettyName") {
            receiver(context.frappeSiteService)
            addModifiers(KModifier.SUSPEND)
            returns(className)
            val blockName = "block".makeDifferent(blackList = reqFields.map { it.prettyFieldName })
            if (docTypeType != Type.SINGLE)
                addParameter("name", String::class.asTypeName())
            else
                addStatement("val name = %S", className)
            addParameter(blockName, LambdaTypeName.get(receiver = builder, returnType = Unit::class.asTypeName()))
            addStatement(
                "return this.update(docType=%T::class, name=name, data=%T().apply(%L).build())",
                className,
                builder,
                blockName,
            )
        }
        if (docTypeType != Type.SINGLE)
            addFunction("update") {
                receiver(context.frappeSiteService)
                addModifiers(KModifier.SUSPEND)
                returns(className)
                val blockName = "block".makeDifferent(blackList = reqFields.map { it.prettyFieldName })
                addParameter("link", getLinkClassName(context))
                addParameter(blockName, LambdaTypeName.get(receiver = builder, returnType = Unit::class.asTypeName()))
                addStatement(
                    "return this.update(docType=link.docType, name=link.value, data=%T().apply(%L).build())",
                    builder,
                    blockName,
                )
            }
        if (docTypeType.creatable)
            addFunction("create$prettyName") {
                receiver(context.frappeSiteService)
                addModifiers(KModifier.SUSPEND)
                returns(className)
                val blockName = "block".makeDifferent(blackList = reqFields.map { it.prettyFieldName })
                addReqParams()
                val builderName = "builder".makeDifferent(blackList = reqFields.map { it.prettyFieldName })
                addParameter(blockName, LambdaTypeName.get(receiver = builder, returnType = Unit::class.asTypeName()))
                addStatement("val %L = %T()", builderName, builder)
                reqFields.forEach { field ->
                    addStatement("%L.%L = %L", builderName, field.prettyFieldName, field.prettyFieldName)
                }
                addStatement("%L.apply(%L)", builderName, blockName)
                addStatement("return this.create(docType=%T::class, data=%L.build())", className, builderName)
            }
        if (docTypeType.creatable)
            addFunction("updateOrCreate$prettyName") {
                receiver(context.frappeSiteService)
                addModifiers(KModifier.SUSPEND)
                returns(className)
                addParameter(buildParameter(name = "name", type = String::class.asTypeName(), block = {}))
                addReqParams()
                val builderName = "builder".makeDifferent(blackList = reqFields.map { it.prettyFieldName })
                val dataName = "data".makeDifferent(blackList = reqFields.map { it.prettyFieldName })
                val blockName = "block".makeDifferent(blackList = reqFields.map { it.prettyFieldName })
                addParameter(blockName, LambdaTypeName.get(receiver = builder, returnType = Unit::class.asTypeName()))
                addStatement("val %L = %T()", builderName, builder)
                reqFields.forEach { field ->
                    addStatement("%L.%L = %L", builderName, field.prettyFieldName, field.prettyFieldName)
                }
                addStatement("%L.apply(%L)", builderName, blockName)
                addStatement("val %L = %L.build()", dataName, builderName)
                addCode {
                    beginControlFlow("return try")
                    addStatement("this.update(docType=%T::class, name=name, data=%L)", className, dataName)
                    nextControlFlow("catch (e: %T)", context.notFoundException)
                    addStatement("this.create(docType=%T::class, data=%L)", className, dataName)
                    endControlFlow()
                }
            }
        if (docTypeType == Type.CHILD)
            addFunction("add") {
                addAnnotation(JvmName::class.asClassName()) {
                    addMember("%S", "add$prettyName")
                }
                receiver(tableBuilder)
                returns(tableBuilder)
                addReqParams()
                val blockName = "block".makeDifferent(blackList = reqFields.map { it.prettyFieldName })
                addParameter(blockName, LambdaTypeName.get(receiver = builder, returnType = Unit::class.asTypeName()))
                addStatement("val builder = %T()", builder)
                reqFields.forEach { field ->
                    addStatement("builder.%L = %L", field.prettyFieldName, field.prettyFieldName)
                }
                addStatement("add(builder.apply(%L).build())", blockName)
                addStatement("return this")
            }
    }
}
