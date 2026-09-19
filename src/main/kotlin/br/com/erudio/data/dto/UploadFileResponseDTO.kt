package br.com.erudio.data.dto

data class UploadFileResponseDTO(
    var fileName: String? = null,
    var fileDownloadUri: String? = null,
    var fileType: String? = null,
    var size: Long = 0
)
