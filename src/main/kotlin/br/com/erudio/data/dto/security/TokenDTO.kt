package br.com.erudio.data.dto.security

import java.util.*

data class TokenDTO(
    var username: String? = null,
    var authenticated: Boolean? = null,
    var created: Date? = null,
    var expiration: Date? = null,
    var accessToken: String? = null,
    var refreshToken: String? = null
)
