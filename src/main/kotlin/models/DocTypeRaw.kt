package io.github.klahap.fraplin.models

import io.github.klahap.fraplin.util.BooleanAsIntSerializer
import io.github.klahap.fraplin.util.DocTypeNameSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class DocTypeRaw(
    @SerialName("name") val name: DocType.Name,
    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("issingle") val isSingle: Boolean = false,
    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("istable") val isTable: Boolean = false,
    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("is_virtual") val isVirtual: Boolean = false,
) {
    private val type = DocType.Type.entries.single {
        when (it) {
            DocType.Type.NORMAL -> !isSingle && !isTable
            DocType.Type.SINGLE -> isSingle && !isTable
            DocType.Type.CHILD -> isTable && !isSingle
        }
    }

    fun toDocType(fields: List<DocFieldRaw>, additionalInfo: DocTypeInfo? = null): DocType.Full {
        val isStrictTyped = additionalInfo?.strictTyped ?: false
        val docFields = sequenceOf(
            getDefaultFields(type),
            fields.mapNotNull { it.toDocField(parent = this, strictTyped = isStrictTyped) },
        ).flatten().toList()
            .distinctBy { it.fieldName }
            .sortedBy { it.fieldName }

        return if (isVirtual) DocType.Virtual(
            docTypeName = name,
            fields = docFields,
        ) else DocType.Base(
            docTypeName = name,
            docTypeType = type,
            fields = docFields,
        )
    }


    companion object {
        private fun getDefaultFields(type: DocType.Type) = buildList {
            add("name", DocField.Primitive.Type.STRING, FieldTypeRaw.Data)
            add(
                DocField.Link(
                    fieldName = "owner",
                    nullable = DocField.Nullable.FALSE,
                    required = false,
                    option = DocType.Name("User"),
                )
            )
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
                nullable = DocField.Nullable.FALSE,
                required = false,
                fieldType = type,
                originFieldType = originalType,
            )
        )
    }
}