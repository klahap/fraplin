package default_code

import io.github.goquati.kotlin.util.Failure
import io.github.goquati.kotlin.util.Result


typealias FraplinResult<T> = Result<T, FraplinError>

data class FraplinError(
    val status: Int,
    val msg: String,
) {
    val prettyMsg get() = "[${status.prettyStatus()}] $msg"
    val err: FraplinResult<Nothing> get() = Failure(this)

    companion object {
        fun unprocessable(msg: String) = FraplinError(422, msg = msg)
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
