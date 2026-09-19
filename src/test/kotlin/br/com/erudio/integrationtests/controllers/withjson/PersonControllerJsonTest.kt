package br.com.erudio.integrationtests.controllers.withjson

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.PersonDTO
import br.com.erudio.integrationtests.dto.TokenDTO
import br.com.erudio.integrationtests.dto.wrappers.json.WrapperPersonDTO
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationTest
import io.restassured.RestAssured.given
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.LogDetail
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.response.Response
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
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonControllerJsonTest : AbstractIntegrationTest() {

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

        specification = RequestSpecBuilder()
            .addHeader(TestConfigs.HEADER_PARAM_ORIGIN, TestConfigs.ORIGIN_ERUDIO)
            .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + tokenDto.accessToken)
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

        val content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(person)
            .`when`()
            .post()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .extract()
            .body()
            .asString()

        val createdPerson = objectMapper.readValue(content, PersonDTO::class.java)
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

        val content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(person)
            .`when`()
            .put()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .extract()
            .body()
            .asString()

        val createdPerson = objectMapper.readValue(content, PersonDTO::class.java)
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

        val content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .pathParam("id", person.id)
            .`when`()
            .get("{id}")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .extract()
            .body()
            .asString()

        val createdPerson = objectMapper.readValue(content, PersonDTO::class.java)
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

        val content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .pathParam("id", person.id)
            .`when`()
            .patch("{id}")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .extract()
            .body()
            .asString()

        val createdPerson = objectMapper.readValue(content, PersonDTO::class.java)
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

        val content = given(specification)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .queryParams("page", 3, "size", 12, "direction", "asc")
            .`when`()
            .get()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .extract()
            .body()
            .asString()

        val wrapper = objectMapper.readValue(content, WrapperPersonDTO::class.java)
        val people = wrapper.embedded!!.people!!

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

        val content = given(specification)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .pathParam("firstName", "and")
            .queryParams("page", 0, "size", 12, "direction", "asc")
            .`when`()
            .get("findPeopleByName/{firstName}")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .extract()
            .body()
            .asString()

        val wrapper = objectMapper.readValue(content, WrapperPersonDTO::class.java)
        val people = wrapper.embedded!!.people!!

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
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .queryParams("page", 3, "size", 12, "direction", "asc")
            .`when`()
            .get()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .extract()
            .body() as Response

        val people: List<Map<String, Any>> = response.jsonPath().getList("_embedded.people")

        for (person in people) {
            val links = person["_links"] as Map<String, Any>

            assertThat("HATEOAS/HAL link 'self' is missing", links, hasKey("self"))
            assertThat("HATEOAS/HAL link 'findAll' is missing", links, hasKey("findAll"))
            assertThat("HATEOAS/HAL link 'findByName' is missing", links, hasKey("findByName"))
            assertThat("HATEOAS/HAL link 'create' is missing", links, hasKey("create"))
            assertThat("HATEOAS/HAL link 'update' is missing", links, hasKey("update"))
            assertThat("HATEOAS/HAL link 'delete' is missing", links, hasKey("delete"))
            assertThat("HATEOAS/HAL link 'disable' is missing", links, hasKey("disable"))
            assertThat("HATEOAS/HAL link 'massCreation' is missing", links, hasKey("massCreation"))
            assertThat("HATEOAS/HAL link 'exportPage' is missing", links, hasKey("exportPage"))

            links.forEach { (key, value) ->
                val href = (value as Map<String, String>)["href"]
                assertThat("HATEOAS/HAL link $key has an invalid URL", href, matchesPattern("https?://.+/api/person/v1.*"))
                assertThat("HATEOAS/HAL link $key has an invalid HTTP method", value["type"], notNullValue())
            }

            val pageLinks: Map<String, Any> = response.jsonPath().getMap("_links")
            assertThat("Page link 'self' is missing", pageLinks, hasKey("self"))
            assertThat("Page link 'first' is missing", pageLinks, hasKey("first"))
            assertThat("Page link 'prev' is missing", pageLinks, hasKey("prev"))
            assertThat("Page link 'next' is missing", pageLinks, hasKey("next"))
            assertThat("Page link 'last' is missing", pageLinks, hasKey("last"))

            val pageAttributes: Map<String, Any> = response.jsonPath().getMap("page")
            assertThat(pageAttributes["size"], `is`(12))
            assertThat(pageAttributes["number"], `is`(3))

            assertTrue((pageAttributes["totalElements"] as Int) > 0, "totalElements should be greater than 0")
            assertTrue((pageAttributes["totalPages"] as Int) > 0, "totalPages should be greater than 0")
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
