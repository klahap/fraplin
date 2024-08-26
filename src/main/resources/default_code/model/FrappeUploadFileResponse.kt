package default_code.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FrappeUploadFileResponse(
    val name: String,
    @SerialName("file_name") val fileName: String?,
    @SerialName("file_type") val fileType: String?,
    @SerialName("file_size") val fileSize: Long?,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("folder") val folder: String?,
    @SerialName("attached_to_doctype") val attachedToDoctype: String?,
    @SerialName("attached_to_name") val attachedToName: String?,
    @SerialName("attached_to_field") val attachedToField: String?,
    @SerialName("content_hash") val contentHash: String?,
) {
    val attachField get() = FrappeAttachField(fileUrl)
}
