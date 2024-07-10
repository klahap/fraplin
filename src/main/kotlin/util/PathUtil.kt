package io.github.klahap.fraplin.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object PathUtil {
    data class DefaultCodeFile(
        private val fileName: String,
        private val packageName: String?,
    ) {
        val relativePath = listOfNotNull(packageName, fileName).joinToString("/")

        suspend fun getContent(packageName: String) = withContext(Dispatchers.IO) {

            PathUtil::class.java.getResourceAsStream("/default_code/$relativePath")!!.readAllBytes()
        }!!.decodeToString()
            .replaceFirst("package default_code", "package $packageName")
            .replace("import default_code", "import $packageName")
    }

    val defaultCodeFiles = buildSet {
        addFile("Annotations.kt")
        addFile("Exceptions.kt")
        addFile("Interfaces.kt")

        addPackage("util") {
            addFile("CoroutineUtil.kt")
            addFile("DelegationUtil.kt")
            addFile("DocTypeUtil.kt")
            addFile("HttpUtil.kt")
            addFile("JsonElementUtil.kt")
            addFile("JsonUtil.kt")
        }
        addPackage("service") {
            addFile("FrappeCloudBaseService.kt")
            addFile("FrappeSiteService.kt")
        }
        addPackage("model") {
            addFile("FrappeBase.kt")
            addFile("FrappeDocStatus.kt")
            addFile("FrappeDocTableBuilder.kt")
            addFile("FrappeFieldType.kt")
            addFile("FrappeFilter.kt")
            addFile("FrappeFilterSet.kt")
            addFile("FrappeOrderBy.kt")
            addFile("FrappeRequestOptions.kt")
        }
        addPackage("util/request") {
            addFile("FilterBoolean.kt")
            addFile("FilterDate.kt")
            addFile("FilterDateTime.kt")
            addFile("FilterDouble.kt")
            addFile("FilterEnum.kt")
            addFile("FilterInlineString.kt")
            addFile("FilterInt.kt")
            addFile("FilterString.kt")
        }
    }


    private class PackageBuilder(val packageName: String) {
        val result = mutableSetOf<DefaultCodeFile>()
        fun build() = result.toSet()
    }

    private fun PackageBuilder.addFile(fileName: String) =
        result.addFile(fileName = fileName, packageName = packageName)

    private fun MutableSet<DefaultCodeFile>.addFile(fileName: String, packageName: String? = null) =
        add(DefaultCodeFile(fileName = fileName, packageName = packageName))

    private fun MutableSet<DefaultCodeFile>.addPackage(packageName: String, block: PackageBuilder.() -> Unit) =
        addAll(PackageBuilder(packageName).apply(block).build())
}
