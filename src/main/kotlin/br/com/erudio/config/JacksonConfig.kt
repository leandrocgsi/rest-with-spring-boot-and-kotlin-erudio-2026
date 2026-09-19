package br.com.erudio.config

import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter
import tools.jackson.databind.BeanDescription
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.SerializationConfig
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.BeanPropertyWriter
import tools.jackson.databind.ser.ValueSerializerModifier
import tools.jackson.dataformat.yaml.YAMLMapper

@Configuration
class JacksonConfig {

    @Bean
    fun hateoasLinksLastModule(): JacksonModule {
        return SimpleModule("hateoasLinksLast").setSerializerModifier(object : ValueSerializerModifier() {
            override fun orderProperties(
                config: SerializationConfig,
                beanDesc: BeanDescription.Supplier,
                beanProperties: MutableList<BeanPropertyWriter>
            ): MutableList<BeanPropertyWriter> {

                val beanClass = beanDesc.beanClass
                if (!RepresentationModel::class.java.isAssignableFrom(beanClass)
                    || CollectionModel::class.java.isAssignableFrom(beanClass)
                ) return beanProperties

                val (links, others) = beanProperties.partition { it.name == "links" }
                return (others + links).toMutableList()
            }
        })
    }

    @Bean
    fun yamlMessageConverterCustomizer(hateoasLinksLastModule: JacksonModule): ServerHttpMessageConvertersCustomizer {
        return ServerHttpMessageConvertersCustomizer { builder ->
            builder.withYamlConverter(
                JacksonYamlHttpMessageConverter(
                    YAMLMapper.builder()
                        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                        .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .addModule(hateoasLinksLastModule)
                )
            )
        }
    }
}
