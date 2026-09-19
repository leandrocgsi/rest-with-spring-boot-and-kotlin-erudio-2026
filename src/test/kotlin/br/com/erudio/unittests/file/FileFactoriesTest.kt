package br.com.erudio.unittests.file

import br.com.erudio.exception.BadRequestException
import br.com.erudio.file.exporter.MediaTypes
import br.com.erudio.file.exporter.factory.FileExporterFactory
import br.com.erudio.file.exporter.impl.CsvExporter
import br.com.erudio.file.exporter.impl.PdfExporter
import br.com.erudio.file.exporter.impl.XlsxExporter
import br.com.erudio.file.importer.factory.FileImporterFactory
import br.com.erudio.file.importer.impl.CsvImporter
import br.com.erudio.file.importer.impl.XlsxImporter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationContext
import org.springframework.test.util.ReflectionTestUtils

class FileFactoriesTest {

    @Nested
    inner class Exporters {

        private val context = mock<ApplicationContext>()
        private val factory = FileExporterFactory()
        private val xlsx = XlsxExporter()
        private val csv = CsvExporter()
        private val pdf = PdfExporter()

        @BeforeEach
        fun setUp() {
            ReflectionTestUtils.setField(factory, "context", context)
            whenever(context.getBean(XlsxExporter::class.java)).thenReturn(xlsx)
            whenever(context.getBean(CsvExporter::class.java)).thenReturn(csv)
            whenever(context.getBean(PdfExporter::class.java)).thenReturn(pdf)
        }

        @Test
        fun picksTheExporterFromTheAcceptHeader() {
            assertSame(xlsx, factory.getExporter(MediaTypes.APPLICATION_XLSX_VALUE))
            assertSame(csv, factory.getExporter(MediaTypes.APPLICATION_CSV_VALUE))
            assertSame(pdf, factory.getExporter(MediaTypes.APPLICATION_PDF_VALUE))
        }

        @Test
        fun theAcceptHeaderIsMatchedIgnoringCase() {
            assertSame(csv, factory.getExporter("TEXT/CSV"))
            assertSame(pdf, factory.getExporter("Application/PDF"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["application/json", "application/xml", "text/plain", "*/*", ""])
        fun rejectsAnyOtherFormat(accept: String) {
            val exception = assertThrows(BadRequestException::class.java) { factory.getExporter(accept) }

            assertEquals("Invalid File Format!", exception.message)
        }

        @Test
        fun theExporterItReturnsIsUsable() {
            val exporter = factory.getExporter(MediaTypes.APPLICATION_CSV_VALUE)

            assertTrue(exporter.exportPeople(listOf()).contentLength() > 0, "at least the header line")
        }
    }

    @Nested
    inner class Importers {

        private val context = mock<ApplicationContext>()
        private val factory = FileImporterFactory()
        private val xlsx = XlsxImporter()
        private val csv = CsvImporter()

        @BeforeEach
        fun setUp() {
            ReflectionTestUtils.setField(factory, "context", context)
            whenever(context.getBean(XlsxImporter::class.java)).thenReturn(xlsx)
            whenever(context.getBean(CsvImporter::class.java)).thenReturn(csv)
        }

        @Test
        fun picksTheImporterFromTheFileExtension() {
            val fromXlsx = factory.getImporter("people.xlsx")
            val fromCsv = factory.getImporter("people.csv")

            assertSame(xlsx, fromXlsx)
            assertSame(csv, fromCsv)
        }

        @Test
        fun onlyTheLastExtensionCounts() {
            assertSame(csv, factory.getImporter("people.xlsx.csv"))
            assertSame(xlsx, factory.getImporter("my people (1).xlsx"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["people.txt", "people.xls", "people.pdf", "people", "csv", "people.csv.bak", ""])
        fun rejectsAnyOtherExtension(fileName: String) {
            val exception = assertThrows(BadRequestException::class.java) { factory.getImporter(fileName) }

            assertEquals("Invalid File Format!", exception.message)
        }
    }
}
