package io.github.klahap.fraplin.util

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.klahap.fraplin.models.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.reflect.KClass


fun DocField.isNullable(forceNullable: Boolean): Boolean =
    if (strictTyped)
        nullable
    else
        nullable || forceNullable

fun DocField.toClassVariablePropertySpec(
    parent: DocType,
    context: CodeGenContext,
) = buildProperty(
    name = prettyFieldName,
    type = toClassVariableTypeName(
        parent = parent,
        context = context,
    ),
) {
    initializer(prettyFieldName)
}

fun DocField.toClassVariableTypeName(parent: DocType, context: CodeGenContext): TypeName =
    getBaseTypeName(parent = parent, context = context)
        .copy(nullable = isNullable(forceNullable = true))

fun DocField.toClassConstructorTypeName(parent: DocType, context: CodeGenContext): TypeName = when (this) {
    is DocField.Table -> LambdaTypeName.get(
        receiver = context.getFrappeDocTableBuilder(getChildClassName(context)),
        returnType = UNIT
    )

    else -> getBaseTypeName(parent = parent, context = context)
        .copy(nullable = isNullable(forceNullable = false))
}


fun TypeSpec.Builder.addDocTypeLinkClass(docType: ClassName, context: CodeGenContext) {
    valueClass("Link") {
        addSuperinterface(context.frappeLinkField.parameterizedBy(docType))
        primaryConstructor {
            addParameter("value", String::class)
            addProperty("value", String::class) {
                initializer("value")
                addModifiers(KModifier.OVERRIDE)
            }
        }
        val linkClass = ClassName("", "Link")
        dataObject("Serializer") {
            superclass(context.frappeInlineStringFieldSerializer.parameterizedBy(linkClass))
            addSuperclassConstructorParameter("{ Link(it) }")
        }
        dataObject("NullableSerializer") {
            superclass(context.frappeInlineStringFieldNullableSerializer.parameterizedBy(linkClass))
            addSuperclassConstructorParameter("{ Link(it) }")
        }
        addObject("JsonType") {
            superclass(context.jsonElementType.nestedClass("InlineString").parameterizedBy(linkClass))
            addSuperclassConstructorParameter("{ Link(it) }")
        }

        addProperty(
            name = "docType",
            type = KClass::class.asTypeName().parameterizedBy(docType),
        ) {
            addModifiers(KModifier.OVERRIDE)
            getter {
                addStatement("return %T::class", docType)
            }
        }
        addCompanion {
            addProperty(
                name = "docType",
                type = KClass::class.asTypeName().parameterizedBy(docType),
            ) {
                getter {
                    addStatement("return %T::class", docType)
                }
            }
        }
    }
}

fun DocField.toClassVariableSpec(
    parent: DocType,
    context: CodeGenContext,
    block: ParameterSpec.Builder.() -> Unit,
) = buildParameter(
    prettyFieldName,
    toClassVariableTypeName(parent = parent, context)
) {
    addAnnotation(context.annotationSerialName) { addMember("%S", fieldName) }
    addAnnotation(context.annotationFrappeField) {
        addMember("type = %T.${originFieldType.name}", context.frappeFieldType)
    }
    when (this@toClassVariableSpec) {
        is DocField.Primitive -> when (fieldType) {
            DocField.Primitive.Type.STRING -> null
            DocField.Primitive.Type.INT -> null
            DocField.Primitive.Type.DOUBLE -> null
            DocField.Primitive.Type.DATE -> if (isNullable(true)) context.localDateNullableSerializer else context.localDateSerializer
            DocField.Primitive.Type.DATETIME -> if (isNullable(true)) context.localDateTimeNullableSerializer else context.localDateTimeSerializer
            DocField.Primitive.Type.TIME -> if (isNullable(true)) context.localTimeNullableSerializer else context.localTimeSerializer
        }?.let {
            addAnnotation(context.annotationSerializable) { addMember("with=%T::class", it) }
        }

        is DocField.Attach -> addAnnotation(context.annotationSerializable) {
            addMember(
                "with = %T.AttachField::class",
                if (isNullable(true)) context.frappeInlineStringFieldNullableSerializer else context.frappeInlineStringFieldSerializer
            )
        }

        is DocField.Check -> addAnnotation(context.annotationSerializable) {
            addMember("with = %T::class", context.booleanAsIntSerializer)
        }

        is DocField.Link -> addAnnotation(context.annotationSerializable) {
            addMember("with = %T::class", this@toClassVariableSpec.getLinkSerializerClassName(context))
        }

        is DocField.Select -> if (isNullable(true)) {
            addAnnotation(context.annotationSerializable) { addMember("with=$enumName.NullableSerializer::class") }
        }

        is DocField.Table -> addAnnotation(context.annotationFrappeTableField) {
            addMember("childDocTypeName = %S", option)
        }

        is DocField.DocStatus -> addAnnotation(context.annotationSerializable) {
            addMember("with = %T::class", context.frappeDocStatusSerializer)
        }

        is DocField.DynamicLink -> Unit
    }

    if (isNullable(forceNullable = true))
        defaultValue("null")
    block()
}

