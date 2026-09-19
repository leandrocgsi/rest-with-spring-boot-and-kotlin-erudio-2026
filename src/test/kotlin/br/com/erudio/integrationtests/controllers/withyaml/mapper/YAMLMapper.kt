package br.com.erudio.integrationtests.controllers.withyaml.mapper

import io.restassured.mapper.ObjectMapper
import io.restassured.mapper.ObjectMapperDeserializationContext
import io.restassured.mapper.ObjectMapperSerializationContext
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature

class YAMLMapper : ObjectMapper {

    private val mapper: tools.jackson.dataformat.yaml.YAMLMapper = tools.jackson.dataformat.yaml.YAMLMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    override fun deserialize(context: ObjectMapperDeserializationContext): Any {
        val content = context.dataToDeserialize.asString()
        val type = context.type as Class<*>
        try {
            return mapper.readValue(content, type)
        } catch (e: JacksonException) {
            throw IllegalArgumentException("Error deserializing YAML content", e)
        }
    }

    override fun serialize(context: ObjectMapperSerializationContext): Any {
        try {
            return mapper.writeValueAsString(context.objectToSerialize)
        } catch (e: JacksonException) {
            throw IllegalArgumentException("Error serializing YAML content", e)
        }
    }
}
