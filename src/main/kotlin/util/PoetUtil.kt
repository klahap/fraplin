package io.github.klahap.fraplin.util

import com.squareup.kotlinpoet.*
import kotlin.reflect.KClass

fun buildFile(packageName: String, name: String, block: FileSpec.Builder.() -> Unit) =
    FileSpec.builder(packageName = packageName, fileName = name).apply(block).build()

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

fun interfaceBuilder(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = TypeSpec.interfaceBuilder(name).apply(block).build()

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

fun TypeSpec.Builder.addInterface(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = addType(interfaceBuilder(name, block))

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

fun FileSpec.Builder.addObject(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = addType(TypeSpec.objectBuilder(name).apply(block).build())

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
