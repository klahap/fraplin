package io.github.klahap.fraplin.models

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.klahap.fraplin.models.config.FraplinOutputConfig

data class CodeGenContext(
    val config: FraplinOutputConfig,
    val docTypes: Map<DocType.Name, DocType>
) {
    val outputPath get() = config.path
    val packageName get() = config.packageName

    private val packageNameModel = "$packageName.model"
    private val packageNameUtil = "$packageName.util"
    private val packageNameService = "$packageName.service"

    val notFoundException = ClassName(packageNameUtil, "HttpException", "ClientError", "NotFound")
    val conflictException = ClassName(packageNameUtil, "HttpException", "ClientError", "Conflict")

    val annotationSerialName = ClassName("kotlinx.serialization", "SerialName")
    val annotationSerializable = ClassName("kotlinx.serialization", "Serializable")
    val annotationFrappeField = ClassName(packageName, "FrappeField")
    val annotationFrappeTableField = ClassName(packageName, "FrappeTableField")
    val annotationFrappeDocType = ClassName(packageName, "FrappeDocType")

    val safeSerializer = ClassName(packageNameUtil, "SafeSerializer")
    val booleanAsIntSerializer = ClassName(packageNameUtil, "BooleanAsIntSerializer")
    val frappeDocStatusSerializer = ClassName(packageNameUtil, "FrappeDocStatusSerializer")
    val frappeInlineStringFieldSerializer = ClassName(packageNameUtil, "FrappeInlineStringFieldSerializer")
    val frappeInlineStringFieldNullableSerializer =
        ClassName(packageNameUtil, "FrappeInlineStringFieldNullableSerializer")
    val localDateSerializer = ClassName(packageNameUtil, "LocalDateSerializer")
    val localDateTimeSerializer = ClassName(packageNameUtil, "LocalDateTimeSerializer")
    val localTimeSerializer = ClassName(packageNameUtil, "LocalTimeSerializer")
    val localDateNullableSerializer = ClassName(packageNameUtil, "LocalDateNullableSerializer")
    val localDateTimeNullableSerializer = ClassName(packageNameUtil, "LocalDateTimeNullableSerializer")
    val localTimeNullableSerializer = ClassName(packageNameUtil, "LocalTimeNullableSerializer")
    val docFieldBuilderDelegation = ClassName(packageNameUtil, "DocFieldBuilderDelegation")
    val jsonElementType = ClassName(packageNameUtil, "JsonElementType")

    val frappeSiteService = ClassName(packageNameService, "FrappeSiteService")

    fun frappeEnum(enumName: String) =
        ClassName(packageName, "FrappeEnum").parameterizedBy(ClassName("", enumName))

    val frappeFieldType = ClassName(packageNameModel, "FrappeFieldType")
    val frappeDocStatus = ClassName(packageNameModel, "FrappeDocStatus")
    val frappeLinkField = ClassName(packageNameModel, "FrappeLinkField")
    val frappeAttachField = ClassName(packageNameModel, "FrappeAttachField")
    fun getFrappeRequestOptions(docType: ClassName) =
        ClassName(packageNameModel, "FrappeRequestOptions", "Builder").parameterizedBy(docType)

    fun getFrappeDocTableBuilder(docType: ClassName) = ClassName(packageNameModel, "FrappeDocTableBuilder")
        .parameterizedBy(docType, docType.nestedClass("Builder"))

    private val frappeDocTypeInterface = ClassName(packageName, "DocType")
    val docTypeBuilder = ClassName(packageName, "DocTypeBuilder")

    fun getFrappeDocType(docTypeType: DocType.Type) = when (docTypeType) {
        DocType.Type.NORMAL -> frappeDocTypeInterface.nestedClass("Normal")
        DocType.Type.SINGLE -> frappeDocTypeInterface.nestedClass("Single")
        DocType.Type.CHILD -> frappeDocTypeInterface.nestedClass("Child")
    }

    val frappeDummyDocType get() = frappeDocTypeInterface.nestedClass("Dummy")
}