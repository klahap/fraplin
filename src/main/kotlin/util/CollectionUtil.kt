package io.github.klahap.fraplin.util

fun <T> Collection<Collection<T>>.intersectAll(): Set<T> =
    fold(flatten().toSet()) { x, y -> x.intersect(y.toSet()) }
