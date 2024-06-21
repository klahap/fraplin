package default_code

import default_code.model.FrappeFieldType


@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FrappeField(val type: FrappeFieldType)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FrappeTableField(val childDocTypeName: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FrappeDocType(val docTypeName: String)
