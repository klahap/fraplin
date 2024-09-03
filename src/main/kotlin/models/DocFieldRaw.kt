package io.github.klahap.fraplin.models

import io.github.klahap.fraplin.util.BooleanAsIntSerializer
import io.github.klahap.fraplin.util.DocTypeNameSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


interface DocFieldRaw {
    val parent: DocType.Name
    val fieldName: String
    val fieldType: FieldTypeRaw
    val required: Boolean
    val options: String?
    val notNullable: Boolean get() = required

    fun toDocField(parent: DocTypeRaw, strictTyped: Boolean): DocField? = when (fieldType) {
        // string
        FieldTypeRaw.Data, FieldTypeRaw.Barcode, FieldTypeRaw.Code,
        FieldTypeRaw.Color, FieldTypeRaw.Signature, FieldTypeRaw.SmallText,
        FieldTypeRaw.LongText, FieldTypeRaw.Text, FieldTypeRaw.Autocomplete, FieldTypeRaw.JSON,
        FieldTypeRaw.ReadOnly, FieldTypeRaw.Geolocation, FieldTypeRaw.TextEditor,
        FieldTypeRaw.HTMLEditor, FieldTypeRaw.MarkdownEditor, FieldTypeRaw.HTML,
        FieldTypeRaw.Icon, FieldTypeRaw.Password -> DocField.Primitive(
            fieldName = fieldName,
            nullable = DocField.Nullable.get(nullable = !notNullable, strictTyped = strictTyped),
            required = required,
            fieldType = DocField.Primitive.Type.STRING,
            originFieldType = fieldType,
        )

        FieldTypeRaw.Attach, FieldTypeRaw.AttachImage -> DocField.Attach(
            fieldName = fieldName,
            nullable = DocField.Nullable.get(nullable = !notNullable, strictTyped = strictTyped),
            required = required,
            originFieldType = fieldType,
        )

        FieldTypeRaw.DateTime -> DocField.Primitive(
            fieldName = fieldName,
            nullable = DocField.Nullable.get(nullable = !notNullable, strictTyped = strictTyped),
            required = required,
            fieldType = DocField.Primitive.Type.DATETIME,
            originFieldType = fieldType,
        )

        FieldTypeRaw.Date -> DocField.Primitive(
            fieldName = fieldName,
            nullable = DocField.Nullable.get(nullable = !notNullable, strictTyped = strictTyped),
            required = required,
            fieldType = DocField.Primitive.Type.DATE,
            originFieldType = fieldType,
        )

        FieldTypeRaw.Time -> DocField.Primitive(
            fieldName = fieldName,
            nullable = DocField.Nullable.get(nullable = !notNullable, strictTyped = strictTyped),
            required = required,
            fieldType = DocField.Primitive.Type.TIME,
            originFieldType = fieldType,
        )

        // float
        FieldTypeRaw.Currency, FieldTypeRaw.Float, FieldTypeRaw.Duration,
        FieldTypeRaw.Percent, FieldTypeRaw.Rating -> DocField.Primitive(
            fieldName = fieldName,
            nullable = DocField.Nullable.get(nullable = !notNullable, strictTyped = strictTyped),
            required = required,
            fieldType = DocField.Primitive.Type.DOUBLE,
            originFieldType = fieldType,
        )

        // int
        FieldTypeRaw.Int -> DocField.Primitive(
            fieldName = fieldName,
            nullable = DocField.Nullable.get(nullable = false, strictTyped = strictTyped),
            required = required,
            fieldType = DocField.Primitive.Type.INT,
            originFieldType = fieldType,
        )

        FieldTypeRaw.Check -> DocField.Check(
            fieldName = fieldName,
            nullable = DocField.Nullable.get(nullable = false, strictTyped = strictTyped),
            required = required,
        )

        FieldTypeRaw.Select -> run {
            DocField.Select(
                fieldName = fieldName,
                nullable = DocField.Nullable.get(nullable = !notNullable, strictTyped = strictTyped),
                required = required,
                parentName = parent.name,
                options = options?.split('\n')
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.toSet() ?: return@run null,
            )
        }

        FieldTypeRaw.Link -> run {
            DocField.Link(
                fieldName = fieldName,
                nullable = DocField.Nullable.get(nullable = !notNullable, strictTyped = strictTyped),
                required = required,
                option = options?.takeIf { it.isNotBlank() }?.let { DocType.Name(it) } ?: return@run null,
            )
        }

        FieldTypeRaw.Table, FieldTypeRaw.TableMultiSelect -> run {
            DocField.Table(
                fieldName = fieldName,
                nullable = DocField.Nullable.get(nullable = !notNullable, strictTyped = strictTyped),
                required = required,
                option = options?.takeIf { it.isNotBlank() }?.let { DocType.Name(it) } ?: return@run null,
            )
        }

        // TODO
        FieldTypeRaw.DynamicLink -> run {
            DocField.DynamicLink(
                fieldName = fieldName,
                nullable = DocField.Nullable.get(nullable = !notNullable, strictTyped = strictTyped),
                required = required,
                option = options?.takeIf { it.isNotBlank() } ?: return@run null,
            )
        }

        // null
        FieldTypeRaw.Button, FieldTypeRaw.Heading, FieldTypeRaw.ColumnBreak,
        FieldTypeRaw.SectionBreak, FieldTypeRaw.TabBreak,
        FieldTypeRaw.Note, FieldTypeRaw.Image -> null
    }


    @Serializable
    data class Common(
        @SerialName("parent") override val parent: DocType.Name,
        @SerialName("fieldname") override val fieldName: String,
        @SerialName("fieldtype") override val fieldType: FieldTypeRaw,
        @Serializable(with = BooleanAsIntSerializer::class)
        @SerialName("reqd") override val required: Boolean,
        @SerialName("options") override val options: String? = null,
    ) : DocFieldRaw

    @Serializable
    data class Custom(
        @SerialName("dt") override val parent: DocType.Name,
        @SerialName("fieldname") override val fieldName: String,
        @SerialName("fieldtype") override val fieldType: FieldTypeRaw,
        @Serializable(with = BooleanAsIntSerializer::class)
        @SerialName("reqd") override val required: Boolean,
        @SerialName("options") override val options: String? = null,
    ) : DocFieldRaw
}
