package br.com.erudio.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    @Value("\${cors.originPatterns:default}")
    private var corsOriginPatterns: String = ""

    override fun addCorsMappings(registry: CorsRegistry) {
        val allowedOrigins = corsOriginPatterns.split(",").toTypedArray()
        registry.addMapping("/**")
            .allowedOrigins(*allowedOrigins)
            .allowedMethods("*")
            .allowCredentials(true)
    }

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {

        configurer.favorParameter(false)
            .ignoreAcceptHeader(false)
            .useRegisteredExtensionsOnly(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML)
            .mediaType("yaml", MediaType.APPLICATION_YAML)
    }
}
