package io.github.klahap.fraplin.models

import kotlinx.serialization.Serializable


@Serializable
data class FraplinSpec(
    val docTypes: List<DocType.Base>,
    val virtualDocTypes: List<DocType.Virtual>,
    val dummyDocTypes: List<DocType.Dummy>,
    val whiteListFunctions: List<WhiteListFunction>,
)
