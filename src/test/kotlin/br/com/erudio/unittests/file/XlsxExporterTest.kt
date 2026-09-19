package br.com.erudio.unittests.file

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.impl.XlsxExporter
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class XlsxExporterTest {

    private val header = listOf("ID", "First Name", "Last Name", "Address", "Gender", "Enabled")

    private val exporter = XlsxExporter()
    private val formatter = DataFormatter()

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

    private fun rowValues(row: Row): List<String?> {
        val values: MutableList<String?> = ArrayList()
        for (i in header.indices) {
            val cell = row.getCell(i)
            values.add(if (cell == null) null else formatter.formatCellValue(cell))
        }
        return values
    }

    private fun firstSheet(workbook: Workbook): Sheet? {
        return workbook.getSheet("People")
    }

    @Test
    fun writesASheetNamedPeopleWithTheHeaderAndOneRowPerPerson() {
        val resource = exporter.exportPeople(
            listOf(
                person(1L, "Ayrton", "Senna", "São Paulo - Brasil", "Male", true),
                person(2L, "Marie", "Curie", "Warsaw - Poland", "Female", false)
            )
        )

        resource.inputStream.use { input ->
            WorkbookFactory.create(input).use { workbook ->
                assertEquals(1, workbook.numberOfSheets)
                val sheet = firstSheet(workbook)
                assertNotNull(sheet, "the sheet must be named People")

                assertEquals(2, sheet!!.lastRowNum)
                assertEquals(header, rowValues(sheet.getRow(0)))
                assertEquals(listOf("1", "Ayrton", "Senna", "São Paulo - Brasil", "Male", "Yes"), rowValues(sheet.getRow(1)))
                assertEquals(listOf("2", "Marie", "Curie", "Warsaw - Poland", "Female", "No"), rowValues(sheet.getRow(2)))
            }
        }
    }

    @Test
    fun writesTheIdAsANumber() {
        val resource = exporter.exportPeople(listOf(person(42L, "Ada", "Lovelace", "London", "Female", true)))

        resource.inputStream.use { input ->
            WorkbookFactory.create(input).use { workbook ->
                val id = firstSheet(workbook)!!.getRow(1).getCell(0)

                assertEquals(CellType.NUMERIC, id.cellType)
                assertEquals(42.0, id.numericCellValue)
            }
        }
    }

    @Test
    fun writesNoWhenTheEnabledFlagIsMissing() {
        val resource = exporter.exportPeople(listOf(person(3L, "Alan", "Turing", "Wilmslow", "Male", null)))

        resource.inputStream.use { input ->
            WorkbookFactory.create(input).use { workbook ->
                assertEquals("No", formatter.formatCellValue(firstSheet(workbook)!!.getRow(1).getCell(5)))
            }
        }
    }

    @Test
    fun writesTheHeaderInBold() {
        val resource = exporter.exportPeople(listOf())

        resource.inputStream.use { input ->
            WorkbookFactory.create(input).use { workbook ->
                val headerCell = firstSheet(workbook)!!.getRow(0).getCell(0)
                val font = workbook.getFontAt(headerCell.cellStyle.fontIndex)

                assertTrue(font.bold)
            }
        }
    }

    @Test
    fun writesOnlyTheHeaderWhenThereIsNobody() {
        val resource = exporter.exportPeople(listOf())

        resource.inputStream.use { input ->
            WorkbookFactory.create(input).use { workbook ->
                val sheet = firstSheet(workbook)!!

                assertEquals(0, sheet.lastRowNum)
                assertEquals(header, rowValues(sheet.getRow(0)))
            }
        }
    }

    @Test
    fun exportingASinglePersonIsNotSupported() {
        assertNull(exporter.exportPerson(person(1L, "Ayrton", "Senna", "x", "Male", true)))
    }
}
