package io.github.klahap.fraplin.models

import kotlinx.serialization.Serializable


@Serializable
data class FraplinSpec(
    val docTypes: List<DocType.Full>,
    val dummyDocTypes: List<DocType.Dummy>,
)
