package br.com.erudio.data.dto.request

data class EmailRequestDTO(
    var to: String? = null,
    var subject: String? = null,
    var body: String? = null
)
