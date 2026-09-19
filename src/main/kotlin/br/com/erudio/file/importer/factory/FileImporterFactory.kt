package br.com.erudio.file.importer.factory

import br.com.erudio.exception.BadRequestException
import br.com.erudio.file.importer.contract.FileImporter
import br.com.erudio.file.importer.impl.CsvImporter
import br.com.erudio.file.importer.impl.XlsxImporter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class FileImporterFactory {

    @Autowired
    private lateinit var context: ApplicationContext

    @Throws(Exception::class)
    fun getImporter(fileName: String): FileImporter {
        return if (fileName.endsWith(".xlsx")) {
            context.getBean(XlsxImporter::class.java)
        } else if (fileName.endsWith(".csv")) {
            context.getBean(CsvImporter::class.java)
        } else {
            throw BadRequestException("Invalid File Format!")
        }
    }
}
