package br.com.erudio.data.dto.security

data class AccountCredentialsDTO(
    var username: String? = null,
    var password: String? = null,
    var fullname: String? = null
)
