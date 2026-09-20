package br.com.erudio.file.exporter.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.contract.PersonExporter
import br.com.erudio.services.QRCodeService
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class PdfExporter : PersonExporter {

    @Autowired
    private lateinit var service: QRCodeService

    @Throws(Exception::class)
    override fun exportPeople(people: List<PersonDTO>): Resource {
        val inputStream = javaClass.getResourceAsStream("/templates/people.jrxml")
            ?: throw RuntimeException("Template file not found: /templates/people.jrxml")

        val jasperReport = JasperCompileManager.compileReport(inputStream)

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
        val mainTemplateStream = javaClass.getResourceAsStream("/templates/person.jrxml")
            ?: throw RuntimeException("Template file not found: /templates/person.jrxml")

        val subReportStream = javaClass.getResourceAsStream("/templates/books.jrxml")
            ?: throw RuntimeException("Template file not found: /templates/books.jrxml")

        val mainReport = JasperCompileManager.compileReport(mainTemplateStream)
        val subReport = JasperCompileManager.compileReport(subReportStream)

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
