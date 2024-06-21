package default_code.model

import default_code.DocType
import kotlin.reflect.KClass

@JvmInline
value class FrappeDocTypeName(val name: String) {
    override fun toString() = name
}

@JvmInline
value class FrappeDocTypeObjectName(val name: String) {
    override fun toString() = name
}

@JvmInline
value class FrappeFieldName(val name: String) {
    override fun toString() = name
}

data class FrappeTableField(
    val childDocType: FrappeDocTypeName,
    val fieldName: FrappeFieldName,
)

sealed interface FrappeInlineStringField {
    val value: String
}

interface FrappeLinkField<T: DocType> : FrappeInlineStringField {
    val docType: KClass<T>
}

@JvmInline
value class FrappeAttachField(override val value: String) : FrappeInlineStringField
