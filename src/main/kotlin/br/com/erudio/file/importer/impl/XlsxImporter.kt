package br.com.erudio.file.importer.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.importer.contract.FileImporter
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class XlsxImporter : FileImporter {

    @Throws(Exception::class)
    override fun importFile(inputStream: InputStream): List<PersonDTO> {

        XSSFWorkbook(inputStream).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val rowIterator = sheet.iterator()

            if (rowIterator.hasNext()) rowIterator.next()

            return parseRowsToPersonDtoList(rowIterator)
        }
    }

    private fun parseRowsToPersonDtoList(rowIterator: Iterator<Row>): List<PersonDTO> {
        val people: MutableList<PersonDTO> = ArrayList()

        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            if (isRowValid(row)) {
                people.add(parseRowToPersonDto(row))
            }
        }
        return people
    }

    private fun parseRowToPersonDto(row: Row): PersonDTO {
        val person = PersonDTO()
        person.firstName = row.getCell(0).stringCellValue
        person.lastName = row.getCell(1).stringCellValue
        person.address = row.getCell(2).stringCellValue
        person.gender = row.getCell(3).stringCellValue
        person.enabled = true
        return person
    }

    private fun isRowValid(row: Row): Boolean {
        return row.getCell(0) != null && row.getCell(0).cellType != CellType.BLANK
    }
}
