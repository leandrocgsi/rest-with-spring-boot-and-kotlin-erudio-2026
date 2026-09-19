package br.com.erudio.integrationtests.controllers.report

import br.com.erudio.file.exporter.MediaTypes
import br.com.erudio.integrationtests.AuthenticatedIntegrationTest
import br.com.erudio.testsupport.NetworkAssumptions.assumeReportImagesAreReachable
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import io.restassured.RestAssured.given
import io.restassured.path.json.JsonPath
import io.restassured.response.ValidatableResponse
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.*

class PersonControllerFilesTest : AuthenticatedIntegrationTest() {

    companion object {
        private const val BASE = "/api/person/v1"
        private const val XLSX = MediaTypes.APPLICATION_XLSX_VALUE
        private const val CSV = MediaTypes.APPLICATION_CSV_VALUE
        private const val PDF = MediaTypes.APPLICATION_PDF_VALUE
        private val EXPORT_HEADER = listOf("ID", "First Name", "Last Name", "Address", "Gender", "Enabled")

        private fun idsOfThePage(page: Int, size: Int, direction: String): List<Int> {
            return JsonPath.from(
                given().spec(authenticated())
                    .queryParam("page", page).queryParam("size", size).queryParam("direction", direction)
                    .accept("application/json")
                    .`when`()
                    .get(BASE)
                    .then()
                    .statusCode(200)
                    .extract().asString()
            ).getList("_embedded.people.id")
        }

        private fun firstNamesOfThePage(page: Int, size: Int, direction: String): List<String> {
            return JsonPath.from(
                given().spec(authenticated())
                    .queryParam("page", page).queryParam("size", size).queryParam("direction", direction)
                    .accept("application/json")
                    .`when`()
                    .get(BASE)
                    .then()
                    .statusCode(200)
                    .extract().asString()
            ).getList("_embedded.people.firstName")
        }

        private fun export(accept: String, page: Int, size: Int, direction: String): ByteArray {
            return given().spec(authenticated())
                .header("Accept", accept)
                .queryParam("page", page).queryParam("size", size).queryParam("direction", direction)
                .`when`()
                .get("$BASE/exportPage")
                .then()
                .statusCode(200)
                .extract().asByteArray()
        }

        private fun parseCsv(csv: ByteArray, expectedHeader: List<String>): List<CSVRecord> {
            InputStreamReader(ByteArrayInputStream(csv), Charsets.UTF_8).use { reader ->
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader).use { parser ->
                    assertEquals(expectedHeader, parser.headerNames)
                    return parser.records
                }
            }
        }

        private fun pdfText(pdf: ByteArray): String {
            val reader = PdfReader(pdf)
            try {
                val extractor = PdfTextExtractor(reader)
                val text = StringBuilder()
                for (page in 1..reader.numberOfPages) {
                    text.append(extractor.getTextFromPage(page)).append('\n')
                }
                return text.toString()
            } finally {
                reader.close()
            }
        }

