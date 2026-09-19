package br.com.erudio.integrationtests.controllers.withxml

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.PersonDTO
import br.com.erudio.integrationtests.dto.TokenDTO
import br.com.erudio.integrationtests.dto.wrappers.xmlandyaml.PagedModelPerson
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationTest
import io.restassured.RestAssured.given
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.LogDetail
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.path.xml.XmlPath
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
import tools.jackson.dataformat.xml.XmlMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonControllerXmlTest : AbstractIntegrationTest() {

    private lateinit var specification: RequestSpecification
    private lateinit var objectMapper: XmlMapper

    private lateinit var person: PersonDTO
    private lateinit var tokenDto: TokenDTO

    @BeforeAll
    fun setUp() {
        objectMapper = XmlMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()

        person = PersonDTO()
        tokenDto = TokenDTO()
    }

    @Test
    @Order(0)
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
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .accept(MediaType.APPLICATION_XML_VALUE)
            .body(person)
            .`when`()
            .post()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_XML_VALUE)
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
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .accept(MediaType.APPLICATION_XML_VALUE)
            .body(person)
            .`when`()
            .put()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_XML_VALUE)
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
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .accept(MediaType.APPLICATION_XML_VALUE)
            .pathParam("id", person.id)
            .`when`()
            .get("{id}")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_XML_VALUE)
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
            .accept(MediaType.APPLICATION_XML_VALUE)
            .pathParam("id", person.id)
            .`when`()
            .patch("{id}")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_XML_VALUE)
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
            .accept(MediaType.APPLICATION_XML_VALUE)
            .queryParams("page", 3, "size", 12, "direction", "asc")
            .`when`()
            .get()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .extract()
            .body()
            .asString()

        val wrapper = objectMapper.readValue(content, PagedModelPerson::class.java)
        val people = wrapper.content!!

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
            .accept(MediaType.APPLICATION_XML_VALUE)
            .pathParam("firstName", "and")
            .queryParams("page", 0, "size", 12, "direction", "asc")
            .`when`()
            .get("findPeopleByName/{firstName}")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .extract()
            .body()
            .asString()

        val wrapper = objectMapper.readValue(content, PagedModelPerson::class.java)
        val people = wrapper.content!!

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
    fun hateoasAndHalTest() {

        val response = given(specification)
            .accept(MediaType.APPLICATION_XML_VALUE)
            .queryParams("page", 3, "size", 12, "direction", "asc")
            .`when`()
            .get()
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .extract()
            .body() as Response

        val xml = response.body.asString()

        val xmlPath = XmlPath(xml)

        val peopleLinks: List<String> = xmlPath.getList("PagedModel.content.content.links.href")

        for (link in peopleLinks) {
            assertThat("HATEOAS/HAL link $link has an invalid URL", link, matchesPattern("https?://.+/api/person/v1.*"))

            assertThat("HATEOAS/HAL link $link has a null URL", notNullValue())
        }

        val pageLinks: List<String> = xmlPath.getList("PagedModel.links.href")
        for (pageLink in pageLinks) {
            assertThat("HATEOAS/HAL pageLink $pageLink has an invalid URL", pageLink, matchesPattern("https?://.+/api/person/v1.*"))

            assertThat("HATEOAS/HAL pageLink $pageLink has a null URL", notNullValue())
        }

        val size = xmlPath.getString("PagedModel.page.size")
        val totalElements = xmlPath.getString("PagedModel.page.totalElements")
        val totalPages = xmlPath.getString("PagedModel.page.totalPages")
        val number = xmlPath.getString("PagedModel.page.number")

        assertThat(size.toInt(), `is`(12))

        assertThat(number.toInt(), `is`(3))

        assertTrue(totalElements.toInt() > 0, "totalElements should be greater than 0")
        assertTrue(totalPages.toInt() > 0, "totalPages should be greater than 0")
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
