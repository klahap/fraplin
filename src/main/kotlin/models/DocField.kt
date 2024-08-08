package io.github.klahap.fraplin.models

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.klahap.fraplin.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed interface DocField {
    val fieldName: String
    val nullable: Nullable
    val required: Boolean
    val originFieldType: FieldTypeRaw
    val prettyFieldName
        get() = fieldName.toCamelCase(capitalized = false)
            .let { if (it in kotlinKeywords) "`$it`" else it }

    enum class Nullable {
        TRUE,
        FALSE_CONDITIONALLY, // because of mandatory is not mandatory in Frappe -.-
        FALSE;

        companion object {
            fun get(nullable: Boolean, strictTyped: Boolean) = Nullable.entries.single {
                when (it) {
                    TRUE -> nullable
                    FALSE_CONDITIONALLY -> !nullable && !strictTyped
                    FALSE -> !nullable && strictTyped
                }
            }
        }
    }


    @Serializable
    @SerialName("Primitive")
    data class Primitive(
        override val fieldName: String,
        override val nullable: Nullable,
        override val required: Boolean,
        override val originFieldType: FieldTypeRaw,
        val fieldType: Type,
    ) : DocField {
        enum class Type {
            STRING, INT, DOUBLE, DATE, DATETIME, TIME
        }
    }

    @Serializable
    @SerialName("Attach")
    data class Attach(
        override val fieldName: String,
        override val nullable: Nullable,
        override val required: Boolean,
        override val originFieldType: FieldTypeRaw,
    ) : DocField

    @Serializable
    @SerialName("Check")
    data class Check(
        override val fieldName: String,
        override val nullable: Nullable,
        override val required: Boolean,
    ) : DocField {
        override val originFieldType = FieldTypeRaw.Check
    }

    @Serializable
    @SerialName("Select")
    data class Select(
        override val fieldName: String,
        override val nullable: Nullable,
        override val required: Boolean,
        val options: Set<String>,
    ) : DocField {
        override val originFieldType = FieldTypeRaw.Select
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

    @Serializable
    @SerialName("Link")
    data class Link(
        override val fieldName: String,
        override val nullable: Nullable,
        override val required: Boolean,
        @Serializable(with = DocTypeNameSerializer::class)
        val option: DocType.Name,
    ) : DocField {
        override val originFieldType = FieldTypeRaw.Link
        fun getDocType(context: CodeGenContext) = context.docTypes[option]!!
        fun getLinkClassName(context: CodeGenContext) = getDocType(context).getLinkClassName(context)
        fun getLinkJsonTypeClassName(context: CodeGenContext) = getDocType(context).getLinkJsonTypeClassName(context)
        fun getLinkSerializerClassName(context: CodeGenContext) =
            getDocType(context).getLinkSerializerClassName(context, nullable = nullable)
    }

    @Serializable
    @SerialName("Table")
    data class Table(
        override val fieldName: String,
        override val nullable: Nullable,
        override val required: Boolean,
        @Serializable(with = DocTypeNameSerializer::class)
        val option: DocType.Name,
    ) : DocField {
        override val originFieldType = FieldTypeRaw.Table
        val prettyChildName = option.value.toCamelCase(capitalized = true)
        fun getChildClassName(context: CodeGenContext) =
            context.docTypes[option]?.getClassName(context)
                ?: throw RuntimeException("child doctype '$option' not loaded")
    }

    @Serializable
    @SerialName("DynamicLink")
    data class DynamicLink(
        override val fieldName: String,
        override val nullable: Nullable,
        override val required: Boolean,
        val option: String,
    ) : DocField {
        override val originFieldType = FieldTypeRaw.DynamicLink
    }

    @Serializable
    @SerialName("DocStatus")
    data object DocStatus : DocField {
        override val fieldName = "docstatus"
        override val nullable = Nullable.FALSE
        override val required = false
        override val originFieldType = FieldTypeRaw.Int
    }
}
