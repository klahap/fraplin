package com.fraplin.models

import com.fraplin.util.SafeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class FieldTypeRaw {
    Link,
    Data,

    @SerialName("Dynamic Link")
    DynamicLink,
    Check,
    Select,
    Table,
    Attach,

    @SerialName("Attach Image")
    AttachImage,

    @SerialName("Text Editor")
    TextEditor,

    @SerialName("Datetime")
    DateTime,
    Date,
    Time,
    Barcode,
    Button,
    Code,
    Color,
    Heading,

    @SerialName("Column Break")
    ColumnBreak,
    Currency,
    Float,
    Geolocation,

    @SerialName("HTML Editor")
    HTMLEditor,
    HTML,
    Image,
    Icon,
    Int,
    Autocomplete,

    @SerialName("Small Text")
    SmallText,

    @SerialName("Long Text")
    LongText,
    Text,

    @SerialName("Markdown Editor")
    MarkdownEditor,
    Password,
    Percent,
    Rating,

    @SerialName("Read Only")
    ReadOnly,

    @SerialName("Section Break")
    SectionBreak,

    @SerialName("Tab Break")
    TabBreak,
    Note,
    Signature,

    @SerialName("Table MultiSelect")
    TableMultiSelect,
    Duration,
    JSON;

    object NullableSerializer : SafeSerializer<FieldTypeRaw>(FieldTypeRaw.serializer())
}
