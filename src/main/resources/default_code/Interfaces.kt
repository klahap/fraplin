package default_code

import default_code.util.JsonElementField
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass


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

    interface Dummy : DocType
}


interface DocTypeBuilder<D : DocType, T> {
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

sealed interface IWhiteListFun {
    val name: String

    sealed interface Args : IWhiteListFun {
        interface Without : Args
        interface With<A : JsonElementField<*>> : Args {
            public fun getArgs(block: A.() -> JsonElement): Map<String, JsonElement>
        }
    }

    sealed interface Scope : IWhiteListFun {
        interface Private : Scope
        interface Public : Scope
    }
}
