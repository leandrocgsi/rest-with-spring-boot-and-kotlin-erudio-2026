package br.com.erudio.file.exporter.factory

import br.com.erudio.exception.BadRequestException
import br.com.erudio.file.exporter.MediaTypes
import br.com.erudio.file.exporter.contract.PersonExporter
import br.com.erudio.file.exporter.impl.CsvExporter
import br.com.erudio.file.exporter.impl.PdfExporter
import br.com.erudio.file.exporter.impl.XlsxExporter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class FileExporterFactory {

    @Autowired
    private lateinit var context: ApplicationContext

    @Throws(Exception::class)
    fun getExporter(acceptHeader: String?): PersonExporter {
        return if (acceptHeader.equals(MediaTypes.APPLICATION_XLSX_VALUE, ignoreCase = true)) {
            context.getBean(XlsxExporter::class.java)
        } else if (acceptHeader.equals(MediaTypes.APPLICATION_CSV_VALUE, ignoreCase = true)) {
            context.getBean(CsvExporter::class.java)
        } else if (acceptHeader.equals(MediaTypes.APPLICATION_PDF_VALUE, ignoreCase = true)) {
            context.getBean(PdfExporter::class.java)
        } else {
            throw BadRequestException("Invalid File Format!")
        }
    }
}
