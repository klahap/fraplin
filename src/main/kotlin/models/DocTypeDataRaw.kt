package io.github.klahap.fraplin.models


data class DocTypeDataRaw(
    val docTypes: Collection<DocTypeRaw> = emptyList(),
    val docFields: Collection<DocFieldRaw> = emptyList(),
) {
    operator fun plus(other: DocTypeDataRaw) = DocTypeDataRaw(
        docTypes = docTypes + other.docTypes,
        docFields = docFields + other.docFields,
    )

    fun merge(additionalInfo: Set<DocTypeInfo>): Collection<DocType.Full> {
        val additionalInfoMap = additionalInfo.associateBy { it.docTypeName }
        val groupedFields = docFields.groupBy { it.parent }
        return docTypes.map { docType ->
            val fields = groupedFields[docType.name] ?: emptyList()
            val info = additionalInfoMap[docType.name]
            docType.toDocType(fields = fields, additionalInfo = info)
        }
    }

    companion object {
        fun Iterable<DocTypeDataRaw>.sum(): DocTypeDataRaw =
            fold(DocTypeDataRaw()) { acc, it -> acc + it }
    }
}