fun DocField.toClassConstructorVariableSpec(
    parent: DocType,
    context: CodeGenContext,
    block: ParameterSpec.Builder.() -> Unit,
) = buildParameter(
    prettyFieldName,
    toClassConstructorTypeName(parent = parent, context = context)
) {
    if (isNullable(forceNullable = false))
        defaultValue("null")
    block()
}

fun DocField.getBaseTypeName(parent: DocType, context: CodeGenContext): TypeName = when (this) {
    is DocField.Check -> Boolean::class.asTypeName()
    is DocField.DocStatus -> context.frappeDocStatus
    is DocField.Link -> getLinkClassName(context)
    is DocField.Primitive -> when (fieldType) {
        DocField.Primitive.Type.STRING -> String::class.asTypeName()
        DocField.Primitive.Type.INT -> Int::class.asTypeName()
        DocField.Primitive.Type.DOUBLE -> Double::class.asTypeName()
        DocField.Primitive.Type.DATE -> LocalDate::class.asTypeName()
        DocField.Primitive.Type.DATETIME -> LocalDateTime::class.asTypeName()
        DocField.Primitive.Type.TIME -> LocalTime::class.asTypeName()
    }

    is DocField.Attach -> context.frappeAttachField
    is DocField.Select -> ClassName("${parent.getPackageName(context)}.${parent.prettyName}", enumName)
    is DocField.Table -> ClassName("kotlin.collections", "List").parameterizedBy(
        getChildClassName(context)
    )
    is DocField.DynamicLink -> String::class.asTypeName()
}

fun DocField.toJsonElementTypeInitString(context: CodeGenContext, mutable: Boolean) = when (this) {
    is DocField.Check -> CodeBlock.of("%T.Boolean", context.jsonElementType)
    is DocField.DocStatus -> CodeBlock.of("%T.DocStatus", context.jsonElementType)
    is DocField.Link -> CodeBlock.of("%T", getLinkJsonTypeClassName(context))
    is DocField.Primitive -> when (fieldType) {
        DocField.Primitive.Type.STRING -> "String"
        DocField.Primitive.Type.INT -> "Int"
        DocField.Primitive.Type.DOUBLE -> "Double"
        DocField.Primitive.Type.DATE -> "LocalDate"
        DocField.Primitive.Type.DATETIME -> "LocalDateTime"
        DocField.Primitive.Type.TIME -> "LocalTime"
    }.let { CodeBlock.of("%T.$it", context.jsonElementType) }

    is DocField.Attach -> CodeBlock.of("%T.InlineString.Attach", context.jsonElementType)
    is DocField.Select -> CodeBlock.of("%T.Enum($enumName::class)", context.jsonElementType)
    is DocField.Table -> CodeBlock.of(
        "%T.%L",
        context.jsonElementType,
        if (mutable) "MutableList<$prettyChildName>()" else "List<$prettyChildName>()",
    )
    is DocField.DynamicLink -> CodeBlock.of("%T.String", context.jsonElementType)
}
