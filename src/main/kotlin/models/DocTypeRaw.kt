package de.frappe.dsl_gen.models

import de.frappe.dsl_gen.util.BooleanAsIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class DocTypeRaw(
    @SerialName("name") val name: String,
    @SerialName("module") val module: String,
    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("issingle") val isSingle: Boolean,
    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("istable") val isTable: Boolean,
) {
    private val type = DocType.Type.values().single {
        when (it) {
            DocType.Type.NORMAL -> !isSingle && !isTable
            DocType.Type.SINGLE -> isSingle
            DocType.Type.CHILD -> isTable
        }
    }

    fun toDocType(fields: List<IDocFieldRaw>, additionalInfo: DocTypeInfo? = null): DocType {
        val isStrictTyped = additionalInfo?.strictTyped ?: false
        return DocType(
            module = module,
            docTypeName = name,
            docTypeType = type,
            fields = sequenceOf(
                getDefaultFields(type),
                fields.mapNotNull { it.toDocField(strictTyped = isStrictTyped) },
            ).flatten().toList()
                .distinctBy { it.fieldName }
                .sortedBy { it.fieldName }
        )
    }


    companion object {
        private data class DefaultField(
            val name: String,
            val type: DocField.Primitive.Type,
            val originalType: FieldTypeRaw,
        )

        private fun getDefaultFields(type: DocType.Type) = listOfNotNull(
            DefaultField("name", DocField.Primitive.Type.STRING, FieldTypeRaw.Data),
            DefaultField("owner", DocField.Primitive.Type.STRING, FieldTypeRaw.Data),
            DefaultField("creation", DocField.Primitive.Type.DATETIME, FieldTypeRaw.DateTime)
                .takeIf { type != DocType.Type.SINGLE },
            DefaultField("modified", DocField.Primitive.Type.DATETIME, FieldTypeRaw.DateTime),
            DefaultField("modified_by", DocField.Primitive.Type.STRING, FieldTypeRaw.Data),
            DefaultField("docstatus", DocField.Primitive.Type.INT, FieldTypeRaw.Int),
            DefaultField("idx", DocField.Primitive.Type.INT, FieldTypeRaw.Int),
        ).map {
            DocField.Primitive(
                fieldName = it.name,
                label = null,
                nullable = false,
                required = false,
                strictTyped = true,
                fieldType = it.type,
                originFieldType = it.originalType,
            )
        }
    }
}