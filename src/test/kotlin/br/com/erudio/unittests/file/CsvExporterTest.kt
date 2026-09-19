package br.com.erudio.unittests.file

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.impl.CsvExporter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.core.io.Resource
import java.io.InputStreamReader

class CsvExporterTest {

    private val header = listOf("ID", "First Name", "Last Name", "Address", "Gender", "Enabled")

    private val exporter = CsvExporter()

    private fun person(id: Long?, firstName: String?, lastName: String?, address: String?, gender: String?, enabled: Boolean?): PersonDTO {
        val person = PersonDTO()
        person.id = id
        person.firstName = firstName
        person.lastName = lastName
        person.address = address
        person.gender = gender
        person.enabled = enabled
        return person
    }

    private data class ParsedCsv(val header: List<String>, val records: List<CSVRecord>)

    private fun parse(resource: Resource): ParsedCsv {
        InputStreamReader(resource.inputStream, Charsets.UTF_8).use { reader ->
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader).use { parser ->
                return ParsedCsv(parser.headerNames, parser.records)
            }
        }
    }

    @Test
    fun writesTheHeaderAndOneLinePerPerson() {
        val csv = parse(
            exporter.exportPeople(
                listOf(
                    person(1L, "Ayrton", "Senna", "São Paulo - Brasil", "Male", true),
                    person(2L, "Leonardo", "da Vinci", "Vinci - Italy", "Male", false)
                )
            )
        )

        assertEquals(header, csv.header)
        assertEquals(2, csv.records.size)
        assertEquals(listOf("1", "Ayrton", "Senna", "São Paulo - Brasil", "Male", "true"), csv.records[0].toList())
        assertEquals(listOf("2", "Leonardo", "da Vinci", "Vinci - Italy", "Male", "false"), csv.records[1].toList())
    }

    @Test
    fun writesOnlyTheHeaderWhenThereIsNobody() {
        val csv = parse(exporter.exportPeople(listOf()))

        assertEquals(header, csv.header)
        assertTrue(csv.records.isEmpty())
    }

    @Test
    fun keepsCommasAndQuotesInsideAValue() {
        val address = "Rua \"A\", 10 - apto 2"

        val csv = parse(exporter.exportPeople(listOf(person(7L, "Ada", "Lovelace", address, "Female", true))))

        assertEquals(1, csv.records.size)
        assertEquals(address, csv.records[0].get("Address"))
        assertEquals(6, csv.records[0].size())
    }

    @Test
    fun encodesTheFileAsUtf8() {
        val resource = exporter.exportPeople(listOf(person(1L, "João", "Conceição", "Avenida São João", "Male", true)))

        assertTrue(String(resource.contentAsByteArray, Charsets.UTF_8).contains("João,Conceição,Avenida São João"))
    }

    @Test
    fun writesMissingValuesAsEmptyFields() {
        val csv = parse(exporter.exportPeople(listOf(person(3L, "Alan", "Turing", null, null, null))))

        val record = csv.records[0]
        assertEquals("", record.get("Address"))
        assertEquals("", record.get("Gender"))
        assertEquals("", record.get("Enabled"))
    }

    @Test
    fun exportingASinglePersonIsNotSupported() {
        assertNull(exporter.exportPerson(person(1L, "Ayrton", "Senna", "x", "Male", true)))
    }
}
