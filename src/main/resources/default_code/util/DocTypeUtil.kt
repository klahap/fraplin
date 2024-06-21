package default_code.util

import default_code.DocType
import default_code.DocTypeBuilder
import default_code.FrappeDocType
import default_code.FrappeTableField as FrappeTableFieldAnnotation
import default_code.model.FrappeDocTypeName
import default_code.model.FrappeFieldName
import default_code.model.FrappeTableField
import kotlinx.serialization.SerialName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

fun <T : DocType> KClass<T>.getDocTypeName() =
    FrappeDocTypeName(findAnnotation<FrappeDocType>()!!.docTypeName)

fun <T : DocType> KClass<T>.getTableFields() = memberProperties.mapNotNull { prop ->
    FrappeTableField(
        childDocType = FrappeDocTypeName(
            prop.findAnnotation<FrappeTableFieldAnnotation>()?.childDocTypeName ?: return@mapNotNull null
        ),
        fieldName = FrappeFieldName(prop.findAnnotation<SerialName>()!!.value),
    )
}.toSet()

inline fun <reified T : DocType> getDocTypeName() = T::class.getDocTypeName()
inline fun <reified T : DocType> getTableFields() = T::class.getTableFields()

@get:JvmName("DocType_frappeName")
val <T : DocType> KProperty1<T, *>.frappeName get() = findAnnotation<SerialName>()!!.value

@get:JvmName("DocTypeBuilder_frappeName")
val <T : DocTypeBuilder<*, T>> KProperty1<T, *>.frappeName get() = findAnnotation<SerialName>()!!.value
