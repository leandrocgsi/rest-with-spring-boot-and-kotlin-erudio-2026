package br.com.erudio.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Links
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter
import tools.jackson.dataformat.yaml.YAMLMapper

@Configuration
class JacksonConfig {

    @Bean
    fun yamlMessageConverterCustomizer(): ServerHttpMessageConvertersCustomizer {
        return ServerHttpMessageConvertersCustomizer { builder ->
            builder.withYamlConverter(
                JacksonYamlHttpMessageConverter(
                    YAMLMapper.builder()
                        .addMixIn(EntityModel::class.java, EntityModelYamlMixin::class.java)
                )
            )
        }
    }

    private abstract class EntityModelYamlMixin {

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        abstract fun getLinks(): Links
    }
}
