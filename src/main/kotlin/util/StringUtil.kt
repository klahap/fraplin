package io.github.klahap.fraplin.util

fun String.takeIfNotBlank() = takeIf { it.isNotBlank() }
