package br.com.erudio.unittests.file

import br.com.erudio.file.importer.impl.XlsxImporter
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class XlsxImporterTest {

    private val importer = XlsxImporter()

    private fun workbook(fill: (Sheet) -> Unit): InputStream {
        XSSFWorkbook().use { workbook ->
            ByteArrayOutputStream().use { out ->
                fill(workbook.createSheet("People"))
                workbook.write(out)
                return ByteArrayInputStream(out.toByteArray())
            }
        }
    }

    private fun header(sheet: Sheet) {
        row(sheet, 0, "first_name", "last_name", "address", "gender")
    }

    private fun row(sheet: Sheet, index: Int, vararg values: String) {
        val row = sheet.createRow(index)
        for (i in values.indices) {
            row.createCell(i).setCellValue(values[i])
        }
    }

    @Test
    fun importsOnePersonPerRowSkippingTheHeader() {
        val people = importer.importFile(workbook { sheet ->
            header(sheet)
            row(sheet, 1, "Ada", "Lovelace", "London", "Female")
            row(sheet, 2, "Alan", "Turing", "Wilmslow", "Male")
        })

        assertEquals(2, people.size)
        val ada = people[0]
        assertEquals("Ada", ada.firstName)
        assertEquals("Lovelace", ada.lastName)
        assertEquals("London", ada.address)
        assertEquals("Female", ada.gender)
        assertEquals("Alan", people[1].firstName)
    }

    @Test
    fun importedPeopleAreEnabledAndHaveNoIdYet() {
        val people = importer.importFile(workbook { sheet ->
            header(sheet)
            row(sheet, 1, "Ada", "Lovelace", "London", "Female")
        })

        assertTrue(people[0].enabled!!)
        assertNull(people[0].id)
    }

    @Test
    fun theFirstRowIsAlwaysTheHeaderWhateverItContains() {
        val people = importer.importFile(workbook { sheet ->
            row(sheet, 0, "Grace", "Hopper", "New York", "Female")
            row(sheet, 1, "Ada", "Lovelace", "London", "Female")
        })

        assertEquals(1, people.size)
        assertEquals("Ada", people[0].firstName)
    }

    @Test
    fun skipsRowsWhoseFirstCellIsBlank() {
        val people = importer.importFile(workbook { sheet ->
            header(sheet)
            row(sheet, 1, "Ada", "Lovelace", "London", "Female")
            val blank: Row = sheet.createRow(2)
            blank.createCell(0, CellType.BLANK)
            blank.createCell(1).setCellValue("Ignored")
            row(sheet, 3, "Alan", "Turing", "Wilmslow", "Male")
        })

        assertEquals(listOf("Ada", "Alan"), people.map { it.firstName })
    }

    @Test
    fun skipsRowsThatDoNotExist() {
        val people = importer.importFile(workbook { sheet ->
            header(sheet)
            row(sheet, 1, "Ada", "Lovelace", "London", "Female")
            row(sheet, 5, "Alan", "Turing", "Wilmslow", "Male")
        })

        assertEquals(2, people.size)
    }

    @Test
    fun readsUtf8Accents() {
        val people = importer.importFile(workbook { sheet ->
            header(sheet)
            row(sheet, 1, "João", "Conceição", "Avenida São João", "Male")
        })

        assertEquals("João", people[0].firstName)
        assertEquals("Conceição", people[0].lastName)
        assertEquals("Avenida São João", people[0].address)
    }

    @Test
    fun importsManyRows() {
        val people = importer.importFile(workbook { sheet ->
            header(sheet)
            for (i in 1..250) {
                row(sheet, i, "Name$i", "Last$i", "Address $i", "Male")
            }
        })

        assertEquals(250, people.size)
        assertEquals("Name250", people[249].firstName)
    }

    @Test
    fun importsNothingFromAHeaderOnlySheet() {
        assertTrue(importer.importFile(workbook { sheet -> header(sheet) }).isEmpty())
    }

    @Test
    fun importsNothingFromAnEmptySheet() {
        assertTrue(importer.importFile(workbook { }).isEmpty())
    }

    @Test
    fun failsWhenANameCellIsNotText() {
        assertThrows(IllegalStateException::class.java) {
            importer.importFile(workbook { sheet ->
                header(sheet)
                val row = sheet.createRow(1)
                row.createCell(0).setCellValue(12345.0)
                row.createCell(1).setCellValue("Lovelace")
                row.createCell(2).setCellValue("London")
                row.createCell(3).setCellValue("Female")
            })
        }
    }

    @Test
    fun failsWhenTheContentIsNotAnXlsxFile() {
        assertThrows(Exception::class.java) {
            importer.importFile(ByteArrayInputStream("first_name,last_name\nAda,Lovelace\n".toByteArray()))
        }
    }
}
