package default_code.util

fun String.takeIfNotBlank() = takeIf { it.isNotBlank() }
