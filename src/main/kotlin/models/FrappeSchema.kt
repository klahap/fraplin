package io.github.klahap.fraplin.models

data class FrappeSchema(
    val docTypes: Collection<DocType.Full>,
    val whiteListFunctions: Collection<WhiteListFunction>,
) {

    data class Collector(
        val docTypes: Collection<DocTypeRaw> = emptyList(),
        val docFields: Collection<DocFieldRaw> = emptyList(),
        val whiteListFunctions: Collection<WhiteListFunction> = emptyList(),
    ) {
        operator fun plus(other: Collector) = Collector(
            docTypes = docTypes + other.docTypes,
            docFields = docFields + other.docFields,
            whiteListFunctions = whiteListFunctions + other.whiteListFunctions,
        )

        fun collect(additionalInfo: Set<DocTypeInfo>) = FrappeSchema(
            docTypes = mergeDocTypes(additionalInfo),
            whiteListFunctions = whiteListFunctions,
        )

        private fun mergeDocTypes(additionalInfo: Set<DocTypeInfo>): Collection<DocType.Full> {
            val additionalInfoMap = additionalInfo.associateBy { it.docTypeName }
            val groupedFields = docFields.groupBy { it.parent }
            return docTypes.map { docType ->
                val fields = groupedFields[docType.name] ?: emptyList()
                val info = additionalInfoMap[docType.name]
                docType.toDocType(fields = fields, additionalInfo = info)
            }
        }

        companion object {
            fun Iterable<Collector>.sum(): Collector =
                fold(Collector()) { acc, it -> acc + it }
        }
    }
}
