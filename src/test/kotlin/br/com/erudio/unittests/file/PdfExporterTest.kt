package br.com.erudio.unittests.file

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.impl.PdfExporter
import br.com.erudio.model.Book
import br.com.erudio.services.QRCodeService
import br.com.erudio.testsupport.NetworkAssumptions.assumeReportImagesAreReachable
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.Resource
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

class PdfExporterTest {

    private val exporter = PdfExporter()

    @BeforeEach
    fun setUp() {
        assumeReportImagesAreReachable()
        ReflectionTestUtils.setField(exporter, "service", QRCodeService())
    }

    private fun person(id: Long, firstName: String, lastName: String): PersonDTO {
        val person = PersonDTO()
        person.id = id
        person.firstName = firstName
        person.lastName = lastName
        person.address = "Address of $firstName"
        person.gender = "Male"
        person.enabled = true
        return person
    }

    private fun book(id: Long, title: String, author: String): Book {
        val book = Book()
        book.id = id
        book.title = title
        book.author = author
        book.price = 49.9
        book.launchDate = Date(1_511_963_405_878L)
        return book
    }

    private data class ParsedPdf(val pages: Int, val text: String)

    private fun read(pdf: Resource): ParsedPdf {
        val reader = PdfReader(pdf.contentAsByteArray)
        try {
            val extractor = PdfTextExtractor(reader)
            val text = StringBuilder()
            for (page in 1..reader.numberOfPages) {
                text.append(extractor.getTextFromPage(page)).append('\n')
            }
            return ParsedPdf(reader.numberOfPages, text.toString())
        } finally {
            reader.close()
        }
    }

    @Test
    fun exportPeopleGeneratesAPdfWithThePeople() {
        val resource = exporter.exportPeople(
            listOf(
                person(1L, "Ayrton", "Senna"),
                person(2L, "Marie", "Curie")
            )
        )

        val bytes = resource.contentAsByteArray
        assertEquals("%PDF", String(bytes, 0, 4, Charsets.US_ASCII))

        val pdf = read(resource)
        assertEquals(1, pdf.pages)
        assertTrue(pdf.text.contains("PEOPLE REPORT"), pdf.text)
        assertTrue(pdf.text.contains("Ayrton"), pdf.text)
        assertTrue(pdf.text.contains("Marie"), pdf.text)
    }

    @Test
    fun exportPeopleShowsTheNameOfTheCourseInThePageHeader() {
        val pdf = read(exporter.exportPeople(listOf(person(1L, "Ayrton", "Senna"))))

        assertTrue(pdf.text.contains("Spring Boot com Kotlin"), pdf.text)
        assertFalse(pdf.text.contains("Spring Boot 2026"), "the old course name must be gone: " + pdf.text)
    }

    @Test
    fun exportPeopleSpansSeveralPagesWhenThereAreManyPeople() {
        val people: MutableList<PersonDTO> = ArrayList()
        for (i in 1L..120L) {
            people.add(person(i, "Person$i", "Number"))
        }

        val pdf = read(exporter.exportPeople(people))

        assertTrue(pdf.pages > 1, "120 people cannot fit in a single page but got " + pdf.pages)
        assertTrue(pdf.text.contains("Person1 "), pdf.text)
        assertTrue(pdf.text.contains("Person120"), pdf.text)
    }

    @Test
    fun exportPeopleGeneratesAValidPdfEvenWithoutPeople() {
        val resource = exporter.exportPeople(listOf())

        assertEquals("%PDF", String(resource.contentAsByteArray, 0, 4, Charsets.US_ASCII))
        val pdf = read(resource)
        assertEquals(1, pdf.pages)
        assertFalse(pdf.text.contains("Ayrton"), pdf.text)
    }

    @Test
    fun exportPersonGeneratesAPdfWithThePersonAndTheirBooks() {
        val person = person(1L, "Ayrton", "Senna")
        person.profileUrl = "https://en.wikipedia.org/wiki/Ayrton_Senna"
        person.photoUrl = "https://raw.githubusercontent.com/leandrocgsi/rest-with-spring-boot-and-java-erudio/refs/heads/main/photos/01_senna.jpg"
        person.books = listOf(
            book(1L, "Working effectively with legacy code", "Michael C. Feathers"),
            book(2L, "Clean Code", "Robert C. Martin")
        )

        val resource = exporter.exportPerson(person)

        assertEquals("%PDF", String(resource.contentAsByteArray, 0, 4, Charsets.US_ASCII))
        val pdf = read(resource)
        assertTrue(pdf.text.contains("Ayrton"), pdf.text)
        assertTrue(pdf.text.contains("Clean Code"), pdf.text)
        assertTrue(pdf.text.contains("Working effectively"), pdf.text)
    }
}
