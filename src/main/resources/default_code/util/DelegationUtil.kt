package default_code.util

import default_code.DocTypeBuilder
import kotlin.reflect.KProperty

class DocFieldBuilderDelegation<T : Any, B : DocTypeBuilder<*, B>>(
    private val fieldName: String,
    private val serializer: JsonElementType<T>,
) {
    operator fun getValue(thisRef: DocTypeBuilder<*, B>, property: KProperty<*>): T? {
        return thisRef[fieldName]
            ?.let { serializer.toValue(it) }
    }

    operator fun setValue(thisRef: DocTypeBuilder<*, B>, property: KProperty<*>, value: T?) {
        thisRef[fieldName] = serializer.toJson(value)
    }
}