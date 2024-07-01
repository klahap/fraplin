package io.github.klahap.fraplin.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path


object PathUtil {
    data class DefaultCodeFile(
        private val name: String,
        private val relativePackage: String?,
    ) {
        val relativePath = listOfNotNull(relativePackage, name).joinToString("/")

        suspend fun getContent(packageName: String) = withContext(Dispatchers.IO) {

            PathUtil::class.java.getResourceAsStream("/default_code/$relativePath")!!.readAllBytes()
        }!!.decodeToString()
            .replaceFirst("package default_code", "package $packageName")
            .replace("import default_code", "import $packageName")
    }

    val defaultCodeFiles = setOf(
        DefaultCodeFile(name = "Annotations.kt", relativePackage = null),
        DefaultCodeFile(name = "Exceptions.kt", relativePackage = null),
        DefaultCodeFile(name = "Interfaces.kt", relativePackage = null),

        DefaultCodeFile(name = "CoroutineUtil.kt", relativePackage = "util"),
        DefaultCodeFile(name = "DelegationUtil.kt", relativePackage = "util"),
        DefaultCodeFile(name = "DocTypeUtil.kt", relativePackage = "util"),
        DefaultCodeFile(name = "HttpUtil.kt", relativePackage = "util"),
        DefaultCodeFile(name = "JsonElementUtil.kt", relativePackage = "util"),
        DefaultCodeFile(name = "JsonUtil.kt", relativePackage = "util"),

        DefaultCodeFile(name = "FrappeCloudBaseService.kt", relativePackage = "service"),
        DefaultCodeFile(name = "FrappeSiteService.kt", relativePackage = "service"),

        DefaultCodeFile(name = "FrappeBase.kt", relativePackage = "model"),
        DefaultCodeFile(name = "FrappeDocTableBuilder.kt", relativePackage = "model"),
        DefaultCodeFile(name = "FrappeFieldType.kt", relativePackage = "model"),
        DefaultCodeFile(name = "FrappeFilter.kt", relativePackage = "model"),
        DefaultCodeFile(name = "FrappeFilterSet.kt", relativePackage = "model"),
        DefaultCodeFile(name = "FrappeOrderBy.kt", relativePackage = "model"),
        DefaultCodeFile(name = "FrappeRequestOptions.kt", relativePackage = "model"),

        DefaultCodeFile(name = "FilterBoolean.kt", relativePackage = "util/request"),
        DefaultCodeFile(name = "FilterDate.kt", relativePackage = "util/request"),
        DefaultCodeFile(name = "FilterDateTime.kt", relativePackage = "util/request"),
        DefaultCodeFile(name = "FilterDouble.kt", relativePackage = "util/request"),
        DefaultCodeFile(name = "FilterEnum.kt", relativePackage = "util/request"),
        DefaultCodeFile(name = "FilterInlineString.kt", relativePackage = "util/request"),
        DefaultCodeFile(name = "FilterInt.kt", relativePackage = "util/request"),
        DefaultCodeFile(name = "FilterString.kt", relativePackage = "util/request"),
    )
}
