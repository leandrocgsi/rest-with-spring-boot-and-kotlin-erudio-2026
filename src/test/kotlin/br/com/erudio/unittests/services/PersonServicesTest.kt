package br.com.erudio.unittests.services

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.repository.PersonRepository
import br.com.erudio.services.PersonService
import br.com.erudio.unittests.mapper.mocks.MockPerson
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.PagedModel
import java.util.*

@ExtendWith(MockitoExtension::class)
class PersonServicesTest {

    private lateinit var input: MockPerson

    @InjectMocks
    private lateinit var service: PersonService

    @Mock
    private lateinit var repository: PersonRepository

    @Mock
    private lateinit var assembler: PagedResourcesAssembler<PersonDTO>

    @BeforeEach
    fun setUp() {
        input = MockPerson()
    }

    private fun assertHasLink(person: PersonDTO, rel: String, type: String, hrefEndsWith: String) {
        assertTrue(
            person.links.any { link ->
                link.rel.value() == rel && link.href.endsWith(hrefEndsWith) && link.type == type
            },
            "link '$rel' ($type, ...$hrefEndsWith) is missing from ${person.links}"
        )
    }

    private fun assertHasFindAllLink(person: PersonDTO) {
        assertTrue(
            person.links.any { link ->
                link.rel.value() == "findAll" && link.href.contains("/api/person/v1") && link.type == "GET"
            },
            "link 'findAll' is missing from ${person.links}"
        )
    }

    @Test
    fun findById() {

        val person = input.mockEntity(1)
        person.id = 1L
        whenever(repository.findById(1L)).thenReturn(Optional.of(person))

        val result = service.findById(1L)

        assertNotNull(result)
        assertNotNull(result.id)
        assertNotNull(result.links)

        assertHasLink(result, "self", "GET", "/api/person/v1/1")
        assertHasFindAllLink(result)
        assertHasLink(result, "create", "POST", "/api/person/v1")
        assertHasLink(result, "update", "PUT", "/api/person/v1")
        assertHasLink(result, "delete", "DELETE", "/api/person/v1/1")

        assertEquals("Address Test1", result.address)
        assertEquals("First Name Test1", result.firstName)
        assertEquals("Last Name Test1", result.lastName)
        assertEquals("Female", result.gender)
    }

    @Test
    fun create() {
        val person = input.mockEntity(1)
        val persisted = person
        persisted.id = 1L

        val dto = input.mockDTO(1)

        whenever(repository.save(person)).thenReturn(persisted)

        val result = service.create(dto)

        assertNotNull(result)
        assertNotNull(result.id)
        assertNotNull(result.links)

        assertHasLink(result, "self", "GET", "/api/person/v1/1")
        assertHasFindAllLink(result)
        assertHasLink(result, "create", "POST", "/api/person/v1")
        assertHasLink(result, "update", "PUT", "/api/person/v1")
        assertHasLink(result, "delete", "DELETE", "/api/person/v1/1")

        assertEquals("Address Test1", result.address)
        assertEquals("First Name Test1", result.firstName)
        assertEquals("Last Name Test1", result.lastName)
        assertEquals("Female", result.gender)
    }

    @Test
    fun testCreateWithNullPerson() {
        val exception = assertThrows(RequiredObjectIsNullException::class.java) {
            service.create(null)
        }

        val expectedMessage = "It is not allowed to persist a null object!"
        val actualMessage = exception.message

        assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun update() {
        val person = input.mockEntity(1)
        val persisted = person
        persisted.id = 1L

        val dto = input.mockDTO(1)

        whenever(repository.findById(1L)).thenReturn(Optional.of(person))
        whenever(repository.save(person)).thenReturn(persisted)

        val result = service.update(dto)

        assertNotNull(result)
        assertNotNull(result.id)
        assertNotNull(result.links)

        assertHasLink(result, "self", "GET", "/api/person/v1/1")
        assertHasFindAllLink(result)
        assertHasLink(result, "create", "POST", "/api/person/v1")
        assertHasLink(result, "update", "PUT", "/api/person/v1")
        assertHasLink(result, "delete", "DELETE", "/api/person/v1/1")

        assertEquals("Address Test1", result.address)
        assertEquals("First Name Test1", result.firstName)
        assertEquals("Last Name Test1", result.lastName)
        assertEquals("Female", result.gender)
    }

    @Test
    fun testUpdateWithNullPerson() {
        val exception = assertThrows(RequiredObjectIsNullException::class.java) {
            service.update(null)
        }

        val expectedMessage = "It is not allowed to persist a null object!"
        val actualMessage = exception.message

        assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun delete() {
        val person = input.mockEntity(1)
        person.id = 1L
        whenever(repository.findById(1L)).thenReturn(Optional.of(person))

        service.delete(1L)
        verify(repository, times(1)).findById(anyOrNull())
        verify(repository, times(1)).delete(any())
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun findAll() {

        val mockEntityList = input.mockEntityList()
        val mockPage = PageImpl(mockEntityList)
        whenever(repository.findAll(any<Pageable>())).thenReturn(mockPage)

        val mockDtoList = input.mockDTOList()

        val entityModels = mockDtoList.map { EntityModel.of(it) }

        val pageMetadata = PagedModel.PageMetadata(
            mockPage.size.toLong(),
            mockPage.number.toLong(),
            mockPage.totalElements,
            mockPage.totalPages.toLong()
        )

        val mockPagedModel = PagedModel.of(entityModels, pageMetadata)
        whenever(assembler.toModel(any<Page<PersonDTO>>(), any<Link>())).thenReturn(mockPagedModel)

        val result = service.findAll(PageRequest.of(0, 14))

        val people = result.content.map { it.content }

        assertNotNull(people)
        assertEquals(14, people.size)

        validateIndividualPerson(people[1], 1)
        validateIndividualPerson(people[4], 4)
        validateIndividualPerson(people[7], 7)

        val pageCaptor = argumentCaptor<Page<PersonDTO>>()
        verify(assembler).toModel(pageCaptor.capture(), any<Link>())
        val linkedPeople = pageCaptor.firstValue.content
        assertEquals(14, linkedPeople.size)
        linkedPeople.forEachIndexed { i, person ->
            assertHasLink(person, "self", "GET", "/api/person/v1/$i")
            assertHasFindAllLink(person)
            assertHasLink(person, "create", "POST", "/api/person/v1")
            assertHasLink(person, "update", "PUT", "/api/person/v1")
            assertHasLink(person, "delete", "DELETE", "/api/person/v1/$i")
        }
    }

    private fun validateIndividualPerson(person: PersonDTO, i: Int) {
        assertNotNull(person)
        assertNotNull(person.id)
        assertNotNull(person.links)

        assertEquals("Address Test$i", person.address)
        assertEquals("First Name Test$i", person.firstName)
        assertEquals("Last Name Test$i", person.lastName)
        assertEquals(if (i % 2 == 0) "Male" else "Female", person.gender)
    }
}
