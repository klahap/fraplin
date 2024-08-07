package io.github.klahap.fraplin.models

import io.github.klahap.fraplin.util.BooleanAsIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


interface IDocFieldRaw {
    val parent: String
    val fieldName: String
    val label: String?
    val fieldType: FieldTypeRaw
    val notNullable: Boolean?
    val required: Boolean
    val options: String?

    fun toDocField(strictTyped: Boolean): DocField? = when (fieldType) {
        // string
        FieldTypeRaw.Data, FieldTypeRaw.Barcode, FieldTypeRaw.Code,
        FieldTypeRaw.Color, FieldTypeRaw.Signature, FieldTypeRaw.SmallText,
        FieldTypeRaw.LongText, FieldTypeRaw.Text, FieldTypeRaw.Autocomplete, FieldTypeRaw.JSON,
        FieldTypeRaw.ReadOnly, FieldTypeRaw.Geolocation, FieldTypeRaw.TextEditor,
        FieldTypeRaw.HTMLEditor, FieldTypeRaw.MarkdownEditor, FieldTypeRaw.HTML,
        FieldTypeRaw.Icon, FieldTypeRaw.Password -> DocField.Primitive(
            fieldName = fieldName,
            nullable = notNullable?.let { !it } ?: true,
            required = required,
            strictTyped = strictTyped,
            fieldType = DocField.Primitive.Type.STRING,
            originFieldType = fieldType,
        )

        FieldTypeRaw.Attach, FieldTypeRaw.AttachImage -> DocField.Attach(
            fieldName = fieldName,
            nullable = notNullable?.let { !it } ?: true,
            required = required,
            strictTyped = strictTyped,
            originFieldType = fieldType,
        )

        FieldTypeRaw.DateTime -> DocField.Primitive(
            fieldName = fieldName,
            nullable = notNullable?.let { !it } ?: true,
            required = required,
            strictTyped = strictTyped,
            fieldType = DocField.Primitive.Type.DATETIME,
            originFieldType = fieldType,
        )

        FieldTypeRaw.Date -> DocField.Primitive(
            fieldName = fieldName,
            nullable = notNullable?.let { !it } ?: true,
            required = required,
            strictTyped = strictTyped,
            fieldType = DocField.Primitive.Type.DATE,
            originFieldType = fieldType,
        )

        FieldTypeRaw.Time -> DocField.Primitive(
            fieldName = fieldName,
            nullable = notNullable?.let { !it } ?: true,
            required = required,
            strictTyped = strictTyped,
            fieldType = DocField.Primitive.Type.TIME,
            originFieldType = fieldType,
        )

        // float
        FieldTypeRaw.Currency, FieldTypeRaw.Float, FieldTypeRaw.Duration,
        FieldTypeRaw.Percent, FieldTypeRaw.Rating -> DocField.Primitive(
            fieldName = fieldName,
            nullable = notNullable?.let { !it } ?: true,
            required = required,
            strictTyped = strictTyped,
            fieldType = DocField.Primitive.Type.DOUBLE,
            originFieldType = fieldType,
        )

        // int
        FieldTypeRaw.Int -> DocField.Primitive(
            fieldName = fieldName,
            nullable = false,
            required = required,
            strictTyped = strictTyped,
            fieldType = DocField.Primitive.Type.INT,
            originFieldType = fieldType,
        )

        FieldTypeRaw.Check -> DocField.Check(
            fieldName = fieldName,
            nullable = false,
            required = required,
            strictTyped = strictTyped,
        )

        FieldTypeRaw.Select -> run {
            DocField.Select(
                fieldName = fieldName,
                nullable = notNullable?.let { !it } ?: true,
                required = required,
                strictTyped = strictTyped,
                options = options?.split('\n')
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.toSet() ?: return@run null,
            )
        }

        FieldTypeRaw.Link -> run {
            DocField.Link(
                fieldName = fieldName,
                nullable = notNullable?.let { !it } ?: true,
                required = required,
                strictTyped = strictTyped,
                option = options?.takeIf { it.isNotBlank() } ?: return@run null,
            )
        }

        FieldTypeRaw.Table -> run {
            DocField.Table(
                fieldName = fieldName,
                nullable = notNullable?.let { !it } ?: true,
                required = required,
                strictTyped = strictTyped,
                option = options?.takeIf { it.isNotBlank() } ?: return@run null,
            )
        }

        // TODO
        FieldTypeRaw.DynamicLink -> run {
            DocField.DynamicLink(
                fieldName = fieldName,
                nullable = notNullable?.let { !it } ?: true,
                required = required,
                strictTyped = strictTyped,
                option = options?.takeIf { it.isNotBlank() } ?: return@run null,
            )
        }

        // TODO
        FieldTypeRaw.TableMultiSelect -> null

        // null
        FieldTypeRaw.Button, FieldTypeRaw.Heading, FieldTypeRaw.ColumnBreak,
        FieldTypeRaw.SectionBreak, FieldTypeRaw.TabBreak,
        FieldTypeRaw.Note, FieldTypeRaw.Image -> null
    }
}


@Serializable
data class DocFieldRaw(
    @SerialName("parent") override val parent: String,
    @SerialName("fieldname") override val fieldName: String,
    @SerialName("label") override val label: String? = null,
    @SerialName("fieldtype") override val fieldType: FieldTypeRaw,
    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("reqd") override val required: Boolean,
    @SerialName("options") override val options: String? = null,
) : IDocFieldRaw {
    override val notNullable: Boolean = required
}


@Serializable
data class DocCustomFieldRaw(
    @SerialName("dt") override val parent: String,
    @SerialName("fieldname") override val fieldName: String,
    @SerialName("label") override val label: String? = null,
    @SerialName("fieldtype") override val fieldType: FieldTypeRaw,
    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("reqd") override val required: Boolean,
    @SerialName("options") override val options: String? = null,
) : IDocFieldRaw {
    override val notNullable: Boolean = required
}