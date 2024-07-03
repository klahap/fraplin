package io.github.klahap.fraplin.models

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.klahap.fraplin.util.*


sealed interface DocField {
    val fieldName: String
    val label: String?
    val nullable: Boolean
    val required: Boolean
    val strictTyped: Boolean
    val originFieldType: FieldTypeRaw
    val prettyFieldName
        get() = fieldName.toCamelCase(capitalized = false)
            .let { if (it in kotlinKeywords) "`$it`" else it }

    data class Primitive(
        override val fieldName: String,
        override val label: String?,
        override val nullable: Boolean,
        override val required: Boolean,
        override val strictTyped: Boolean,
        override val originFieldType: FieldTypeRaw,
        val fieldType: Type,
    ) : DocField {
        enum class Type {
            STRING, INT, DOUBLE, DATE, DATETIME, TIME
        }
    }

    data class Attach(
        override val fieldName: String,
        override val label: String?,
        override val nullable: Boolean,
        override val required: Boolean,
        override val strictTyped: Boolean,
        override val originFieldType: FieldTypeRaw,
    ) : DocField

    data class Check(
        override val fieldName: String,
        override val label: String?,
        override val nullable: Boolean,
        override val required: Boolean,
        override val strictTyped: Boolean,
        override val originFieldType: FieldTypeRaw,
    ) : DocField

    data class Select(
        override val fieldName: String,
        override val label: String?,
        override val nullable: Boolean,
        override val required: Boolean,
        override val strictTyped: Boolean,
        override val originFieldType: FieldTypeRaw,
        val options: Set<String>,
    ) : DocField {
        val enumName = fieldName.toCamelCase(capitalized = true)
        private val enumType get() = ClassName("", enumName)

        fun getEnumSpec(config: CodeGenContext) = enumBuilder(enumName) {
            addAnnotation(config.annotationSerializable)
            primaryConstructor {
                addPrimaryConstructorProperty("origin", String::class) {
                    addModifiers(KModifier.OVERRIDE)
                }
                addSuperinterface(config.frappeEnum(enumName))
            }
            options.forEach { option ->
                addEnumConstant(name = option.toCamelCase(capitalized = true)) {
                    addAnnotation(config.annotationSerialName) { addMember("%S", option) }
                    addSuperclassConstructorParameter("%S", option)
                }
            }
            addObject(name = "NullableSerializer") {
                superclass(config.safeSerializer.parameterizedBy(enumType))
                addSuperclassConstructorParameter("%M()", MemberName(enumType, "serializer"))
            }
        }
    }

    data class Link(
        override val fieldName: String,
        override val label: String?,
        override val nullable: Boolean,
        override val required: Boolean,
        override val strictTyped: Boolean,
        override val originFieldType: FieldTypeRaw,
        val option: String,
    ) : DocField {
        fun getDocType(context: CodeGenContext) = context.docTypes[option]!!
        fun getLinkClassName(context: CodeGenContext) = getDocType(context).getLinkClassName(context)
        fun getLinkJsonTypeClassName(context: CodeGenContext) = getDocType(context).getLinkJsonTypeClassName(context)
        fun getLinkSerializerClassName(context: CodeGenContext) =
            getDocType(context).getLinkSerializerClassName(context, nullable = nullable)
    }

    data class Table(
        override val fieldName: String,
        override val label: String?,
        override val nullable: Boolean,
        override val required: Boolean,
        override val strictTyped: Boolean,
        override val originFieldType: FieldTypeRaw,
        val option: String,
    ) : DocField {
        val prettyChildName = option.toCamelCase(capitalized = true)
        fun getChildClassName(context: CodeGenContext) =
            context.docTypes[option]?.getClassName(context)
                ?: throw RuntimeException("child doctype '$option' not loaded")
    }
}

