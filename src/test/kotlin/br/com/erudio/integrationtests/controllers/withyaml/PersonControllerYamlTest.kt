package br.com.erudio.integrationtests.controllers.withyaml

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.controllers.withyaml.mapper.YAMLMapper
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.PersonDTO
import br.com.erudio.integrationtests.dto.TokenDTO
import br.com.erudio.integrationtests.dto.wrappers.xmlandyaml.PagedModelPerson
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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.yaml.snakeyaml.Yaml

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonControllerYamlTest : AbstractIntegrationTest() {

    private lateinit var specification: RequestSpecification
    private lateinit var objectMapper: YAMLMapper

    private lateinit var person: PersonDTO
    private lateinit var tokenDto: TokenDTO

    @BeforeAll
    fun setUp() {
        objectMapper = YAMLMapper()

        person = PersonDTO()
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
            .setBasePath("/api/person/v1")
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
        mockPerson()

        val createdPerson = given().config(yamlConfig()).spec(specification)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .body(person, objectMapper)
            .`when`()
            .post()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .body()
            .`as`(PersonDTO::class.java, objectMapper)

        person = createdPerson

        assertNotNull(createdPerson.id)
        assertTrue(createdPerson.id!! > 0)

        assertEquals("Linus", createdPerson.firstName)
        assertEquals("Torvalds", createdPerson.lastName)
        assertEquals("Helsinki - Finland", createdPerson.address)
        assertEquals("Male", createdPerson.gender)
        assertTrue(createdPerson.enabled!!)
    }

    @Test
    @Order(2)
    fun updateTest() {
        person.lastName = "Benedict Torvalds"

        val createdPerson = given().config(yamlConfig()).spec(specification)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .body(person, objectMapper)
            .`when`()
            .put()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .body()
            .`as`(PersonDTO::class.java, objectMapper)

        person = createdPerson

        assertNotNull(createdPerson.id)
        assertTrue(createdPerson.id!! > 0)

        assertEquals("Linus", createdPerson.firstName)
        assertEquals("Benedict Torvalds", createdPerson.lastName)
        assertEquals("Helsinki - Finland", createdPerson.address)
        assertEquals("Male", createdPerson.gender)
        assertTrue(createdPerson.enabled!!)
    }

    @Test
    @Order(3)
    fun findByIdTest() {

        val createdPerson = given().config(yamlConfig()).spec(specification)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .pathParam("id", person.id)
            .`when`()
            .get("{id}")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .body()
            .`as`(PersonDTO::class.java, objectMapper)

        person = createdPerson

        assertNotNull(createdPerson.id)
        assertTrue(createdPerson.id!! > 0)

        assertEquals("Linus", createdPerson.firstName)
        assertEquals("Benedict Torvalds", createdPerson.lastName)
        assertEquals("Helsinki - Finland", createdPerson.address)
        assertEquals("Male", createdPerson.gender)
        assertTrue(createdPerson.enabled!!)
    }

    @Test
    @Order(4)
    fun disableTest() {

        val createdPerson = given().config(yamlConfig()).spec(specification)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .pathParam("id", person.id)
            .`when`()
            .patch("{id}")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .body()
            .`as`(PersonDTO::class.java, objectMapper)

        person = createdPerson

        assertNotNull(createdPerson.id)
        assertTrue(createdPerson.id!! > 0)

        assertEquals("Linus", createdPerson.firstName)
        assertEquals("Benedict Torvalds", createdPerson.lastName)
        assertEquals("Helsinki - Finland", createdPerson.address)
        assertEquals("Male", createdPerson.gender)
        assertFalse(createdPerson.enabled!!)
    }

    @Test
    @Order(5)
    fun deleteTest() {

        given(specification)
            .pathParam("id", person.id)
            .`when`()
            .delete("{id}")
            .then()
            .statusCode(204)
    }

    @Test
    @Order(6)
    fun findAllTest() {

        val response = given(specification)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .queryParams("page", 3, "size", 12, "direction", "asc")
            .`when`()
            .get()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .body()
            .`as`(PagedModelPerson::class.java, objectMapper)

        val people = response.content!!

        val personOne = people[0]

        assertNotNull(personOne.id)
        assertTrue(personOne.id!! > 0)

        assertEquals("Allin", personOne.firstName)
        assertEquals("Emmot", personOne.lastName)
        assertEquals("7913 Lindbergh Way", personOne.address)
        assertEquals("Male", personOne.gender)
        assertFalse(personOne.enabled!!)

        val personFour = people[4]

        assertNotNull(personFour.id)
        assertTrue(personFour.id!! > 0)

        assertEquals("Alonso", personFour.firstName)
        assertEquals("Luchelli", personFour.lastName)
        assertEquals("9 Doe Crossing Avenue", personFour.address)
        assertEquals("Male", personFour.gender)
        assertFalse(personFour.enabled!!)
    }

    @Test
    @Order(7)
    fun findByNameTest() {

        val response = given(specification)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .pathParam("firstName", "and")
            .queryParams("page", 0, "size", 12, "direction", "asc")
            .`when`()
            .get("findPeopleByName/{firstName}")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .body()
            .`as`(PagedModelPerson::class.java, objectMapper)

        val people = response.content!!

        val personOne = people[0]

        assertNotNull(personOne.id)
        assertTrue(personOne.id!! > 0)

        assertEquals("Alessandro", personOne.firstName)
        assertEquals("McFaul", personOne.lastName)
        assertEquals("5 Lukken Plaza", personOne.address)
        assertEquals("Male", personOne.gender)
        assertTrue(personOne.enabled!!)

        val personFour = people[4]

        assertNotNull(personFour.id)
        assertTrue(personFour.id!! > 0)

        assertEquals("Brandyn", personFour.firstName)
        assertEquals("Grasha", personFour.lastName)
        assertEquals("96 Mosinee Parkway", personFour.address)
        assertEquals("Male", personFour.gender)
        assertTrue(personFour.enabled!!)
    }

    @Test
    @Order(8)
    @Suppress("UNCHECKED_CAST")
    fun hateoasAndHalTest() {

        val response = given(specification)
            .accept(MediaType.APPLICATION_YAML_VALUE)
            .queryParams("page", 3, "size", 12, "direction", "asc")
            .`when`()
            .get()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_YAML_VALUE)
            .extract()
            .response()

        val yaml = response.body.asString()

        val yamlParser = Yaml()
        val parsedYaml: Map<String, Any> = yamlParser.load(yaml)

        val content = parsedYaml["content"] as List<Map<String, Any>>

        for (person in content) {

            val links = person["links"] as List<Map<String, String>>
            for (link in links) {
                assertThat("HATEOAS/HAL link rel is missing", link, hasKey("rel"))
                assertThat("HATEOAS/HAL link href is missing", link, hasKey("href"))
                assertThat("HATEOAS/HAL link type is missing", link, hasKey("type"))

                assertThat("HATEOAS/HAL link $link has an invalid URL", link["href"], matchesPattern("https?://.+/api/person/v1.*"))
            }
        }

        val page = parsedYaml["page"] as Map<String, Any>
        assertThat("Page number is incorrect", page["number"], `is`(3))
        assertThat("Page size is incorrect", page["size"], `is`(12))

        val totalElements = page["totalElements"].toString().toInt()
        val totalPages = page["totalPages"].toString().toInt()

        assertTrue(totalElements > 0, "totalElements should be greater than 0")
        assertTrue(totalPages > 0, "totalPages should be greater than 0")

        val pageLinks = parsedYaml["links"] as List<Map<String, String>>
        for (pageLink in pageLinks) {

            assertThat("HATEOAS/HAL page link href is missing", pageLink, hasKey("href"))

            assertThat("HATEOAS/HAL page link $pageLink has an invalid URL", pageLink["href"], matchesPattern("https?://.+/api/person/v1.*"))
        }
    }

    private fun mockPerson() {
        person.firstName = "Linus"
        person.lastName = "Torvalds"
        person.address = "Helsinki - Finland"
        person.gender = "Male"
        person.enabled = true
        person.profileUrl = "https://pub.erudio.com.br/meus-cursos"
        person.photoUrl = "https://pub.erudio.com.br/meus-cursos"
    }
}
