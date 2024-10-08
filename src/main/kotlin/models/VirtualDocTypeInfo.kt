package io.github.klahap.fraplin.models

import kotlinx.serialization.Serializable

enum class EndpointType { GET, LIST, DELETE, UPDATE, CREATE }
enum class DataType { GET, UPDATE, CREATE }

@Serializable
data class VirtualDocTypeInfo(
    val name: String,
    val strictTyped: Boolean = false,
    val ignoreFields: Map<DataType, List<String>>,
    val ignoreEndpoints: List<EndpointType>,
) : Comparable<VirtualDocTypeInfo> {
    val docTypeName get() = DocType.Name(name)

    val docTypeInfo
        get() = DocTypeInfo(
            name = name,
            strictTyped = strictTyped,
        )

    override fun compareTo(other: VirtualDocTypeInfo) = name.compareTo(other.name)

    init {
        assert(name.isNotBlank())
    }

    class Builder(
        val name: String,
        val strictTyped: Boolean = false,
    ) {
        private val ignoredFields = mutableMapOf<DataType, Set<String>>()
        private val ignoredEndpoints = mutableSetOf<EndpointType>()

        fun ignoreEndpoints(type: EndpointType, vararg types: EndpointType) {
            ignoredEndpoints += setOf(type) + types
        }

        fun ignoreEndpoints(types: Iterable<EndpointType>) {
            ignoredEndpoints += types
        }

        fun ignoreFields(type: DataType, name: String, vararg names: String) {
            ignoreFields(type = type, names = setOf(name) + names)
        }

        fun ignoreFields(name: String, vararg names: String) {
            ignoreFields(names = setOf(name) + names)
        }

        fun ignoreFields(type: DataType, names: Iterable<String>) {
            ignoredFields.compute(type) { _, old -> (old ?: emptySet()) + names }
        }

        fun ignoreFields(names: Iterable<String>) {
            DataType.entries.forEach { ignoreFields(type = it, names = names) }
        }

        fun build(): VirtualDocTypeInfo {
            val validDataTypes = getValidDataTypes(ignoreEndpoints = ignoredEndpoints, ignoreFields = ignoredFields)
            return VirtualDocTypeInfo(
                name = name,
                strictTyped = strictTyped,
                ignoreFields = ignoredFields
                    .filterKeys { it in validDataTypes }
                    .mapValues { it.value.sorted() }
                    .toSortedMap(),
                ignoreEndpoints = ignoredEndpoints.sorted(),
            )
        }
    }

    companion object {
        fun Map<DataType, Collection<String>>.isEqualToBaseType(type: DataType) = when (type) {
            DataType.GET -> true
            DataType.UPDATE -> get(type)?.toSet() == get(DataType.GET)?.toSet()
            DataType.CREATE -> get(type)?.toSet() == get(DataType.GET)?.toSet()
        }

        fun getValidDataTypes(
            ignoreEndpoints: Collection<EndpointType>,
            ignoreFields: Map<DataType, Collection<String>>,
        ) = EndpointType.values()
            .filter { !ignoreEndpoints.contains(it) }
            .toSet().flatMap { endpointType ->
                when (endpointType) {
                    EndpointType.GET -> listOf(DataType.GET)
                    EndpointType.LIST -> listOf(DataType.GET)
                    EndpointType.DELETE -> emptyList()
                    EndpointType.UPDATE -> listOfNotNull(
                        DataType.UPDATE.takeIf { !ignoreFields.isEqualToBaseType(it) },
                        DataType.GET
                    )

                    EndpointType.CREATE -> listOfNotNull(
                        DataType.CREATE.takeIf { !ignoreFields.isEqualToBaseType(it) },
                        DataType.GET
                    )
                }
            }.toSet()
    }
}
