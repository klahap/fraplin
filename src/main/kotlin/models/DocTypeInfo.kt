package de.frappe.dsl_gen.models

data class DocTypeInfo(
    val name: String,
    val strictTyped: Boolean = false,
) {
    init {
        assert(name.isNotBlank())
    }
}
