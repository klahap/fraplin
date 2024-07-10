package io.github.klahap.fraplin.models

import io.github.klahap.fraplin.util.BooleanAsIntSerializer
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
        private fun getDefaultFields(type: DocType.Type) = buildList {
            add("name", DocField.Primitive.Type.STRING, FieldTypeRaw.Data)
            add("owner", DocField.Primitive.Type.STRING, FieldTypeRaw.Data)
            if (type != DocType.Type.SINGLE)
                add("creation", DocField.Primitive.Type.DATETIME, FieldTypeRaw.DateTime)
            add("modified", DocField.Primitive.Type.DATETIME, FieldTypeRaw.DateTime)
            add("modified_by", DocField.Primitive.Type.STRING, FieldTypeRaw.Data)
            add(DocField.DocStatus)
            add("idx", DocField.Primitive.Type.INT, FieldTypeRaw.Int)
        }

        private fun MutableList<DocField>.add(
            name: String,
            type: DocField.Primitive.Type,
            originalType: FieldTypeRaw,
        ) = add(
            DocField.Primitive(
                fieldName = name,
                label = null,
                nullable = false,
                required = false,
                strictTyped = true,
                fieldType = type,
                originFieldType = originalType,
            )
        )
    }
}