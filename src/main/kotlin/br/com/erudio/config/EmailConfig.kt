package br.com.erudio.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
@ConfigurationProperties(prefix = "spring.mail")
class EmailConfig(
    var host: String? = null,
    var port: Int = 0,
    var username: String? = null,
    var password: String? = null,
    var from: String? = null,
    var ssl: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        other as EmailConfig
        return port == other.port && ssl == other.ssl && host == other.host &&
                username == other.username && password == other.password && from == other.from
    }

    override fun hashCode(): Int = Objects.hash(host, port, username, password, from, ssl)
}
