package br.com.erudio.file.exporter.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.contract.PersonExporter
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class XlsxExporter : PersonExporter {

    @Throws(Exception::class)
    override fun exportPeople(people: List<PersonDTO>): Resource {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("People")

            val headerRow = sheet.createRow(0)
            val headers = arrayOf("ID", "First Name", "Last Name", "Address", "Gender", "Enabled")
            for (i in headers.indices) {
                val cell = headerRow.createCell(i)
                cell.setCellValue(headers[i])
                cell.cellStyle = createHeaderCellStyle(workbook)
            }

            var rowIndex = 1
            for (person in people) {
                val row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(person.id!!.toDouble())
                row.createCell(1).setCellValue(person.firstName)
                row.createCell(2).setCellValue(person.lastName)
                row.createCell(3).setCellValue(person.address)
                row.createCell(4).setCellValue(person.gender)
                row.createCell(5).setCellValue(if (person.enabled == true) "Yes" else "No")
            }

            for (i in headers.indices) {
                sheet.autoSizeColumn(i)
            }

            val outputStream = ByteArrayOutputStream()
            workbook.write(outputStream)

            return ByteArrayResource(outputStream.toByteArray())
        }
    }

    private fun createHeaderCellStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        style.setFont(font)
        style.alignment = HorizontalAlignment.CENTER
        return style
    }

    @Throws(Exception::class)
    override fun exportPerson(person: PersonDTO): Resource? {
        return null
    }
}
