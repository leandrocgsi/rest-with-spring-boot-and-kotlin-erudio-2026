package br.com.erudio.integrationtests.dto

import jakarta.xml.bind.annotation.XmlRootElement
import java.util.*

@XmlRootElement
data class TokenDTO(
    var username: String? = null,
    var authenticated: Boolean? = null,
    var created: Date? = null,
    var expiration: Date? = null,
    var accessToken: String? = null,
    var refreshToken: String? = null
)
