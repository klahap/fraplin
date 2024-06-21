package default_code

import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


sealed interface DocTypeAbility {
    sealed interface Query
    sealed interface Create
    sealed interface Update
    sealed interface Delete
    sealed interface Read
}

sealed interface DocType {
    interface Normal : DocType, DocTypeAbility.Query, DocTypeAbility.Create, DocTypeAbility.Update,
        DocTypeAbility.Delete, DocTypeAbility.Read

    interface Single : DocType, DocTypeAbility.Update, DocTypeAbility.Read

    interface Child : DocType, DocTypeAbility.Query, DocTypeAbility.Create, DocTypeAbility.Update,
        DocTypeAbility.Delete, DocTypeAbility.Read
}


interface DocTypeBuilder<D: DocType, T> {
    operator fun get(field: String): JsonElement?
    operator fun set(field: String, value: JsonElement)
    fun remove(field: String): JsonElement?
    fun clear()
    fun build(): JsonElement
}

interface FrappeEnum<T> where T : Enum<T>, T : FrappeEnum<T> {
    val origin: String

    companion object {
        fun <T> originValueOf(clazz: KClass<T>, origin: String): T where T : Enum<T>, T : FrappeEnum<T> {
            return clazz.java.enumConstants
                ?.firstOrNull { it.origin == origin }
                ?: throw Exception("origin value '$origin' not exists in ${clazz.simpleName}")
        }
    }
}