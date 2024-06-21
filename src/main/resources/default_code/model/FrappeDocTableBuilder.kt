package default_code.model

import default_code.DocType
import default_code.DocTypeBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

class FrappeDocTableBuilder<D : DocType, B : DocTypeBuilder<D, B>> {
    private val childs: MutableList<JsonElement> = mutableListOf()

    fun add(value: JsonElement) = also { childs.add(value) }
    fun build() = JsonArray(childs)
}