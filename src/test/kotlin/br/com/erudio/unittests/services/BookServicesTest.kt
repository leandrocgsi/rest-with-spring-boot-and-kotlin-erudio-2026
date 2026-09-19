package br.com.erudio.unittests.services

import br.com.erudio.data.dto.BookDTO
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.model.Book
import br.com.erudio.repository.BookRepository
import br.com.erudio.services.BookService
import br.com.erudio.unittests.mapper.mocks.MockBook
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
class BookServicesTest {

    private lateinit var input: MockBook

    @InjectMocks
    private lateinit var service: BookService

    @Mock
    private lateinit var repository: BookRepository

    @Mock
    private lateinit var assembler: PagedResourcesAssembler<BookDTO>

    @BeforeEach
    fun setUp() {
        input = MockBook()
    }

    private fun assertHasLink(book: BookDTO, rel: String, type: String, hrefEndsWith: String) {
        assertTrue(
            book.links.any { link ->
                link.rel.value() == rel && link.href.endsWith(hrefEndsWith) && link.type == type
            },
            "link '$rel' ($type, ...$hrefEndsWith) is missing from ${book.links}"
        )
    }

    private fun assertHasFindAllLink(book: BookDTO) {
        assertTrue(
            book.links.any { link ->
                link.rel.value() == "findAll" && link.href.contains("/api/book/v1") && link.type == "GET"
            },
            "link 'findAll' is missing from ${book.links}"
        )
    }

    @Test
    fun findById() {

        val book = input.mockEntity(1)
        book.id = 1L
        whenever(repository.findById(1L)).thenReturn(Optional.of(book))

        val result = service.findById(1L)

        assertNotNull(result)
        assertNotNull(result.id)
        assertNotNull(result.links)

        assertHasLink(result, "self", "GET", "/api/book/v1/1")
        assertHasFindAllLink(result)
        assertHasLink(result, "create", "POST", "/api/book/v1")
        assertHasLink(result, "update", "PUT", "/api/book/v1")
        assertHasLink(result, "delete", "DELETE", "/api/book/v1/1")

        assertEquals("Some Author1", result.author)
        assertEquals(25.0, result.price)
        assertEquals("Some Title1", result.title)
        assertNotNull(result.launchDate)
    }

    @Test
    fun create() {

        val dto = input.mockDTO(1)

        val entity = input.mockEntity(1)

        whenever(repository.save(any<Book>())).thenReturn(entity)

        val result = service.create(dto)

        assertNotNull(result)
        assertNotNull(result.id)
        assertNotNull(result.links)

        assertHasLink(result, "self", "GET", "/api/book/v1/1")
        assertHasFindAllLink(result)
        assertHasLink(result, "create", "POST", "/api/book/v1")
        assertHasLink(result, "update", "PUT", "/api/book/v1")
        assertHasLink(result, "delete", "DELETE", "/api/book/v1/1")

        assertEquals("Some Author1", result.author)
        assertEquals(25.0, result.price)
        assertEquals("Some Title1", result.title)
        assertNotNull(result.launchDate)
    }

    @Test
    fun testCreateWithNullBook() {
        val exception = assertThrows(RequiredObjectIsNullException::class.java) {
            service.create(null)
        }

        val expectedMessage = "It is not allowed to persist a null object!"
        val actualMessage = exception.message

        assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun update() {
        val book = input.mockEntity(1)
        val persisted = book
        persisted.id = 1L

        val dto = input.mockDTO(1)

        whenever(repository.findById(1L)).thenReturn(Optional.of(book))
        whenever(repository.save(book)).thenReturn(persisted)

        val result = service.update(dto)

        assertNotNull(result)
        assertNotNull(result.id)
        assertNotNull(result.links)

        assertHasLink(result, "self", "GET", "/api/book/v1/1")
        assertHasFindAllLink(result)
        assertHasLink(result, "create", "POST", "/api/book/v1")
        assertHasLink(result, "update", "PUT", "/api/book/v1")
        assertHasLink(result, "delete", "DELETE", "/api/book/v1/1")

        assertEquals("Some Author1", result.author)
        assertEquals(25.0, result.price)
        assertEquals("Some Title1", result.title)
        assertNotNull(result.launchDate)
    }

    @Test
    fun testUpdateWithNullBook() {
        val exception = assertThrows(RequiredObjectIsNullException::class.java) {
            service.update(null)
        }

        val expectedMessage = "It is not allowed to persist a null object!"
        val actualMessage = exception.message

        assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun delete() {
        val book = input.mockEntity(1)
        book.id = 1L
        whenever(repository.findById(1L)).thenReturn(Optional.of(book))

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
        whenever(assembler.toModel(any<Page<BookDTO>>(), any<Link>())).thenReturn(mockPagedModel)

        val result = service.findAll(PageRequest.of(0, 14))

        val books = result.content.map { it.content }

        assertNotNull(books)
        assertEquals(14, books.size)

        validateIndividualBook(books[1], 1)
        validateIndividualBook(books[4], 4)
        validateIndividualBook(books[7], 7)

        val pageCaptor = argumentCaptor<Page<BookDTO>>()
        verify(assembler).toModel(pageCaptor.capture(), any<Link>())
        val linkedBooks = pageCaptor.firstValue.content
        assertEquals(14, linkedBooks.size)
        linkedBooks.forEachIndexed { i, book ->
            assertHasLink(book, "self", "GET", "/api/book/v1/$i")
            assertHasFindAllLink(book)
            assertHasLink(book, "create", "POST", "/api/book/v1")
            assertHasLink(book, "update", "PUT", "/api/book/v1")
            assertHasLink(book, "delete", "DELETE", "/api/book/v1/$i")
        }
    }

    private fun validateIndividualBook(book: BookDTO, i: Int) {
        assertNotNull(book)
        assertNotNull(book.id)
        assertNotNull(book.links)

        assertEquals("Some Author$i", book.author)
        assertEquals(25.0, book.price)
        assertEquals("Some Title$i", book.title)
        assertNotNull(book.launchDate)
    }
}
