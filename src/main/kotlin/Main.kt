package io.github.klahap.fraplin

import io.github.klahap.fraplin.models.DocTypeInfo
import io.github.klahap.fraplin.services.FrappeCodeGenService
import io.github.klahap.fraplin.services.FrappeCloudBaseService
import io.github.klahap.fraplin.services.FrappeSiteService
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.system.exitProcess


fun printHelper(exitCode: Int): Nothing {
    println("invalid args")
    exitProcess(exitCode)
}


suspend fun main(args: Array<String>) {
    val siteUrl: HttpUrl
    val cloudToken: String?
    val userToken: String?
    val output: Path
    val packageName: String
    val docTypes: Set<DocTypeInfo>
    args.also { if (it.size % 2 != 0) printHelper(1) }
        .toList().chunked(2).associate { it[0] to it[1] }
        .let { argDict ->
            siteUrl = argDict["--siteUrl"]?.toHttpUrl() ?: printHelper(1)
            cloudToken = argDict["--cloudToken"]
            userToken = argDict["--userToken"]
            output = argDict["--output"]?.let { Path(it) } ?: printHelper(1)
            packageName = argDict["--packageName"] ?: printHelper(1)
            docTypes = argDict["--docTypes"]?.split(',')?.map { DocTypeInfo(it) }?.toSet() ?: printHelper(1)
            if (listOfNotNull(userToken, cloudToken).size != 1)
                printHelper(1)
        }

    val siteService = if (userToken != null)
        FrappeSiteService(siteUrl = siteUrl, userApiToken = userToken)
    else
        FrappeCloudBaseService(token = cloudToken!!).getSiteClient(siteUrl = siteUrl)

    val frappeCodeGenService = FrappeCodeGenService(siteService)
    frappeCodeGenService.generate(
        packageName = packageName,
        output = output,
        docTypeInfos = docTypes,
    )
    exitProcess(0)
}
