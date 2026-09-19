package br.com.erudio.integrationtests.controllers.withyaml

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.controllers.withyaml.mapper.YAMLMapper
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.TokenDTO
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationTest
import io.restassured.RestAssured.given
import io.restassured.config.EncoderConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.http.ContentType
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthControllerYamlTest : AbstractIntegrationTest() {

    private lateinit var tokenDto: TokenDTO
    private lateinit var objectMapper: YAMLMapper

    @BeforeAll
    fun setUp() {
        objectMapper = YAMLMapper()

        tokenDto = TokenDTO()
    }

    @Test
    @Order(1)
    fun signin() {
        val credentials = AccountCredentialsDTO("leandro", "admin123")

        tokenDto = given()
            .config(
                RestAssuredConfig.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig()
                            .encodeContentTypeAs(MediaType.APPLICATION_YAML_VALUE, ContentType.TEXT)
                    )
            )
            .basePath("/auth/signin")
            .port(TestConfigs.SERVER_PORT)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .body(credentials, objectMapper)
            .`when`()
            .post()
            .then()
            .statusCode(200)
            .extract()
            .body()
            .`as`(TokenDTO::class.java, objectMapper)

        assertNotNull(tokenDto.accessToken)
        assertNotNull(tokenDto.refreshToken)
    }

    @Test
    @Order(2)
    fun refreshToken() {
        tokenDto = given()
            .config(
                RestAssuredConfig.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig()
                            .encodeContentTypeAs(MediaType.APPLICATION_YAML_VALUE, ContentType.TEXT)
                    )
            )
            .basePath("/auth/refresh")
            .port(TestConfigs.SERVER_PORT)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .pathParam("username", tokenDto.username)
            .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + tokenDto.refreshToken)
            .`when`()
            .put("{username}")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .`as`(TokenDTO::class.java, objectMapper)

        assertNotNull(tokenDto.accessToken)
        assertNotNull(tokenDto.refreshToken)
    }
}
