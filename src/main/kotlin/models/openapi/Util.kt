package io.github.klahap.fraplin.models.openapi

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun jsonOf(element: String) = JsonPrimitive(element)
fun jsonOf(element: Boolean) = JsonPrimitive(element)
fun jsonOf(element: Int) = JsonPrimitive(element)
fun jsonOf(vararg element: Pair<String, JsonElement>?) = JsonObject(element.filterNotNull().toMap())
fun jsonOf(elements: Map<String, JsonElement>) = JsonObject(elements)
fun jsonOf(vararg element: JsonElement) = JsonArray(element.toList())
fun jsonOf(elements: Iterable<JsonElement>) = JsonArray(elements.toList())
