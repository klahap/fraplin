package default_code.util

import io.github.goquati.kotlin.util.Failure
import io.github.goquati.kotlin.util.Result
import io.github.goquati.kotlin.util.Success
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext


suspend fun <T, R> Iterable<T>.runParallel(
    context: CoroutineContext? = null,
    block: suspend (T) -> R,
) = coroutineScope {
    if (context == null)
        this@runParallel.map { async { block(it) } }.awaitAll()
    else
        withContext(context) {
            this@runParallel.map { async { block(it) } }.awaitAll()
        }
}

suspend fun <K, V> ConcurrentHashMap<K, CompletableFuture<V>>.getValueForKey(
    key: K,
    block: suspend (K) -> V,
): V = withContext(Dispatchers.IO) {
    val scope = this
    computeIfAbsent(key) {
        scope.future { block(key) }
    }.await()
}

suspend fun <T, E> Flow<Result<T, E>>.toResultList(destination: MutableList<T> = ArrayList()): Result<List<T>, E> =
    toResultCollection(destination)

suspend fun <T, E> Flow<Result<T, E>>.toResultSet(destination: MutableSet<T> = LinkedHashSet()): Result<Set<T>, E> =
    toResultCollection(destination)

suspend fun <T, E, C : MutableCollection<in T>> Flow<Result<T, E>>.toResultCollection(destination: C): Result<C, E> {
    var error: E? = null
    val result = takeWhile {
        if (it.isFailure) error = it.failure
        !it.isFailure
    }.map { it.success }.toCollection(destination)
    return error?.let { Failure(it) } ?: Success(result)
}

fun <T> Flow<Result<T, *>>.filterValue(): Flow<T> = mapNotNull { if (it.isSuccess) it.success else null }
fun <E> Flow<Result<*, E>>.filterError(): Flow<E> = mapNotNull { if (it.isFailure) it.failure else null }
