package br.com.erudio.file.importer.contract

import br.com.erudio.data.dto.PersonDTO
import java.io.InputStream

interface FileImporter {

    @Throws(Exception::class)
    fun importFile(inputStream: InputStream): List<PersonDTO>
}
