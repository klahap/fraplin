package default_code

import io.github.goquati.kotlin.util.Failure
import io.github.goquati.kotlin.util.Result

typealias FraplinResult<T> = Result<T, FraplinException>

data class FraplinException(
    val status: Int,
    val msg: String,
) : Exception("${status.prettyStatus()}: $msg") {
    val result: FraplinResult<Nothing> get() = Failure(this)

    companion object {
        fun unprocessable(msg: String) = FraplinException(422, msg)
        private fun Int.prettyStatus() = when (this) {
            404 -> "frappe object not found"
            409 -> "frappe conflict"
            422 -> "unprocessable frappe response"
            500 -> "internal frappe error"
            in 400..499 -> "frappe client error"
            in 500..599 -> "frappe server error"
            else -> null
        }?.let { "$this, $it" } ?: "$this"
    }
}
