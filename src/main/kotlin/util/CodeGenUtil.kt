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

fun DocField.getTypeName(parent: DocType, context: CodeGenContext, mutable: Boolean, forceNullable: Boolean): TypeName =
    getBaseTypeName(parent = parent, context = context, mutable = mutable)
        .copy(nullable = isNullable(forceNullable = forceNullable))


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

fun DocField.getParameterSpec(
    parent: DocType,
    context: CodeGenContext,
    withAnnotations: Boolean,
    mutable: Boolean,
    forceNullable: Boolean,
    block: ParameterSpec.Builder.() -> Unit,
) = buildParameter(
    prettyFieldName,
    getTypeName(parent = parent, context, mutable = mutable, forceNullable = forceNullable)
) {
    if (withAnnotations) {
        addAnnotation(context.annotationSerialName) { addMember("%S", fieldName) }
        addAnnotation(context.annotationFrappeField) {
            addMember("type = %T.${originFieldType.name}", context.frappeFieldType)
        }
        when (this@getParameterSpec) {
            is DocField.Primitive -> when (fieldType) {
                DocField.Primitive.Type.STRING -> null
                DocField.Primitive.Type.INT -> null
                DocField.Primitive.Type.DOUBLE -> null
                DocField.Primitive.Type.DATE -> if (isNullable(forceNullable)) context.localDateNullableSerializer else context.localDateSerializer
                DocField.Primitive.Type.DATETIME -> if (isNullable(forceNullable)) context.localDateTimeNullableSerializer else context.localDateTimeSerializer
                DocField.Primitive.Type.TIME -> if (isNullable(forceNullable)) context.localTimeNullableSerializer else context.localTimeSerializer
            }?.let {
                addAnnotation(context.annotationSerializable) { addMember("with=%T::class", it) }
            }

            is DocField.Attach -> addAnnotation(context.annotationSerializable) {
                addMember(
                    "with = %T.AttachField::class",
                    if (isNullable(forceNullable)) context.frappeInlineStringFieldNullableSerializer else context.frappeInlineStringFieldSerializer
                )
            }

            is DocField.Check -> addAnnotation(context.annotationSerializable) {
                addMember("with = %T::class", context.booleanAsIntSerializer)
            }

            is DocField.Link -> addAnnotation(context.annotationSerializable) {
                addMember("with = %T::class", this@getParameterSpec.getLinkSerializerClassName(context))
            }

            is DocField.Select -> if (isNullable(forceNullable)) {
                addAnnotation(context.annotationSerializable) { addMember("with=$enumName.NullableSerializer::class") }
            }

            is DocField.Table -> addAnnotation(context.annotationFrappeTableField) {
                addMember("childDocTypeName = %S", option)
            }
        }
    }
    if (isNullable(forceNullable = forceNullable))
        defaultValue("null")
    block()
}

fun DocField.getBaseTypeName(parent: DocType, context: CodeGenContext, mutable: Boolean): TypeName = when (this) {
    is DocField.Check -> Boolean::class.asTypeName()
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
    is DocField.Table -> ClassName("kotlin.collections", if (mutable) "MutableList" else "List").parameterizedBy(
        context.docTypes[option]?.getClassName(context)
            ?: throw RuntimeException("child doctype '$option' not loaded")
    )
}

fun DocField.toJsonElementTypeInitString(context: CodeGenContext, mutable: Boolean) = when (this) {
    is DocField.Check -> CodeBlock.of("%T.Boolean", context.jsonElementType)
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

}

fun DocField.getPropertySpec(
    parent: DocType,
    context: CodeGenContext,
    mutable: Boolean,
    forceNullable: Boolean,
) = buildProperty(
    name = prettyFieldName,
    type = getTypeName(
        parent = parent,
        context = context,
        mutable = mutable,
        forceNullable = forceNullable,
    ),
) {
    initializer(prettyFieldName)
}
