package br.com.erudio.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "email")
class EmailDefaultsConfig(
    var subject: String? = null,
    var message: String? = null
)
