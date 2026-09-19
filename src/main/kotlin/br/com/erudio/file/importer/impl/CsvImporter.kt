package br.com.erudio.file.importer.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.importer.contract.FileImporter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.InputStreamReader

@Component
class CsvImporter : FileImporter {

    @Throws(Exception::class)
    override fun importFile(inputStream: InputStream): List<PersonDTO> {
        val format = CSVFormat.Builder.create()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get()

        val records: Iterable<CSVRecord> = format.parse(InputStreamReader(inputStream))
        return parseRecordsToPersonDTOs(records)
    }

    private fun parseRecordsToPersonDTOs(records: Iterable<CSVRecord>): List<PersonDTO> {
        val people: MutableList<PersonDTO> = ArrayList()

        for (record in records) {
            val person = PersonDTO()
            person.firstName = record.get("first_name")
            person.lastName = record.get("last_name")
            person.address = record.get("address")
            person.gender = record.get("gender")
            person.enabled = true
            people.add(person)
        }
        return people
    }
}
