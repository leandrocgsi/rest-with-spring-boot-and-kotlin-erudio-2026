package br.com.erudio.repository

import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationTest
import br.com.erudio.model.Person
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonRepositoryTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var repository: PersonRepository

    private lateinit var person: Person

    @BeforeAll
    fun setUp() {
        person = Person()
    }

    @Test
    @Order(1)
    fun findPeopleByName() {
        val pageable: Pageable = PageRequest.of(
            0,
            12,
            Sort.by(Sort.Direction.ASC, "firstName")
        )

        person = repository.findPeopleByName("iko", pageable).content[0]

        assertNotNull(person)
        assertNotNull(person.id)
        assertEquals("Nikola", person.firstName)
        assertEquals("Tesla", person.lastName)
        assertEquals("Male", person.gender)
        assertTrue(person.enabled!!)
    }

    @Test
    @Order(2)
    fun disablePerson() {

        val id = person.id!!
        repository.disablePerson(id)

        val result = repository.findById(id)
        person = result.get()

        assertNotNull(person)
        assertNotNull(person.id)
        assertEquals("Nikola", person.firstName)
        assertEquals("Tesla", person.lastName)
        assertEquals("Male", person.gender)
        assertFalse(person.enabled!!)
    }
}
