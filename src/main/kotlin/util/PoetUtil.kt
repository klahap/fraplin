package io.github.klahap.fraplin.util

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.klahap.fraplin.models.DocType
import kotlin.reflect.KClass

data class CodeGenContext(
    val packageName: String,
    val docTypes: Map<String, DocType>
) {
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
}

fun fileBuilder(
    packageName: String,
    fileName: String,
    block: FileSpec.Builder.() -> Unit,
) = FileSpec.builder(packageName, fileName).apply(block).build()

fun FileSpec.Builder.dataClass(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) {
    val typeSpec = TypeSpec.classBuilder(name)
        .addModifiers(KModifier.DATA)
        .apply(block)
        .build()
    addType(typeSpec)
}

fun TypeSpec.Builder.valueClass(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = clazz(name) {
    addModifiers(KModifier.VALUE)
    addAnnotation(JvmInline::class)
    block()
}

fun FileSpec.Builder.clazz(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
): FileSpec.Builder = addType(TypeSpec.classBuilder(name).apply(block).build())


fun TypeSpec.Builder.clazz(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
): TypeSpec.Builder = addType(TypeSpec.classBuilder(name).apply(block).build())

fun TypeSpec.Builder.addProperty(
    name: String,
    type: TypeName,
    block: PropertySpec.Builder.() -> Unit,
) = addProperty(PropertySpec.builder(name, type).apply(block).build())

fun PropertySpec.Builder.getter(block: FunSpec.Builder.() -> Unit) =
    getter(FunSpec.getterBuilder().apply(block).build())

fun TypeSpec.Builder.addInitializerBlock(
    block: CodeBlock.Builder.() -> Unit
) = addInitializerBlock(CodeBlock.builder().apply(block).build())


fun TypeSpec.Builder.addCompanion(
    block: TypeSpec.Builder.() -> Unit,
) = addType(TypeSpec.companionObjectBuilder().apply(block).build())

fun TypeSpec.Builder.primaryConstructor(
    block: FunSpec.Builder.() -> Unit,
) = primaryConstructor(FunSpec.constructorBuilder().apply(block).build())

fun TypeSpec.Builder.addFunction(
    name: String,
    block: FunSpec.Builder.() -> Unit,
) = addFunction(FunSpec.builder(name).apply(block).build())

fun FileSpec.Builder.addFunction(
    name: String,
    block: FunSpec.Builder.() -> Unit,
) = addFunction(FunSpec.builder(name).apply(block).build())

context(TypeSpec.Builder)
fun FunSpec.Builder.addPrimaryConstructorProperty(
    name: String,
    type: KClass<*>,
    block: PropertySpec.Builder.() -> Unit
) {
    addParameter(name, type)
    addProperty(name, type) {
        initializer(name)
        block()
    }
}

context(TypeSpec.Builder)
fun FunSpec.Builder.addPrimaryConstructorProperty(
    name: String,
    type: TypeName,
    block: PropertySpec.Builder.() -> Unit
) {
    addParameter(name, type)
    addProperty(name, type) {
        initializer(name)
        block()
    }
}

fun FunSpec.Builder.addParameter(
    name: String,
    type: TypeName,
    block: ParameterSpec.Builder.() -> Unit,
) = addParameter(ParameterSpec.builder(name, type).apply(block).build())

fun TypeSpec.Builder.addProperty(
    name: String,
    type: KClass<*>,
    block: PropertySpec.Builder.() -> Unit,
) = addProperty(buildProperty(name, type, block))

fun objectBuilder(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = TypeSpec.objectBuilder(name).apply(block).build()

fun TypeSpec.Builder.dataObject(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
): TypeSpec.Builder = addType(
    TypeSpec.objectBuilder(name).apply {
        addModifiers(KModifier.DATA)
        block()
    }.build()
)

fun TypeSpec.Builder.addSuperinterface(type: TypeName, block: CodeBlock.Builder.() -> Unit) =
    addSuperinterface(type, CodeBlock.builder().apply(block).build())

fun TypeSpec.Builder.addObject(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = addType(objectBuilder(name, block))

fun TypeSpec.Builder.addEnumConstant(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = addEnumConstant(name, anonymousClassBuilder(block))

fun buildParameter(
    name: String,
    type: TypeName,
    block: ParameterSpec.Builder.() -> Unit,
) = ParameterSpec.builder(name = name, type = type).apply(block).build()

fun buildProperty(
    name: String,
    type: TypeName,
    block: PropertySpec.Builder.() -> Unit,
) = PropertySpec.builder(name = name, type = type).apply(block).build()

fun buildProperty(
    name: String,
    type: KClass<*>,
    block: PropertySpec.Builder.() -> Unit,
) = PropertySpec.builder(name = name, type = type).apply(block).build()

fun anonymousClassBuilder(
    block: TypeSpec.Builder.() -> Unit,
) = TypeSpec.anonymousClassBuilder().apply(block).build()


fun TypeSpec.Builder.addAnnotation(
    type: ClassName,
    block: AnnotationSpec.Builder.() -> Unit,
) = addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun FunSpec.Builder.addAnnotation(
    type: ClassName,
    block: AnnotationSpec.Builder.() -> Unit,
) = addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun ParameterSpec.Builder.addAnnotation(
    type: ClassName,
    block: AnnotationSpec.Builder.() -> Unit,
) = addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun PropertySpec.Builder.addAnnotation(
    type: ClassName,
    block: AnnotationSpec.Builder.() -> Unit,
) = addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun enumBuilder(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = TypeSpec.enumBuilder(name).apply(block).build()

fun FileSpec.Builder.addInterface(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = addType(TypeSpec.interfaceBuilder(name).apply(block).build())

fun FileSpec.Builder.addEnum(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = addType(TypeSpec.enumBuilder(name).apply(block).build())

fun TypeSpec.Builder.addEnum(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = addType(TypeSpec.enumBuilder(name).apply(block).build())

fun codeBlockBuilder(
    block: CodeBlock.Builder.() -> Unit,
) = CodeBlock.Builder().apply(block).build()

fun FunSpec.Builder.addCode(block: CodeBlock.Builder.() -> Unit) =
    addCode(CodeBlock.builder().apply(block).build())
