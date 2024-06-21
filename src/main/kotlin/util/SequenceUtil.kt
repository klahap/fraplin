package de.frappe.dsl_gen.util

fun range(start: Int = 0, step: Int = 1) =
    generateSequence(start) { it + step }
