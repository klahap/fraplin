package io.github.klahap.fraplin.util

fun range(start: Int = 0, step: Int = 1) =
    generateSequence(start) { it + step }
