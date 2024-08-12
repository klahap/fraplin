package io.github.klahap.fraplin.util

import io.github.klahap.fraplin.models.DocField
import io.github.klahap.fraplin.models.OpenApiGenContext
import io.github.klahap.fraplin.models.openapi.Component
import io.github.klahap.fraplin.models.openapi.Schema

fun DocField.toOpenApiSchema(context: OpenApiGenContext): Schema {
    val isNullable = when (nullable) {
        DocField.Nullable.TRUE -> true
        DocField.Nullable.FALSE_CONDITIONALLY -> false
        DocField.Nullable.FALSE -> false
    }
    return when (this) {
        is DocField.Attach -> Schema.Primitive(type = "string", nullable = isNullable)
        is DocField.Check -> Schema.Primitive(type = "boolean", nullable = isNullable)
        DocField.DocStatus -> Schema.Ref(Component.Ref(getDocStatusEnumName(context)))
        is DocField.DynamicLink -> Schema.Primitive(type = "string", nullable = isNullable)
        is DocField.Link -> Schema.Primitive(type = "string", nullable = isNullable)
        is DocField.Primitive -> Schema.Primitive(
            type = when (fieldType) {
                DocField.Primitive.Type.STRING -> "string"
                DocField.Primitive.Type.INT -> "integer"
                DocField.Primitive.Type.DOUBLE -> "number"
                DocField.Primitive.Type.DATE, DocField.Primitive.Type.DATETIME, DocField.Primitive.Type.TIME -> "string"
            },
            format = when (fieldType) {
                DocField.Primitive.Type.STRING -> null
                DocField.Primitive.Type.INT -> "int64"
                DocField.Primitive.Type.DOUBLE -> "double"
                DocField.Primitive.Type.DATE -> "date"
                DocField.Primitive.Type.DATETIME -> "date-time"
                DocField.Primitive.Type.TIME -> null
            },
            nullable = isNullable,
        )

        is DocField.Select -> Schema.Ref(Component.Ref(getOpenApiEnumName(context)))
        is DocField.Table -> Schema.Primitive(type = "string", nullable = isNullable)
    }
}

fun DocField.Select.getOpenApiSpecEnum(context: OpenApiGenContext) = Component.StringEnum(
    name = getOpenApiEnumName(context),
    values = prettyOptions,
)

private fun getDocStatusEnumName(context: OpenApiGenContext) = "${context.schemaPrefix}DocStatus"
fun DocField.DocStatus.getOpenApiSpecEnum(context: OpenApiGenContext) = Component.IntEnum(
    name = getDocStatusEnumName(context),
    values = mapOf(
        "DRAFT" to 0,
        "SUBMITTED" to 1,
        "CANCELLED" to 2,
    ),
)