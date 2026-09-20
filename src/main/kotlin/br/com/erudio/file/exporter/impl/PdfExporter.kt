package br.com.erudio.file.exporter.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.contract.PersonExporter
import br.com.erudio.services.QRCodeService
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

@Component
class PdfExporter : PersonExporter {

    @Autowired
    private lateinit var service: QRCodeService

    private val reports = ConcurrentHashMap<String, JasperReport>()

    private fun report(template: String): JasperReport = reports.getOrPut(template) {
        val path = "/templates/$template"
        val stream = javaClass.getResourceAsStream(path)
            ?: throw RuntimeException("Template file not found: $path")
        stream.use { JasperCompileManager.compileReport(it) }
    }

    @Throws(Exception::class)
    override fun exportPeople(people: List<PersonDTO>): Resource {
        val jasperReport = report("people.jrxml")

        val dataSource = JRBeanCollectionDataSource(people)
        val parameters: MutableMap<String, Any> = HashMap()

        val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)
        ByteArrayOutputStream().use { outputStream ->
            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream)
            return ByteArrayResource(outputStream.toByteArray())
        }
    }

    @Throws(Exception::class)
    override fun exportPerson(person: PersonDTO): Resource {
        val mainReport = report("person.jrxml")
        val subReport = report("books.jrxml")

        val qrCodeStream = service.generateQRCode(person.profileUrl!!, 200, 200)

        val subReportDataSource = JRBeanCollectionDataSource(person.books)

        val parameters: MutableMap<String, Any> = HashMap()
        parameters["SUB_REPORT_DATA_SOURCE"] = subReportDataSource
        parameters["BOOK_SUB_REPORT"] = subReport
        parameters["QR_CODEIMAGE"] = qrCodeStream

        val mainDataSource = JRBeanCollectionDataSource(listOf(person))

        val jasperPrint = JasperFillManager.fillReport(mainReport, parameters, mainDataSource)
        ByteArrayOutputStream().use { outputStream ->
            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream)
            return ByteArrayResource(outputStream.toByteArray())
        }
    }
}
