package default_code.util

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
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
    computeIfAbsent(key) {
        CompletableFuture.supplyAsync { runBlocking { block(key) } }
    }.await()
}
