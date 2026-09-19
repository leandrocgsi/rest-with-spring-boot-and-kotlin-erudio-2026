package br.com.erudio.integrationtests.controllers.cors.withjson

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.PersonDTO
import br.com.erudio.integrationtests.dto.TokenDTO
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationTest
import io.restassured.RestAssured.given
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.LogDetail
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonControllerCorsTest : AbstractIntegrationTest() {

    private lateinit var specification: RequestSpecification
    private lateinit var objectMapper: ObjectMapper

    private lateinit var person: PersonDTO
    private lateinit var tokenDto: TokenDTO

    @BeforeAll
    fun setUp() {
        objectMapper = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()

        person = PersonDTO()
        tokenDto = TokenDTO()
    }

    @Test
    @Order(0)
    fun signin() {
        val credentials = AccountCredentialsDTO("leandro", "admin123")

        tokenDto = given()
            .basePath("/auth/signin")
            .port(TestConfigs.SERVER_PORT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(credentials)
            .`when`()
            .post()
            .then()
            .statusCode(200)
            .extract()
            .body()
            .`as`(TokenDTO::class.java)

        assertNotNull(tokenDto.accessToken)
        assertNotNull(tokenDto.refreshToken)
    }

    @Test
    @Order(1)
    fun create() {
        mockPerson()

        specification = RequestSpecBuilder()
            .addHeader(TestConfigs.HEADER_PARAM_ORIGIN, TestConfigs.ORIGIN_ERUDIO)
            .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + tokenDto.accessToken)
            .setBasePath("/api/person/v1")
            .setPort(TestConfigs.SERVER_PORT)
            .addFilter(RequestLoggingFilter(LogDetail.ALL))
            .addFilter(ResponseLoggingFilter(LogDetail.ALL))
            .build()

        val content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(person)
            .`when`()
            .post()
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()

        val createdPerson = objectMapper.readValue(content, PersonDTO::class.java)
        person = createdPerson

        assertNotNull(createdPerson.id)
        assertNotNull(createdPerson.firstName)
        assertNotNull(createdPerson.lastName)
        assertNotNull(createdPerson.address)
        assertNotNull(createdPerson.gender)

        assertTrue(createdPerson.id!! > 0)

        assertEquals("Richard", createdPerson.firstName)
        assertEquals("Stallman", createdPerson.lastName)
        assertEquals("New York City - New York - USA", createdPerson.address)
        assertEquals("Male", createdPerson.gender)
        assertTrue(createdPerson.enabled!!)
    }

    @Test
    @Order(2)
    fun createWithWrongOrigin() {

        specification = RequestSpecBuilder()
            .addHeader(TestConfigs.HEADER_PARAM_ORIGIN, TestConfigs.ORIGIN_SEMERU)
            .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + tokenDto.accessToken)
            .setBasePath("/api/person/v1")
            .setPort(TestConfigs.SERVER_PORT)
            .addFilter(RequestLoggingFilter(LogDetail.ALL))
            .addFilter(ResponseLoggingFilter(LogDetail.ALL))
            .build()

        val content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(person)
            .`when`()
            .post()
            .then()
            .statusCode(403)
            .extract()
            .body()
            .asString()

        assertEquals("Invalid CORS request", content)
    }

    @Test
    @Order(3)
    fun findById() {
        specification = RequestSpecBuilder()
            .addHeader(TestConfigs.HEADER_PARAM_ORIGIN, TestConfigs.ORIGIN_LOCAL)
            .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + tokenDto.accessToken)
            .setBasePath("/api/person/v1")
            .setPort(TestConfigs.SERVER_PORT)
            .addFilter(RequestLoggingFilter(LogDetail.ALL))
            .addFilter(ResponseLoggingFilter(LogDetail.ALL))
            .build()

        val content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .pathParam("id", person.id)
            .`when`()
            .get("{id}")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()

        val createdPerson = objectMapper.readValue(content, PersonDTO::class.java)
        person = createdPerson

        assertNotNull(createdPerson.id)
        assertNotNull(createdPerson.firstName)
        assertNotNull(createdPerson.lastName)
        assertNotNull(createdPerson.address)
        assertNotNull(createdPerson.gender)

        assertTrue(createdPerson.id!! > 0)

        assertEquals("Richard", createdPerson.firstName)
        assertEquals("Stallman", createdPerson.lastName)
        assertEquals("New York City - New York - USA", createdPerson.address)
        assertEquals("Male", createdPerson.gender)
        assertTrue(createdPerson.enabled!!)
    }

    @Test
    @Order(4)
    fun findByIdWithWrongOrigin() {
        specification = RequestSpecBuilder()
            .addHeader(TestConfigs.HEADER_PARAM_ORIGIN, TestConfigs.ORIGIN_SEMERU)
            .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + tokenDto.accessToken)
            .setBasePath("/api/person/v1")
            .setPort(TestConfigs.SERVER_PORT)
            .addFilter(RequestLoggingFilter(LogDetail.ALL))
            .addFilter(ResponseLoggingFilter(LogDetail.ALL))
            .build()

        val content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .pathParam("id", person.id)
            .`when`()
            .get("{id}")
            .then()
            .statusCode(403)
            .extract()
            .body()
            .asString()

        assertEquals("Invalid CORS request", content)
    }

    private fun mockPerson() {
        person.firstName = "Richard"
        person.lastName = "Stallman"
        person.address = "New York City - New York - USA"
        person.gender = "Male"
        person.enabled = true
    }
}
