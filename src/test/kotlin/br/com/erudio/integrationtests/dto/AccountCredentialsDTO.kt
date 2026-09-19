package br.com.erudio.integrationtests.dto

import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement
data class AccountCredentialsDTO(
    var username: String? = null,
    var password: String? = null,
    var fullname: String? = null
)
