package br.com.erudio.integrationtests.controllers.withyaml

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.controllers.withyaml.mapper.YAMLMapper
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.BookDTO
import br.com.erudio.integrationtests.dto.TokenDTO
import br.com.erudio.integrationtests.dto.wrappers.xmlandyaml.PagedModelBook
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationTest
import io.restassured.RestAssured.given
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.EncoderConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.filter.log.LogDetail
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
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
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookControllerYamlTest : AbstractIntegrationTest() {

    private lateinit var specification: RequestSpecification
    private lateinit var objectMapper: YAMLMapper

    private lateinit var book: BookDTO
    private lateinit var tokenDto: TokenDTO

    @BeforeAll
    fun setUp() {
        objectMapper = YAMLMapper()
        book = BookDTO()
        tokenDto = TokenDTO()
    }

    private fun yamlConfig(): RestAssuredConfig {
        return RestAssuredConfig.config()
            .encoderConfig(
                EncoderConfig.encoderConfig()
                    .encodeContentTypeAs(MediaType.APPLICATION_YAML_VALUE, ContentType.TEXT)
            )
    }

    @Test
    @Order(0)
    fun signin() {
        val credentials = AccountCredentialsDTO("leandro", "admin123")

        tokenDto = given()
            .config(yamlConfig())
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

        specification = RequestSpecBuilder()
            .addHeader(TestConfigs.HEADER_PARAM_ORIGIN, TestConfigs.ORIGIN_ERUDIO)
            .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + tokenDto.refreshToken)
            .setBasePath("/api/book/v1")
            .setPort(TestConfigs.SERVER_PORT)
            .addFilter(RequestLoggingFilter(LogDetail.ALL))
            .addFilter(ResponseLoggingFilter(LogDetail.ALL))
            .build()

        assertNotNull(tokenDto.accessToken)
        assertNotNull(tokenDto.refreshToken)
    }

    @Test
    @Order(1)
    fun createTest() {
        mockBook()

        val createdBook = given().config(yamlConfig()).spec(specification)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .body(book, objectMapper)
            .`when`()
            .post()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .body()
            .`as`(BookDTO::class.java, objectMapper)

        book = createdBook

        assertNotNull(createdBook.id)
        assertNotNull(book.id)
        assertEquals("Docker Deep Dive", book.title)
        assertEquals("Nigel Poulton", book.author)
        assertEquals(55.99, book.price)
    }

    @Test
    @Order(2)
    fun updateTest() {

        book.title = "Docker Deep Dive - Updated"

        val createdBook = given().config(yamlConfig()).spec(specification)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .body(book, objectMapper)
            .`when`()
            .put()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .body()
            .`as`(BookDTO::class.java, objectMapper)

        book = createdBook

        assertNotNull(createdBook.id)
        assertNotNull(book.id)
        assertEquals("Docker Deep Dive - Updated", book.title)
        assertEquals("Nigel Poulton", book.author)
        assertEquals(55.99, book.price)
    }

    @Test
    @Order(3)
    fun findByIdTest() {

        val createdBook = given().config(yamlConfig()).spec(specification)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .pathParam("id", book.id)
            .`when`()
            .get("{id}")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .body()
            .`as`(BookDTO::class.java, objectMapper)

        book = createdBook

        assertNotNull(createdBook.id)
        assertTrue(createdBook.id!! > 0)

        assertNotNull(book.id)
        assertEquals("Docker Deep Dive - Updated", book.title)
        assertEquals("Nigel Poulton", book.author)
        assertEquals(55.99, book.price)
    }

    @Test
    @Order(4)
    fun deleteTest() {

        given(specification)
            .pathParam("id", book.id)
            .`when`()
            .delete("{id}")
            .then()
            .statusCode(204)
    }

    @Test
    @Order(5)
    fun findAllTest() {

        val response = given(specification)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .queryParams("page", 9, "size", 12, "direction", "asc")
            .`when`()
            .get()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .body()
            .`as`(PagedModelBook::class.java, objectMapper)

        val content = response.content!!

        val bookOne = content[0]

        assertNotNull(bookOne.id)
        assertNotNull(bookOne.title)
        assertNotNull(bookOne.author)
        assertNotNull(bookOne.price)
        assertTrue(bookOne.id!! > 0)
        assertEquals("The Art of Agile Development", bookOne.title)
        assertEquals("James Shore e Shane Warden", bookOne.author)
        assertEquals(97.21, bookOne.price)

        val foundBookSeven = content[7]

        assertNotNull(foundBookSeven.id)
        assertNotNull(foundBookSeven.title)
        assertNotNull(foundBookSeven.author)
        assertNotNull(foundBookSeven.price)
        assertTrue(foundBookSeven.id!! > 0)
        assertEquals("The Art of Computer Programming, Volume 1: Fundamental Algorithms", foundBookSeven.title)
        assertEquals("Donald E. Knuth", foundBookSeven.author)
        assertEquals(139.69, foundBookSeven.price)
    }

    private fun mockBook() {
        book.title = "Docker Deep Dive"
        book.author = "Nigel Poulton"
        book.price = 55.99
        book.launchDate = Date()
    }
}
