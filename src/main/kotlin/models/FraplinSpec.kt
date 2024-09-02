package io.github.klahap.fraplin.models

import kotlinx.serialization.Serializable


@Serializable
data class FraplinSpec(
    val docTypes: List<DocType.Base> = emptyList(),
    val openApi: List<FraplinOpenApiSpec> = emptyList(),
    val dummyDocTypes: List<DocType.Dummy> = emptyList(),
    val whiteListFunctions: List<WhiteListFunction> = emptyList(),
)