        private fun tag(): String {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 10)
        }

        private fun xlsx(rows: Array<Array<String>>): ByteArray {
            XSSFWorkbook().use { workbook ->
                ByteArrayOutputStream().use { out ->
                    val sheet = workbook.createSheet("People")
                    for (r in rows.indices) {
                        val row = sheet.createRow(r)
                        for (c in rows[r].indices) {
                            row.createCell(c).setCellValue(rows[r][c])
                        }
                    }
                    workbook.write(out)
                    return out.toByteArray()
                }
            }
        }

        private fun massCreation(name: String, content: ByteArray, contentType: String): ValidatableResponse {
            return given().spec(authenticated())
                .multiPart("file", name, content, contentType)
                .`when`()
                .post("$BASE/massCreation")
                .then()
        }

        private fun peopleNamed(tag: String): Int {
            return given().spec(authenticated())
                .accept("application/json")
                .`when`()
                .get("$BASE/findPeopleByName/$tag")
                .then()
                .statusCode(200)
                .extract().path("page.totalElements")
        }
    }

    private val createdIds: MutableList<Long> = ArrayList()

    @AfterEach
    fun deleteWhatTheTestImported() {
        createdIds.forEach { id -> given().spec(authenticated()).delete("$BASE/$id").then().statusCode(204) }
        createdIds.clear()
    }

    private fun remember(created: ValidatableResponse) {
        created.extract().jsonPath().getList("id", Long::class.java).forEach { createdIds.add(it) }
    }

    @Test
    fun exportPageAsCsvHasOneLinePerPersonOfThePageInTheSameOrderAsTheListing() {
        val csv = export(CSV, 0, 5, "asc")

        val records = parseCsv(csv, EXPORT_HEADER)
        assertEquals(5, records.size)
        assertEquals(
            idsOfThePage(0, 5, "asc").map { it.toString() },
            records.map { record -> record.get("ID") })
        assertEquals(
            firstNamesOfThePage(0, 5, "asc"),
            records.map { record -> record.get("First Name") })
    }

    @Test
    fun exportPageAsCsvHasTheHeadersAndTheDownloadName() {
        given().spec(authenticated())
            .header("Accept", CSV)
            .queryParam("size", 2)
            .`when`()
            .get("$BASE/exportPage")
            .then()
            .statusCode(200)
            .contentType(startsWith("text/csv"))
            .header("Content-Disposition", equalTo("attachment; filename=\"people_exported.csv\""))
            .body(startsWith("ID,First Name,Last Name,Address,Gender,Enabled"))
    }

    @Test
    fun exportPageRespectsThePageSizeAndTheDirection() {
        val ascending = parseCsv(export(CSV, 0, 3, "asc"), EXPORT_HEADER)
        val descending = parseCsv(export(CSV, 0, 3, "desc"), EXPORT_HEADER)
        val secondPage = parseCsv(export(CSV, 1, 3, "asc"), EXPORT_HEADER)

        assertEquals(3, ascending.size)
        assertEquals(3, descending.size)
        assertEquals(3, secondPage.size)
        assertNotEquals(ascending[0].get("ID"), descending[0].get("ID"))
        assertEquals(
            idsOfThePage(0, 3, "desc").map { it.toString() },
            descending.map { record -> record.get("ID") })
        assertEquals(
            idsOfThePage(1, 3, "asc").map { it.toString() },
            secondPage.map { record -> record.get("ID") })
    }

    @Test
    fun exportPageOutsideTheDataIsJustTheHeader() {
        assertTrue(parseCsv(export(CSV, 99999, 5, "asc"), EXPORT_HEADER).isEmpty())
    }

    @Test
    fun exportPageAsXlsxHasTheSameRowsAsTheListing() {
        val bytes = export(XLSX, 0, 5, "asc")

        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheet("People")
            assertNotNull(sheet)
            assertEquals(5, sheet.lastRowNum)

            val formatter = DataFormatter()
            val header: MutableList<String> = ArrayList()
            sheet.getRow(0).forEach { cell -> header.add(formatter.formatCellValue(cell)) }
            assertEquals(EXPORT_HEADER, header)

            val expectedNames = firstNamesOfThePage(0, 5, "asc")
            val expectedIds = idsOfThePage(0, 5, "asc")
            for (i in 0 until 5) {
                val row = sheet.getRow(i + 1)
                assertEquals(expectedIds[i].toDouble(), row.getCell(0).numericCellValue)
                assertEquals(expectedNames[i], formatter.formatCellValue(row.getCell(1)))
                assertThat(formatter.formatCellValue(row.getCell(5)), anyOf(`is`("Yes"), `is`("No")))
            }
        }
    }

    @Test
    fun exportPageAsXlsxHasTheContentTypeAndTheDownloadName() {
        given().spec(authenticated())
            .header("Accept", XLSX)
            .queryParam("size", 2)
            .`when`()
            .get("$BASE/exportPage")
            .then()
            .statusCode(200)
            .contentType(XLSX)
            .header("Content-Disposition", equalTo("attachment; filename=\"people_exported.xlsx\""))
    }

    @Test
    fun exportPageAsPdfIsAValidPdfWithThePeopleOfThePage() {
        assumeReportImagesAreReachable()

        val pdf = given().spec(authenticated())
            .header("Accept", PDF)
            .queryParam("page", 0).queryParam("size", 5)
            .`when`()
            .get("$BASE/exportPage")
            .then()
            .statusCode(200)
            .contentType(PDF)
            .header("Content-Disposition", equalTo("attachment; filename=\"people_exported.pdf\""))
            .extract().asByteArray()

        assertEquals("%PDF", String(pdf, 0, 4, Charsets.US_ASCII))
        val text = pdfText(pdf)
        assertTrue(text.contains("PEOPLE REPORT"), text)
        for (firstName in firstNamesOfThePage(0, 5, "asc")) {
            assertTrue(text.contains(firstName), "$firstName is missing from: $text")
        }
    }

    @Test
    fun exportOnePersonAsPdfIsAValidPdfAboutThatPerson() {
        assumeReportImagesAreReachable()
        val firstName: String = given().spec(authenticated()).accept("application/json").get("$BASE/1")
            .then().statusCode(200).extract().path("firstName")

        val pdf = given().spec(authenticated())
            .header("Accept", PDF)
            .`when`()
            .get("$BASE/export/1")
            .then()
            .statusCode(200)
            .contentType(PDF)
            .header("Content-Disposition", containsString(".pdf"))
            .extract().asByteArray()

        assertEquals("%PDF", String(pdf, 0, 4, Charsets.US_ASCII))
        assertTrue(pdfText(pdf).contains(firstName))
    }

    @Test
    fun exportOnePersonThatDoesNotExistIsNotFound() {
        given().spec(authenticated())
            .header("Accept", "$PDF, application/json")
            .`when`()
            .get("$BASE/export/999999")
            .then()
            .statusCode(404)
            .body("message", equalTo("No records found for this ID!"))
    }

    @Test
    fun exportOnePersonThatDoesNotExistAsksingOnlyForPdfGetsAnEmptyForbidden() {
        given().spec(authenticated())
            .header("Accept", PDF)
            .`when`()
            .get("$BASE/export/999999")
            .then()
            .statusCode(403)
            .body(emptyOrNullString())
    }

    @Test
    fun aSinglePersonOnlyExportsToPdf() {
        given().spec(authenticated())
            .header("Accept", CSV)
            .`when`()
            .get("$BASE/export/1")
            .then()
            .statusCode(406)
    }

    @Test
    fun exportPageRejectsAFormatItCannotProduce() {
        given().spec(authenticated())
            .header("Accept", "application/json")
            .`when`()
            .get("$BASE/exportPage")
            .then()
            .statusCode(both(greaterThanOrEqualTo(400)).and(lessThan(500)))
    }

    @Test
    fun exportsRequireAuthentication() {
        given().spec(anonymous()).header("Accept", CSV).get("$BASE/exportPage").then().statusCode(403)
        given().spec(anonymous()).header("Accept", PDF).get("$BASE/export/1").then().statusCode(403)
    }

    @Test
    fun massCreationFromACsvCreatesEveryPersonOfTheFile() {
        val tag = tag()
        val csv = "first_name,last_name,address,gender\n" +
                "Imp${tag}A,Csv,Street 1,Female\n" +
                "Imp${tag}B,Csv,\"Street 2, apto 3\",Male\n"

        val created = massCreation("people.csv", csv.toByteArray(Charsets.UTF_8), "text/csv")
            .statusCode(200)
            .body("size()", `is`(2))
            .body("firstName", contains("Imp${tag}A", "Imp${tag}B"))
            .body("lastName", everyItem(`is`("Csv")))
            .body("address", contains("Street 1", "Street 2, apto 3"))
            .body("gender", contains("Female", "Male"))
            .body("enabled", everyItem(`is`(true)))
            .body("id", everyItem(notNullValue()))
            .body("links", everyItem(not(empty<Any>())))
        remember(created)

        assertEquals(2, peopleNamed("imp$tag"))
    }

    @Test
    fun massCreationFromACsvKeepsTheAccents() {
        val tag = tag()
        val csv = "first_name,last_name,address,gender\nJoão$tag,Conceição,Avenida São João,Male\n"

        val created = massCreation("people.csv", csv.toByteArray(Charsets.UTF_8), "text/csv")
            .statusCode(200)
            .body("[0].firstName", equalTo("João$tag"))
            .body("[0].lastName", equalTo("Conceição"))
            .body("[0].address", equalTo("Avenida São João"))
        remember(created)

        given().spec(authenticated()).accept("application/json")
            .get("$BASE/" + created.extract().path<Any>("[0].id"))
            .then().statusCode(200)
            .body("firstName", equalTo("João$tag"))
            .body("address", equalTo("Avenida São João"))
    }

    @Test
    fun massCreationFromACsvWithAMissingColumnCreatesNobody() {
        val tag = tag()
        val csv = "first_name,last_name,address\nImp$tag,Csv,Street 1\n"

        massCreation("people.csv", csv.toByteArray(Charsets.UTF_8), "text/csv")
            .statusCode(500)
            .body("message", equalTo("Error processing the file!"))

        assertEquals(0, peopleNamed("imp$tag"))
    }

    @Test
    fun massCreationFromAnXlsxCreatesEveryPersonOfTheFile() {
        val tag = tag()
        val workbook = xlsx(
            arrayOf(
                arrayOf("first_name", "last_name", "address", "gender"),
                arrayOf("Imp${tag}A", "Xlsx", "Street 1", "Female"),
                arrayOf("Imp${tag}B", "Xlsx", "Street 2", "Male"),
                arrayOf("Imp${tag}C", "Xlsx", "Street 3", "Female")
            )
        )

        val created = massCreation("people.xlsx", workbook, XLSX)
            .statusCode(200)
            .body("size()", `is`(3))
            .body("firstName", contains("Imp${tag}A", "Imp${tag}B", "Imp${tag}C"))
            .body("lastName", everyItem(`is`("Xlsx")))
            .body("enabled", everyItem(`is`(true)))
            .body("id", everyItem(notNullValue()))
        remember(created)

        assertEquals(3, peopleNamed("imp$tag"))
    }

    @Test
    fun massCreationCanAnswerInXml() {
        val tag = tag()
        val workbook = xlsx(
            arrayOf(
                arrayOf("first_name", "last_name", "address", "gender"),
                arrayOf("Imp$tag", "Xml", "Street 1", "Male")
            )
        )

        val response = given().spec(authenticated())
            .header("Accept", "application/xml")
            .multiPart("file", "people.xlsx", workbook, XLSX)
            .`when`()
            .post("$BASE/massCreation")
            .then()
            .statusCode(200)
            .contentType(containsString("xml"))
            .body("List.item.firstName", equalTo("Imp$tag"))
        response.extract().xmlPath().getList("List.item.id", Long::class.java).forEach { createdIds.add(it) }
    }

    @Test
    fun massCreationOfAnUnsupportedFileTypeIsRejected() {
        massCreation("people.txt", "first_name,last_name,address,gender\nX,Y,Z,Male\n".toByteArray(Charsets.UTF_8), "text/plain")
            .statusCode(500)
            .body("message", equalTo("Error processing the file!"))
    }

    @Test
    fun massCreationOfAnEmptyFileIsABadRequest() {
        massCreation("people.csv", ByteArray(0), "text/csv")
            .statusCode(400)
            .body("message", equalTo("Please set a Valid File!"))
    }

    @Test
    fun massCreationWithoutAFileIsRejected() {
        given().spec(authenticated())
            .multiPart("notTheFile", "people.csv", "x".toByteArray(Charsets.UTF_8), "text/csv")
            .`when`()
            .post("$BASE/massCreation")
            .then()
            .statusCode(400)
    }

    @Test
    fun massCreationRequiresAuthentication() {
        given().spec(anonymous())
            .multiPart("file", "people.csv", "x".toByteArray(Charsets.UTF_8), "text/csv")
            .`when`()
            .post("$BASE/massCreation")
            .then()
            .statusCode(403)
    }
}
