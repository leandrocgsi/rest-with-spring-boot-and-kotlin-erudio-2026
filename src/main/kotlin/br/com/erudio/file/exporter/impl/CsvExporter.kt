package br.com.erudio.file.exporter.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.contract.PersonExporter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

@Component
class CsvExporter : PersonExporter {

    @Throws(Exception::class)
    override fun exportPeople(people: List<PersonDTO>): Resource {
        val outputStream = ByteArrayOutputStream()
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

        val csvFormat = CSVFormat.Builder.create()
            .setHeader("ID", "First Name", "Last Name", "Address", "Gender", "Enabled")
            .setSkipHeaderRecord(false)
            .get()

        CSVPrinter(writer, csvFormat).use { csvPrinter ->
            for (person in people) {
                csvPrinter.printRecord(
                    person.id,
                    person.firstName,
                    person.lastName,
                    person.address,
                    person.gender,
                    person.enabled
                )
            }
        }
        return ByteArrayResource(outputStream.toByteArray())
    }

    @Throws(Exception::class)
    override fun exportPerson(person: PersonDTO): Resource? {
        return null
    }
}
