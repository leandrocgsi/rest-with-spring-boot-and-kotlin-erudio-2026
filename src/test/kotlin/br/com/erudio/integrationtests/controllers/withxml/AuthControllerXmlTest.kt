package br.com.erudio.integrationtests.controllers.withxml

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.TokenDTO
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationTest
import io.restassured.RestAssured.given
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import tools.jackson.databind.DeserializationFeature
import tools.jackson.dataformat.xml.XmlMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthControllerXmlTest : AbstractIntegrationTest() {

    private lateinit var tokenDto: TokenDTO
    private lateinit var objectMapper: XmlMapper

    @BeforeAll
    fun setUp() {
        objectMapper = XmlMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()

        tokenDto = TokenDTO()
    }

    @Test
    @Order(1)
    fun signin() {
        val credentials = AccountCredentialsDTO("leandro", "admin123")

        val content = given()
            .basePath("/auth/signin")
            .port(TestConfigs.SERVER_PORT)
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .accept(MediaType.APPLICATION_XML_VALUE)
            .body(credentials)
            .`when`()
            .post()
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()

        tokenDto = objectMapper.readValue(content, TokenDTO::class.java)

        assertNotNull(tokenDto.accessToken)
        assertNotNull(tokenDto.refreshToken)
    }

    @Test
    @Order(2)
    fun refreshToken() {
        val content = given()
            .basePath("/auth/refresh")
            .port(TestConfigs.SERVER_PORT)
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .accept(MediaType.APPLICATION_XML_VALUE)
            .pathParam("username", tokenDto.username)
            .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + tokenDto.refreshToken)
            .`when`()
            .put("{username}")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()

        tokenDto = objectMapper.readValue(content, TokenDTO::class.java)

        assertNotNull(tokenDto.accessToken)
        assertNotNull(tokenDto.refreshToken)
    }
}
