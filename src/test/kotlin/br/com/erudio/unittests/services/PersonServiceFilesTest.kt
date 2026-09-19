package br.com.erudio.unittests.services

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.exception.BadRequestException
import br.com.erudio.exception.FileStorageException
import br.com.erudio.exception.ResourceNotFoundException
import br.com.erudio.file.exporter.contract.PersonExporter
import br.com.erudio.file.exporter.factory.FileExporterFactory
import br.com.erudio.file.importer.contract.FileImporter
import br.com.erudio.file.importer.factory.FileImporterFactory
import br.com.erudio.model.Person
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@ExtendWith(MockitoExtension::class)
class PersonServiceFilesTest {

    companion object {
        private const val CSV = "text/csv"
    }

    private val input = MockPerson()

    @InjectMocks
    private lateinit var service: PersonService

    @Mock
    private lateinit var repository: PersonRepository

    @Mock
    private lateinit var importerFactory: FileImporterFactory

    @Mock
    private lateinit var exporterFactory: FileExporterFactory

    @Mock
    private lateinit var assembler: PagedResourcesAssembler<PersonDTO>

    private lateinit var personExporter: PersonExporter
    private lateinit var exported: Resource

    @BeforeEach
    fun setUp() {
        personExporter = mock()
        exported = ByteArrayResource("exported".toByteArray(Charsets.UTF_8))
    }

    private fun dto(firstName: String): PersonDTO {
        val person = PersonDTO()
        person.firstName = firstName
        person.lastName = "Last"
        person.address = "Address"
        person.gender = "Male"
        person.enabled = true
        return person
    }

    private fun csvFile(name: String, content: String): MultipartFile {
        return MockMultipartFile("file", name, CSV, content.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun exportPageExportsTheRequestedPageInTheFormatOfTheAcceptHeader() {
        val pageable = PageRequest.of(0, 3, Sort.by("firstName"))
        whenever(repository.findAll(pageable)).thenReturn(PageImpl(input.mockEntityList().subList(0, 3)))
        whenever(exporterFactory.getExporter(CSV)).thenReturn(personExporter)
        whenever(personExporter.exportPeople(any())).thenReturn(exported)

        val result = service.exportPage(pageable, CSV)

        assertSame(exported, result)
        val people = argumentCaptor<List<PersonDTO>>()
        verify(personExporter).exportPeople(people.capture())
        assertEquals(
            listOf("First Name Test0", "First Name Test1", "First Name Test2"),
            people.firstValue.map { it.firstName })
    }

    @Test
    fun exportPageWrapsAnyFailureKeepingTheCause() {
        val pageable = PageRequest.of(0, 3)
        whenever(repository.findAll(pageable)).thenReturn(PageImpl(listOf()))
        whenever(exporterFactory.getExporter("application/json")).thenThrow(BadRequestException("Invalid File Format!"))

        val exception = assertThrows(RuntimeException::class.java) { service.exportPage(pageable, "application/json") }

        assertEquals("Error during file export!", exception.message)
        assertInstanceOf(BadRequestException::class.java, exception.cause)
    }

    @Test
    fun exportPersonExportsThePersonThatWasFound() {
        val entity = input.mockEntity(1)
        whenever(repository.findById(1L)).thenReturn(Optional.of(entity))
        whenever(exporterFactory.getExporter("application/pdf")).thenReturn(personExporter)
        whenever(personExporter.exportPerson(any())).thenReturn(exported)

        val result = service.exportPerson(1L, "application/pdf")

        assertSame(exported, result)
        val person = argumentCaptor<PersonDTO>()
        verify(personExporter).exportPerson(person.capture())
        assertEquals("First Name Test1", person.firstValue.firstName)
    }

    @Test
    fun exportPersonFailsWhenThePersonDoesNotExist() {
        whenever(repository.findById(99L)).thenReturn(Optional.empty())

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            service.exportPerson(99L, "application/pdf")
        }

        assertEquals("No records found for this ID!", exception.message)
        verifyNoInteractions(exporterFactory)
    }

    @Test
    fun exportPersonWrapsAnyFailureKeepingTheCause() {
        whenever(repository.findById(1L)).thenReturn(Optional.of(input.mockEntity(1)))
        whenever(exporterFactory.getExporter("application/pdf")).thenReturn(personExporter)
        whenever(personExporter.exportPerson(any())).thenThrow(IllegalStateException("template broken"))

        val exception = assertThrows(RuntimeException::class.java) { service.exportPerson(1L, "application/pdf") }

        assertEquals("Error during file export!", exception.message)
        assertEquals("template broken", exception.cause!!.message)
    }

    @Test
    fun massCreationSavesEveryPersonOfTheFileAndReturnsThemWithLinks() {
        val fileImporter = mock<FileImporter>()
        whenever(importerFactory.getImporter("people.csv")).thenReturn(fileImporter)
        whenever(fileImporter.importFile(any())).thenReturn(listOf(dto("Ada"), dto("Alan")))
        val ids = AtomicLong(100)
        whenever(repository.save(any<Person>())).thenAnswer { invocation ->
            val saved = invocation.getArgument<Person>(0)
            saved.id = ids.incrementAndGet()
            saved
        }

        val created = service.massCreation(csvFile("people.csv", "irrelevant, the importer is a mock"))

        assertEquals(2, created.size)
        assertEquals(listOf("Ada", "Alan"), created.map { it.firstName })
        assertEquals(listOf(101L, 102L), created.map { it.id })
        created.forEach { person -> assertFalse(person.links.isEmpty, "every created person carries its HATEOAS links") }
        verify(repository, times(2)).save(any<Person>())
    }

    @Test
    fun massCreationOfAnEmptyFileIsABadRequest() {
        val exception = assertThrows(BadRequestException::class.java) {
            service.massCreation(csvFile("people.csv", ""))
        }

        assertEquals("Please set a Valid File!", exception.message)
        verifyNoInteractions(importerFactory, repository)
    }

    @Test
    fun massCreationReportsAnUnsupportedFileAsAStorageError() {
        whenever(importerFactory.getImporter("people.txt")).thenThrow(BadRequestException("Invalid File Format!"))

        val exception = assertThrows(FileStorageException::class.java) {
            service.massCreation(csvFile("people.txt", "whatever"))
        }

        assertEquals("Error processing the file!", exception.message)
        verifyNoInteractions(repository)
    }

    @Test
    fun massCreationReportsAnUnreadableFileAsAStorageError() {
        val fileImporter = mock<FileImporter>()
        whenever(importerFactory.getImporter("people.csv")).thenReturn(fileImporter)
        whenever(fileImporter.importFile(any())).thenThrow(IllegalArgumentException("Mapping for gender not found"))

        val exception = assertThrows(FileStorageException::class.java) {
            service.massCreation(csvFile("people.csv", "first_name\nAda\n"))
        }

        assertEquals("Error processing the file!", exception.message)
        verify(repository, never()).save(any<Person>())
    }

    @Test
    fun massCreationReportsAFileWithoutNameAsAStorageError() {
        val nameless = mock<MultipartFile>()
        whenever(nameless.isEmpty).thenReturn(false)
        whenever(nameless.inputStream).thenReturn(InputStream.nullInputStream())
        whenever(nameless.originalFilename).thenReturn(null)

        val exception = assertThrows(FileStorageException::class.java) { service.massCreation(nameless) }

        assertEquals("Error processing the file!", exception.message)
        verifyNoInteractions(importerFactory, repository)
    }
}
